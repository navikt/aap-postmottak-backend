package no.nav.aap.postmottak

import com.papsign.ktor.openapigen.model.info.InfoModel
import com.papsign.ktor.openapigen.route.apiRouting
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopPreparing
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.authenticate
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbmigrering.Migrering
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.server.auth.IdentityProvider
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
import no.nav.aap.postmottak.api.flyt.avklaringsbehovApi
import no.nav.aap.postmottak.api.flyt.behandlingApi
import no.nav.aap.postmottak.api.flyt.flytApi
import no.nav.aap.postmottak.api.test.testApi
import no.nav.aap.postmottak.avklaringsbehov.løsning.utledSubtypes
import no.nav.aap.postmottak.klient.defaultGatewayProvider
import no.nav.aap.postmottak.mottak.kafka.Stream
import no.nav.aap.postmottak.mottak.lagMottakStream
import no.nav.aap.postmottak.prosessering.PostmottakLogInfoProvider
import no.nav.aap.postmottak.prosessering.ProsesseringsJobber
import no.nav.aap.postmottak.repository.postgresRepositoryRegistry
import no.nav.aap.tilgang.TilgangGateway
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

    // Vi følger IKKE ktor sin metodikk for å regne ut tuning parametre som funksjon av parallellitet,
    // for vi bruker blocking IO, ikke async IO.
    // https://github.com/ktorio/ktor/blob/3.3.1/ktor-server/ktor-server-core/common/src/io/ktor/server/engine/ApplicationEngine.kt#L30
    const val callGroupSize = 64

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
        infoModel = InfoModel(title = "AAP - Postmottak"),
        identityProvider = IdentityProvider.ENTRA_ID,
    )

    install(StatusPages, StatusPagesConfigHelper.setup())

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    val dataSource = initDatasource(dbConfig, PrometheusProvider.prometheus)
    Migrering.migrate(dataSource)

    val motor = lagMotor(dataSource, repositoryRegistry, gatewayProvider)
    val mottakStream = lagMottakStream(dataSource, repositoryRegistry, gatewayProvider)

    routing {
        authenticate(IdentityProvider.ENTRA_ID.value) {
            install(NavIdentInterceptor)

            apiRouting {
                behandlingApi(dataSource, repositoryRegistry)
                flytApi(dataSource, repositoryRegistry, gatewayProvider)
                avklaringsbehovApi(dataSource, repositoryRegistry, gatewayProvider)
                dokumentApi(dataSource, repositoryRegistry, gatewayProvider)
                avklarTemaApi(dataSource, repositoryRegistry)
                finnSakApi(dataSource, repositoryRegistry)
                digitaliseringApi(dataSource, repositoryRegistry, gatewayProvider)
                overleveringApi(dataSource, repositoryRegistry)
                motorApi(dataSource)
                testApi(dataSource, gatewayProvider)
                auditlogApi(dataSource, repositoryRegistry)
                driftApi(dataSource, repositoryRegistry)
            }
        }
        actuator(motor, mottakStream)
    }

    TilgangGateway.initialiserPrometheus(PrometheusProvider.prometheus)

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
