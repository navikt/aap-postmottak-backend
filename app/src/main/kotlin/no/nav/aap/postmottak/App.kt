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
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.binder.logging.LogbackMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.komponenter.server.AZURE
import no.nav.aap.komponenter.server.commonKtorModule
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbmigrering.Migrering
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import no.nav.aap.motor.Motor
import no.nav.aap.motor.api.motorApi
import no.nav.aap.motor.retry.RetryService
import no.nav.aap.postmottak.behandling.avklaringsbehov.flate.avklaringsbehovApi
import no.nav.aap.postmottak.behandling.avklaringsbehov.løsning.utledSubtypes
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklarteam.flate.avklarTemaApi
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.flate.finnSakApi
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.flate.dokumentApi
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.flate.kategoriseringApi
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering.flate.struktureringApi
import no.nav.aap.postmottak.flyt.flate.DefinisjonDTO
import no.nav.aap.postmottak.flyt.flate.behandlingApi
import no.nav.aap.postmottak.flyt.flate.flytApi
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.mottak.kafka.Stream
import no.nav.aap.postmottak.mottak.mottakStream
import no.nav.aap.postmottak.server.exception.FlytOperasjonException
import no.nav.aap.postmottak.server.prosessering.BehandlingsflytLogInfoProvider
import no.nav.aap.postmottak.server.prosessering.ProsesseringsJobber
import no.nav.aap.postmottak.test.testApi
import no.nav.aap.verdityper.feilhåndtering.ElementNotFoundException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

internal val SECURE_LOGGER: Logger = LoggerFactory.getLogger("secureLog")

class App

val SYSTEMBRUKER = Bruker("Kelvin")

private const val ANTALL_WORKERS = 4

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e -> SECURE_LOGGER.error("Uhåndtert feil", e) }
    embeddedServer(Netty, configure = {
        connector {
            port = 8080
        }
        connectionGroupSize = 8
        workerGroupSize = 8
        callGroupSize = 16
    }) { server(DbConfig()) }.start(wait = true)
}

internal fun Application.server(
    dbConfig: DbConfig
) {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    DefaultJsonMapper.objectMapper()
        .registerSubtypes(utledSubtypes())

    commonKtorModule(prometheus, azureConfig = AzureConfig(), InfoModel(title = "AAP - Postmottak"))

    install(MicrometerMetrics) {
        registry = prometheus
        meterBinders += LogbackMetrics()
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            when (cause) {
                is ElementNotFoundException -> {
                    call.respondText(status = HttpStatusCode.NotFound, text = cause.message ?: "")
                }

                is FlytOperasjonException -> {
                    call.respond(status = cause.status(), message = cause.body())
                }

                else -> {
                    LoggerFactory.getLogger(App::class.java)
                        .warn("Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                    call.respond(status = HttpStatusCode.InternalServerError, message = ErrorRespons(cause.message))
                }
            }
        }
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    val dataSource = initDatasource(dbConfig)
    Migrering.migrate(dataSource)
    val motor = module(dataSource)

    val mottakStream = mottakStream(dataSource, prometheus)

    routing {
        authenticate(AZURE) {
            apiRouting {
                configApi()
                behandlingApi(dataSource)
                flytApi(dataSource)
                avklaringsbehovApi(dataSource)
                dokumentApi()
                avklarTemaApi(dataSource)
                kategoriseringApi(dataSource)
                finnSakApi(dataSource)
                struktureringApi(dataSource)
                motorApi(dataSource)
                testApi(dataSource)
            }
        }
        actuator(prometheus, motor, mottakStream)
    }

}

fun Application.module(dataSource: DataSource): Motor {
    val motor = Motor(
        dataSource = dataSource,
        antallKammer = ANTALL_WORKERS,
        logInfoProvider = BehandlingsflytLogInfoProvider,
        jobber = ProsesseringsJobber.alle()
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
        get<Unit, List<DefinisjonDTO>> {
            respond(Definisjon.entries.map {
                DefinisjonDTO(
                    navn = it.name, type = it.kode,
                    behovType = it.type,
                    løsesISteg = it.løsesISteg
                )
            }.toList())
        }
    }
}

private fun Routing.actuator(prometheus: PrometheusMeterRegistry, motor: Motor, stream: Stream) {
    route("/actuator") {
        get("/metrics") {
            call.respond(prometheus.scrape())
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

fun initDatasource(dbConfig: DbConfig): HikariDataSource {
    return HikariDataSource(HikariConfig().apply {
        jdbcUrl = dbConfig.url
        username = dbConfig.username
        password = dbConfig.password
        maximumPoolSize = 10 + (ANTALL_WORKERS * 2)
        minimumIdle = 1
        driverClassName = "org.postgresql.Driver"
        connectionTestQuery = "SELECT 1"

    })
}
