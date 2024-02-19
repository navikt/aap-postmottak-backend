package no.nav.aap.behandlingsflyt

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.adapter.PdlBarnGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.adapter.PdlPersonopplysningGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlIdentGateway
import no.nav.aap.behandlingsflyt.test.Fakes
import org.testcontainers.containers.PostgreSQLContainer

// Kjøres opp for å få logback i console uten json
fun main() {
    val postgres = postgreSQLContainer()

    // Setter opp virtuell sandkase lokalt
    setupFakes()

    // Starter server
    embeddedServer(Netty, port = 8080) {
        server(
            DbConfig(
                host = "sdg",
                port = "sdf",
                database = "sdf",
                url = postgres.jdbcUrl,
                username = postgres.username,
                password = postgres.password
            )
        )
    }.start(wait = true)
}

private fun postgreSQLContainer(): PostgreSQLContainer<Nothing> {
    val postgres = PostgreSQLContainer<Nothing>("postgres:16")
    postgres.start()
    Thread.sleep(10000); // Trengs denne virkelig?
    return postgres
}

fun setupFakes() {
    val fakes = Fakes()
    PdlIdentGateway.init(fakes.azureConf, fakes.pdlConf)
    PdlPersonopplysningGateway.init(fakes.azureConf, fakes.pdlConf)
    PdlBarnGateway.init(fakes.azureConf, fakes.pdlConf)
}
