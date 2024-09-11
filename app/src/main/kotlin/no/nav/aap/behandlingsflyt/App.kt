package no.nav.aap.behandlingsflyt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.binder.logging.LogbackMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import libs.kafka.KafkaStreams
import libs.kafka.SchemaRegistryConfig
import libs.kafka.Streams
import libs.kafka.StreamsConfig
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.flate.avklaringsbehovApi
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.utledSubtypes
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.flate.dokumentApi
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.avklarteam.flate.avklarTemaVurderingApi
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.kategorisering.flate.kategoriseringApi
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.strukturering.flate.struktureringApi
import no.nav.aap.behandlingsflyt.flyt.flate.DefinisjonDTO
import no.nav.aap.behandlingsflyt.flyt.flate.behandlingApi
import no.nav.aap.behandlingsflyt.flyt.flate.flytApi
import no.nav.aap.behandlingsflyt.mottak.MottakListener
import no.nav.aap.behandlingsflyt.server.apiRoute
import no.nav.aap.behandlingsflyt.server.authenticate.AZURE
import no.nav.aap.behandlingsflyt.server.authenticate.authentication
import no.nav.aap.behandlingsflyt.server.exception.FlytOperasjonException
import no.nav.aap.behandlingsflyt.server.prosessering.BehandlingsflytLogInfoProvider
import no.nav.aap.behandlingsflyt.server.prosessering.ProsesseringsJobber
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import no.nav.aap.motor.Motor
import no.nav.aap.motor.api.motorApi
import no.nav.aap.motor.retry.RetryService
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbflyway.Migrering
import no.nav.aap.verdityper.feilhåndtering.ElementNotFoundException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource

private val SECURE_LOGGER: Logger = LoggerFactory.getLogger("secureLog")

class App

val SYSTEMBRUKER = Bruker("Kelvin")

private const val ANTALL_WORKERS = 5

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e -> SECURE_LOGGER.error("Uhåndtert feil", e) }
    embeddedServer(Netty, port = 8080) { server(DbConfig()) }.start(wait = true)
}

internal fun Application.server(dbConfig: DbConfig,
                                kafka: Streams = KafkaStreams()) {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    DefaultJsonMapper.objectMapper()
        .registerSubtypes(utledSubtypes())

    install(MicrometerMetrics) {
        registry = prometheus
        meterBinders += LogbackMetrics()
    }
    generateOpenAPI()
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper = DefaultJsonMapper.objectMapper(), true))
    }
    install(CallId) {
        retrieveFromHeader(HttpHeaders.XCorrelationId)
        generate { UUID.randomUUID().toString() }
    }
    install(CallLogging) {
        callIdMdc("callId")
        filter { call -> call.request.path().startsWith("/actuator").not() }
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

    authentication(AzureConfig())

    val dataSource = initDatasource(dbConfig)
    Migrering.migrate(dataSource)
    val motor = module(dataSource)

    val config = StreamsConfig(schemaRegistry = SchemaRegistryConfig())
    val topology = MottakListener(config, dataSource)

    kafka.connect(
        topology = topology(),
        config = config,
        registry = prometheus,
    )

    routing {
        authenticate(AZURE) {
            apiRoute {
                configApi()
                behandlingApi(dataSource)
                flytApi(dataSource)
                avklaringsbehovApi(dataSource)
                dokumentApi()
                avklarTemaVurderingApi(dataSource)
                kategoriseringApi(dataSource)
                struktureringApi(dataSource)
                motorApi(dataSource)
            }
        }
        actuator(prometheus, motor)
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

    environment.monitor.subscribe(ApplicationStarted) {
        motor.start()
    }
    environment.monitor.subscribe(ApplicationStopped) { application ->
        application.environment.log.info("Server har stoppet")
        motor.stop()
        // Release resources and unsubscribe from events
        application.environment.monitor.unsubscribe(ApplicationStarted) {}
        application.environment.monitor.unsubscribe(ApplicationStopped) {}
    }

    return motor
}

fun NormalOpenAPIRoute.configApi() {
    route("/config/definisjoner") {
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

private fun Routing.actuator(prometheus: PrometheusMeterRegistry, motor: Motor) {
    route("/actuator") {
        get("/metrics") {
            call.respond(prometheus.scrape())
        }

        get("/live") {
            val status = HttpStatusCode.OK
            call.respond(status, "Oppe!")
        }

        get("/ready") {
            if (motor.kjører()) {
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
    SECURE_LOGGER.info(dbConfig.toString())
    return HikariDataSource(HikariConfig().apply {
    jdbcUrl = dbConfig.url
    username = dbConfig.username
    password = dbConfig.password
    maximumPoolSize = 10 + ANTALL_WORKERS
    minimumIdle = 1
    driverClassName = "org.postgresql.Driver"
    connectionTestQuery = "SELECT 1"

})}
