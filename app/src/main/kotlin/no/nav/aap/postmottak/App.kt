package no.nav.aap.postmottak

import com.papsign.ktor.openapigen.model.info.InfoModel
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbmigrering.Migrering
import no.nav.aap.komponenter.gateway.GatewayRegistry
import no.nav.aap.komponenter.httpklient.exception.ApiException
import no.nav.aap.komponenter.httpklient.exception.InternfeilException
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.server.AZURE
import no.nav.aap.komponenter.server.commonKtorModule
import no.nav.aap.komponenter.server.plugins.NavIdentInterceptor
import no.nav.aap.motor.Motor
import no.nav.aap.motor.api.motorApi
import no.nav.aap.motor.retry.RetryService
import no.nav.aap.postmottak.api.auditlog.auditlogApi
import no.nav.aap.postmottak.api.faktagrunnlag.dokument.dokumentApi
import no.nav.aap.postmottak.api.faktagrunnlag.overlevering.overleveringApi
import no.nav.aap.postmottak.api.faktagrunnlag.sak.finnSakApi
import no.nav.aap.postmottak.api.faktagrunnlag.strukturering.digitaliseringApi
import no.nav.aap.postmottak.api.faktagrunnlag.tema.avklarTemaApi
import no.nav.aap.postmottak.api.flyt.behandlingApi
import no.nav.aap.postmottak.api.flyt.flytApi
import no.nav.aap.postmottak.avklaringsbehov.flate.avklaringsbehovApi
import no.nav.aap.postmottak.avklaringsbehov.løsning.utledSubtypes
import no.nav.aap.postmottak.exception.FlytOperasjonException
import no.nav.aap.postmottak.klient.AapInternApiKlient
import no.nav.aap.postmottak.klient.arena.ArenaKlient
import no.nav.aap.postmottak.klient.arena.VeilarbarenaKlient
import no.nav.aap.postmottak.klient.behandlingsflyt.BehandlingsflytKlient
import no.nav.aap.postmottak.klient.gosysoppgave.GosysOppgaveKlient
import no.nav.aap.postmottak.klient.nom.NomKlient
import no.nav.aap.postmottak.klient.norg.NorgKlient
import no.nav.aap.postmottak.klient.oppgave.OppgaveKlient
import no.nav.aap.postmottak.klient.pdl.PdlGraphqlKlient
import no.nav.aap.postmottak.klient.saf.SafOboRestClient
import no.nav.aap.postmottak.klient.saf.SafRestClient
import no.nav.aap.postmottak.klient.saf.graphql.SafGraphqlClientCredentialsClient
import no.nav.aap.postmottak.klient.saf.graphql.SafGraphqlOboClient
import no.nav.aap.postmottak.klient.statistikk.StatistikkKlient
import no.nav.aap.postmottak.klient.unleash.UnleashService
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.mottak.kafka.Stream
import no.nav.aap.postmottak.mottak.mottakStream
import no.nav.aap.postmottak.prosessering.PostmottakLogInfoProvider
import no.nav.aap.postmottak.prosessering.ProsesseringsJobber
import no.nav.aap.postmottak.repository.postgresRepositoryRegistry
import no.nav.aap.postmottak.test.testApi
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class App

private const val ANTALL_WORKERS = 4

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e ->
        LoggerFactory.getLogger(App::class.java).error("Uhåndtert feil.", e)
    }
    val serverPort = System.getenv("HTTP_PORT")?.toInt() ?: 8080
    embeddedServer(Netty, configure = {
        connector {
            port = serverPort
        }
        connectionGroupSize = 8
        workerGroupSize = 8
        callGroupSize = 16
    }) { server(DbConfig(), postgresRepositoryRegistry) }.start(wait = true)
}

internal fun Application.server(
    dbConfig: DbConfig,
    repositoryRegistry: RepositoryRegistry
) {
    PrometheusProvider.prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    DefaultJsonMapper.objectMapper()
        .registerSubtypes(utledSubtypes())

    commonKtorModule(
        prometheus = PrometheusProvider.prometheus,
        azureConfig = AzureConfig(),
        InfoModel(title = "AAP - Postmottak")
    )

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            val logger = LoggerFactory.getLogger(javaClass)

            when (cause) {
                is InternfeilException -> {
                    logger.error(cause.cause?.message ?: cause.message)
                    call.respondWithError(cause)
                }

                is ApiException -> {
                    logger.warn(cause.message, cause)
                    call.respondWithError(cause)
                }

                is FlytOperasjonException -> {
                    call.respondWithError(
                        ApiException(
                            status = cause.status(),
                            message = cause.body().message ?: "Ukjent feil i behandlingsflyt"
                        )
                    )
                }

                else -> {
                    logger.error("Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                    call.respondWithError(InternfeilException("En ukjent feil oppsto"))
                }
            }
        }
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    val dataSource = initDatasource(dbConfig, PrometheusProvider.prometheus)
    Migrering.migrate(dataSource)
    val motor = startMotor(dataSource, repositoryRegistry)

    registerGateways()
    
    val mottakStream = mottakStream(dataSource, repositoryRegistry)

    routing {
        authenticate(AZURE) {
            install(NavIdentInterceptor)

            apiRouting {
                configApi()
                behandlingApi(dataSource, repositoryRegistry)
                flytApi(dataSource, repositoryRegistry)
                avklaringsbehovApi(dataSource, repositoryRegistry)
                dokumentApi(dataSource, repositoryRegistry)
                avklarTemaApi(dataSource, repositoryRegistry)
                finnSakApi(dataSource, repositoryRegistry)
                digitaliseringApi(dataSource, repositoryRegistry)
                overleveringApi(dataSource, repositoryRegistry)
                motorApi(dataSource)
                testApi(dataSource, repositoryRegistry)
                auditlogApi(dataSource, repositoryRegistry)
            }
        }
        actuator(motor, mottakStream)
    }

}

private suspend fun ApplicationCall.respondWithError(exception: ApiException) {
    respond(
        exception.status,
        exception.tilApiErrorResponse()
    )
}

private fun registerGateways() {
    GatewayRegistry
        .register<OppgaveKlient>()
        .register<GosysOppgaveKlient>()
        .register<SafGraphqlClientCredentialsClient>()
        .register<SafGraphqlOboClient>()
        .register<SafOboRestClient>()
        .register<SafRestClient>()
        .register<BehandlingsflytKlient>()
        .register<NomKlient>()
        .register<ArenaKlient>()
        .register<PdlGraphqlKlient>()
        .register<NorgKlient>()
        .register<AapInternApiKlient>()
        .register<StatistikkKlient>()
        .register<VeilarbarenaKlient>()
        .register<UnleashService>()
        .status()
}

fun Application.startMotor(dataSource: DataSource, repositoryRegistry: RepositoryRegistry): Motor {
    val motor = Motor(
        dataSource = dataSource,
        antallKammer = ANTALL_WORKERS,
        logInfoProvider = PostmottakLogInfoProvider,
        jobber = ProsesseringsJobber.alle(),
        prometheus = PrometheusProvider.prometheus,
        repositoryRegistry = repositoryRegistry,
    )

    dataSource.transaction { dbConnection ->
        RetryService(dbConnection).enable()
    }

    monitor.subscribe(ApplicationStarted) {
        motor.start()
    }
    monitor.subscribe(ApplicationStopped) { application ->
        application.environment.log.info("Server har stoppet")
        motor.stop()
        // Release resources and unsubscribe from events
        application.monitor.unsubscribe(ApplicationStarted) {}
        application.monitor.unsubscribe(ApplicationStopped) {}
    }

    return motor
}

fun NormalOpenAPIRoute.configApi() {
    route("/config/definisjoner") {
        @Suppress("UnauthorizedGet")
        get<Unit, Map<AvklaringsbehovKode, Definisjon>> {
            val response = HashMap<AvklaringsbehovKode, Definisjon>()
            Definisjon.entries.forEach {
                response[it.kode] = it
            }
            respond(response)
        }
    }
}

private fun Routing.actuator(motor: Motor, stream: Stream) {
    route("/actuator") {
        get("/metrics") {
            call.respond(PrometheusProvider.prometheus.scrape())
        }

        get("/live") {
            if (stream.live()) call.respond(HttpStatusCode.OK, "Oppe!")
            else call.respond(HttpStatusCode.ServiceUnavailable)
        }

        get("/ready") {
            if (motor.kjører() && stream.ready()) {
                val status = HttpStatusCode.OK
                call.respond(status, "Oppe!")
            } else {
                call.respond(HttpStatusCode.ServiceUnavailable, "Kjører ikke")
            }
        }
    }
}

data class DbConfig(
    val url: String = requiredConfigForKey("DB_POSTMOTTAK_JDBC_URL"),
    val username: String = requiredConfigForKey("DB_POSTMOTTAK_USERNAME"),
    val password: String = requiredConfigForKey("DB_POSTMOTTAK_PASSWORD")
)

fun initDatasource(dbConfig: DbConfig, meterRegistry: MeterRegistry): HikariDataSource {
    return HikariDataSource(HikariConfig().apply {
        jdbcUrl = dbConfig.url
        username = dbConfig.username
        password = dbConfig.password
        maximumPoolSize = 10 + (ANTALL_WORKERS * 2)
        minimumIdle = 1
        driverClassName = "org.postgresql.Driver"
        connectionTestQuery = "SELECT 1"
        metricRegistry = meterRegistry
    })
}
