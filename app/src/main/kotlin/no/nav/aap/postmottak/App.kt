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
import no.nav.aap.komponenter.gateway.GatewayProvider
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
import no.nav.aap.postmottak.AppConfig.ANTALL_WORKERS_FOR_MOTOR
import no.nav.aap.postmottak.AppConfig.stansArbeidTimeout
import no.nav.aap.postmottak.api.auditlog.auditlogApi
import no.nav.aap.postmottak.api.drift.driftApi
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
import no.nav.aap.postmottak.klient.defaultGatewayProvider
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.mottak.kafka.Stream
import no.nav.aap.postmottak.mottak.lagMottakStream
import no.nav.aap.postmottak.prosessering.PostmottakLogInfoProvider
import no.nav.aap.postmottak.prosessering.ProsesseringsJobber
import no.nav.aap.postmottak.repository.postgresRepositoryRegistry
import no.nav.aap.postmottak.test.testApi
import org.slf4j.LoggerFactory
import javax.sql.DataSource
import kotlin.time.Duration.Companion.seconds

class App

internal object AppConfig {
    // Matcher terminationGracePeriodSeconds for podden i Kubernetes-manifestet ("nais.yaml")
    private val kubernetesTimeout = 30.seconds

    // Tid før ktor avslutter uansett. Må være litt mindre enn `kubernetesTimeout`.
    val shutdownTimeout = kubernetesTimeout - 2.seconds

    // Tid appen får til å fullføre påbegynte requests, jobber etc. Må være mindre enn `endeligShutdownTimeout`.
    val shutdownGracePeriod = shutdownTimeout - 3.seconds

    // Tid appen får til å avslutte Motor, Kafka, etc
    val stansArbeidTimeout = shutdownGracePeriod - 1.seconds

    // Vi skrur opp ktor sin default-verdi, som er "antall CPUer", satt ved -XX:ActiveProcessorCount i Dockerfile,
    // fordi appen vår er I/O-bound
    private val ktorParallellitet = 8
    // Vi følger ktor sin metodikk for å regne ut tuning parametre som funksjon av parallellitet
    // https://github.com/ktorio/ktor/blob/3.3.1/ktor-server/ktor-server-core/common/src/io/ktor/server/engine/ApplicationEngine.kt#L30
    val connectionGroupSize = ktorParallellitet / 2 + 1
    val workerGroupSize = ktorParallellitet / 2 + 1
    val callGroupSize = ktorParallellitet

    const val ANTALL_WORKERS_FOR_MOTOR = 4
}

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e ->
        LoggerFactory.getLogger(App::class.java).error("Uhåndtert feil.", e)
    }
    val serverPort = System.getenv("HTTP_PORT")?.toInt() ?: 8080
    embeddedServer(Netty, configure = {
        shutdownGracePeriod = AppConfig.shutdownGracePeriod.inWholeMilliseconds
        shutdownTimeout = AppConfig.shutdownTimeout.inWholeMilliseconds
        connector {
            port = serverPort
        }
        connectionGroupSize = AppConfig.connectionGroupSize
        workerGroupSize = AppConfig.workerGroupSize
        callGroupSize = AppConfig.callGroupSize
    }) {
        server(DbConfig(), postgresRepositoryRegistry, defaultGatewayProvider())
    }.start(wait = true)
}

internal fun Application.server(
    dbConfig: DbConfig,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider
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

    val motor = lagMotor(dataSource, repositoryRegistry, gatewayProvider)
    val mottakStream = lagMottakStream(dataSource, repositoryRegistry, gatewayProvider)

    routing {
        authenticate(AZURE) {
            install(NavIdentInterceptor)

            apiRouting {
                configApi()
                behandlingApi(dataSource, repositoryRegistry)
                flytApi(dataSource, repositoryRegistry, gatewayProvider)
                avklaringsbehovApi(dataSource, repositoryRegistry, gatewayProvider)
                dokumentApi(dataSource, repositoryRegistry, gatewayProvider)
                avklarTemaApi(dataSource, repositoryRegistry)
                finnSakApi(dataSource, repositoryRegistry)
                digitaliseringApi(dataSource, repositoryRegistry, gatewayProvider)
                overleveringApi(dataSource, repositoryRegistry)
                motorApi(dataSource)
                testApi(dataSource)
                auditlogApi(dataSource, repositoryRegistry)
                driftApi(dataSource, repositoryRegistry)
            }
        }
        actuator(motor, mottakStream)
    }

    monitor.subscribe(ApplicationStarted) {
        motor.start()
        mottakStream.start()
    }

    monitor.subscribe(ApplicationStopPreparing) { environment ->
        environment.log.info("ktor forbereder seg på å stoppe.")
    }
    monitor.subscribe(ApplicationStopping) { environment ->
        environment.log.info("ktor stopper nå å ta imot nye requester, og lar mottatte requester kjøre frem til timeout.")
        mottakStream.close(stansArbeidTimeout)
        motor.stop(stansArbeidTimeout)
    }
    monitor.subscribe(ApplicationStopped) { environment ->
        environment.log.info("ktor har fullført nedstoppingen sin. Eventuelle requester og annet arbeid som ikke ble fullført innen timeout ble avbrutt.")
        try {
            // Helt til slutt, nå som vi har stanset Motor, etc. Lukk database-koblingen.
            dataSource.close()
        } catch (_: Exception) {
            // Ignorert
        }
    }
}

private suspend fun ApplicationCall.respondWithError(exception: ApiException) {
    respond(
        exception.status,
        exception.tilApiErrorResponse()
    )
}

fun lagMotor(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider
): Motor {
    val motor = Motor(
        dataSource = dataSource,
        antallKammer = ANTALL_WORKERS_FOR_MOTOR,
        logInfoProvider = PostmottakLogInfoProvider,
        jobber = ProsesseringsJobber.alle(),
        prometheus = PrometheusProvider.prometheus,
        repositoryRegistry = repositoryRegistry,
        gatewayProvider = gatewayProvider
    )

    dataSource.transaction { dbConnection ->
        RetryService(dbConnection).enable()
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
        maximumPoolSize = 10 + (ANTALL_WORKERS_FOR_MOTOR * 2)
        minimumIdle = 1
        driverClassName = "org.postgresql.Driver"
        connectionTestQuery = "SELECT 1"
        metricRegistry = meterRegistry
    })
}
