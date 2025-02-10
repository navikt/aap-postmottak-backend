package no.nav.aap.postmottak.test

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import no.nav.aap.fordeler.arena.ArenaOpprettOppgaveForespørsel
import no.nav.aap.fordeler.arena.ArenaOpprettOppgaveRespons
import no.nav.aap.postmottak.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.postmottak.gateway.FerdigstillRequest
import no.nav.aap.postmottak.gateway.OppdaterJournalpostRequest
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.test.fakes.aapInternApiFake
import no.nav.aap.postmottak.test.fakes.behandlingsflytFake
import no.nav.aap.postmottak.test.fakes.gosysOppgaveFake
import no.nav.aap.postmottak.test.fakes.nomFake
import no.nav.aap.postmottak.test.fakes.norgFake
import no.nav.aap.postmottak.test.fakes.safFake
import no.nav.aap.postmottak.test.fakes.veilarbarena
import no.nav.aap.postmottak.test.modell.TestPerson
import no.nav.aap.tilgang.JournalpostTilgangRequest
import no.nav.aap.tilgang.TilgangResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate

class FakeServer(port: Int = 0, private val module: Application.() -> Unit) {
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> =
        embeddedServer(Netty, port = port, module = module).start()

    fun stop() {
        server.stop()
    }

    fun clean() {
        val port = server.port()
        server.stop(0, 0)
        server = embeddedServer(Netty, port = port, module = module).start()
    }

    fun setCustomModule(module: Application.() -> Unit) {
        val port = server.port()
        server.stop(0, 0)
        server = embeddedServer(Netty, port = port, module = module).start()
    }

    fun port(): Int = server.port()

    private fun EmbeddedServer<*, *>.port(): Int {
        return runBlocking {
            this@port.engine.resolvedConnectors()
        }.first { it.type == ConnectorType.HTTP }.port
    }
}

private const val POSTMOTTAK_BACKEND = "postmottak-backend"

class Fakes(azurePort: Int = 0) : AutoCloseable {
    private val log: Logger = LoggerFactory.getLogger(Fakes::class.java)
    val azure = FakeServer(azurePort) { azureFake() }
    private val oppgave = FakeServer(module = { oppgaveFake() })
    val fakePersoner: MutableMap<String, TestPerson> = mutableMapOf()
    val saf = FakeServer(module = { safFake() })
    val joark = FakeServer(module = { joarkFake() })
    val behandlingsflyt = FakeServer(module = { behandlingsflytFake() })
    val tilgang = FakeServer(module = { tilgangFake() })
    val gosysOppgave = FakeServer(module = { gosysOppgaveFake() })
    val aapInternApi = FakeServer(module = { aapInternApiFake() })
    val pdl = FakeServer(module = { pdlFake() })
    val fssProxy = FakeServer(module = { fssProxy() })
    val nomFake = FakeServer(module = { nomFake() })
    val norgFake = FakeServer(module = { norgFake() })
    val staistikkFake = FakeServer(module = { statistikkFake()} )
    val veilarbarena = FakeServer(module = { veilarbarena() })

    init {
        Thread.currentThread().setUncaughtExceptionHandler { _, e -> log.error("Uhåndtert feil", e) }

        System.setProperty("NAIS_CLUSTER_NAME", "LOCAL")
        System.setProperty("gosys.url", "http://localhost:3000/")
        // Azure
        System.setProperty("azure.openid.config.token.endpoint", "http://localhost:${azure.port()}/token")
        System.setProperty("azure.app.client.id", POSTMOTTAK_BACKEND)
        System.setProperty("azure.app.client.secret", "")
        System.setProperty("azure.openid.config.jwks.uri", "http://localhost:${azure.port()}/jwks")
        System.setProperty("azure.openid.config.issuer", POSTMOTTAK_BACKEND)

        // Oppgave
        System.setProperty("integrasjon.oppgave.scope", "oppgave")
        System.setProperty("integrasjon.oppgave.url", "http://localhost:${oppgave.port()}")

        // Gosys-oppgave
        System.setProperty("integrasjon.oppgaveapi.scope", "gosysOppgave")
        System.setProperty("integrasjon.oppgaveapi.url", "http://localhost:${gosysOppgave.port()}")

        // Behandlingsflyt
        System.setProperty("integrasjon.behandlingsflyt.scope", "behandlingsflyt")
        System.setProperty("integrasjon.behandlingsflyt.url", "http://localhost:${behandlingsflyt.port()}")

        // Saf
        System.setProperty("integrasjon.saf.url.graphql", "http://localhost:${saf.port()}/graphql")
        System.setProperty("integrasjon.saf.scope", "saf")
        System.setProperty("integrasjon.saf.url.rest", "http://localhost:${saf.port()}/rest")

        // Joark
        System.setProperty("integrasjon.joark.url", "http://localhost:${joark.port()}")
        System.setProperty("integrasjon.joark.scope", "scope")

        // Tilgang
        System.setProperty("integrasjon.tilgang.url", "http://localhost:${tilgang.port()}")
        System.setProperty("integrasjon.tilgang.scope", "scope")

        // AAP Intern API
        System.setProperty("integrasjon.aap.intern.api.url", "http://localhost:${aapInternApi.port()}")
        System.setProperty("integrasjon.aap.intern.api.scope", "scope")

        // PDL
        System.setProperty("integrasjon.pdl.url", "http://localhost:${pdl.port()}")
        System.setProperty("integrasjon.pdl.scope", "scope")

        // Aap FSS proxy
        System.setProperty("integrasjon.aap.fss.proxy.url", "http://localhost:${fssProxy.port()}")
        System.setProperty("integrasjon.aap.fss.proxy.scope", "scope")

        // NOM
        System.setProperty("integrasjon.nom.url", "http://localhost:${nomFake.port()}")
        System.setProperty("integrasjon.nom.scope", "scope")

        // NORG
        System.setProperty("integrasjon.norg.url", "http://localhost:${norgFake.port()}")

        // Statistikk
        System.setProperty("integrasjon.statistikk.url", "http://localhost:${staistikkFake.port()}")
        System.setProperty("integrasjon.statistikk.scope", "scope")
        
        // Veilarbarena
        System.setProperty("integrasjon.veilarbarena.url", "http://localhost:${veilarbarena.port()}")
        System.setProperty("integrasjon.veilarbarena.scope", "scope")

        // testpersoner
        val BARNLØS_PERSON_30ÅR =
            TestPerson(
                identer = setOf(Ident("12345678910", true)),
                fødselsdato = Fødselsdato(
                    LocalDate.now().minusYears(30),
                ),
            )
        val BARNLØS_PERSON_18ÅR =
            TestPerson(
                identer = setOf(Ident("42346734567", true)),
                fødselsdato = Fødselsdato(LocalDate.now().minusYears(18).minusDays(10)),
            )
        val PERSON_MED_BARN_65ÅR =
            TestPerson(
                identer = setOf(Ident("86322434234", true)),
                fødselsdato = Fødselsdato(LocalDate.now().minusYears(65)),
                barn = listOf(
                    BARNLØS_PERSON_18ÅR, BARNLØS_PERSON_30ÅR
                ),
            )

        // Legg til alle testpersoner
        listOf(PERSON_MED_BARN_65ÅR).forEach { leggTil(it) }
    }

    override fun close() {
        azure.stop()
        oppgave.stop()
        saf.stop()
        joark.stop()
        nomFake.stop()
    }

    fun leggTil(person: TestPerson) {
        person.identer.forEach { fakePersoner[it.identifikator] = person }
        person.barn.forEach { leggTil(it) }
    }


    private fun Application.oppgaveFake() {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
            }
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@oppgaveFake.log.info(
                    "Inntekt :: Ukjent feil ved kall til '{}'",
                    call.request.local.uri,
                    cause
                )
                call.respond(status = HttpStatusCode.InternalServerError, message = ErrorRespons(cause.message))
            }
        }
        routing {
            post("/behandling") {
                call.respond(HttpStatusCode.NoContent)
            }
        }
        routing {
            post("/oppdater-postmottak-oppgaver") {
                call.respond(HttpStatusCode.Companion.NoContent)
            }
        }
    }

    private fun Application.statistikkFake() {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
            }
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                call.respond(status = HttpStatusCode.InternalServerError, message = ErrorRespons(cause.message))
            }
        }
        routing {
            post("/postmottak") {
                call.respond(HttpStatusCode.OK, "{}")
            }
        }
    }

    private fun Application.joarkFake() {
        install(ContentNegotiation) {
            jackson()
        }
        routing {
            put("/rest/journalpostapi/v1/journalpost/{journalpostId}") {
                call.receive<OppdaterJournalpostRequest>()
                call.respondText { """{"journalpostId": "467011764"}""" }
            }
            patch("/rest/journalpostapi/v1/journalpost/{journalpostId}/ferdigstill") {
                call.receive<FerdigstillRequest>()
                call.respondText(ContentType.Text.Plain) {
                    "I'm just a string"
                }
            }
        }
    }

    private fun Application.pdlFake() {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
        }

        routing {
            post("/graphql") {
                val body = call.receive<String>()
                if (body.contains("hentIdenter")) {
                    call.respondText(genererHentIdenterRespons(body))
                } else if (body.contains("hentPersonBolk")) {
                    call.respondText(genererHentPersonBolkRespons(body))
                } else if (body.contains("hentGeografiskTilknytning")) {
                    call.respondText(genererHentAdressebeskytelseOgGeotilknytning())
                } else {
                    call.respondText(genererHentPersonRespons())
                }
            }
        }
    }

    private fun Application.azureFake() {
        install(ContentNegotiation) {
            jackson()
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@azureFake.log.info("AZURE :: Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                call.respond(status = HttpStatusCode.InternalServerError, message = ErrorRespons(cause.message))
            }
        }
        routing {
            post("/token") {
                val body = call.receiveText()
                val token = AzureTokenGen(
                    POSTMOTTAK_BACKEND, POSTMOTTAK_BACKEND
                ).generate(body.contains("grant_type=client_credentials"))
                call.respond(TestToken(access_token = token))
            }
            get("/jwks") {
                call.respond(AZURE_JWKS)
            }
        }
    }

    private fun Application.tilgangFake() {
        install(ContentNegotiation) {
            jackson()
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@tilgangFake.log.info(
                    "TILGANG :: Ukjent feil ved kall til '{}'",
                    call.request.local.uri,
                    cause
                )
                call.respond(
                    status = HttpStatusCode.Companion.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }
        routing {
            post("/tilgang/journalpost") {
                call.receive<JournalpostTilgangRequest>()
                call.respond(TilgangResponse(true))
            }
        }
    }

    private fun Application.fssProxy() {
        install(ContentNegotiation) {
            jackson()
        }
        routing {
            get("/arena/nyesteaktivesak/{ident}") {
                call.respondText(ContentType.Text.Plain) {
                    "12345678901"
                }
            }
            post("/arena/opprettoppgave") {
                call.receive<ArenaOpprettOppgaveForespørsel>()
                call.respond(ArenaOpprettOppgaveRespons("OPG-1234", "SAK-5678"))
            }
        }
    }


    data class TestToken(
        val access_token: String,
        val refresh_token: String = "very.secure.token",
        val id_token: String = "very.secure.token",
        val token_type: String = "token-type",
        val scope: String? = null,
        val expires_in: Int = 3599,
    )

    private fun genererHentPersonBolkRespons(body: String): String {
        val identer = finnIdenterIBody(body)

        if (identer.size == 2) {
            return """
                    { "data":
                    {"hentPersonBolk": [
                            {
                              "ident": "${identer[0]}",
                              "person": {
                                 "navn": [
                                   {
                                     "fornavn": "Ola",
                                     "mellomnavn": null,
                                     "etternavn": "Normann"
                                   }
                                 ]
                              },
                              "code": "ok"
                            },
                            {
                              "ident": "${identer[1]}",
                              "person": null,
                              "code": "not_found"
                            }
                            ]
                    }}
                """.trimIndent()
        } else {
            return """
                    { "data":
                    {"hentPersonBolk": [
                            {
                              "ident": "${if (identer.isEmpty()) "1234568" else identer[0]}",
                              "person": {
                                 "navn": [
                                   {
                                     "fornavn": "Ola",
                                     "mellomnavn": null,
                                     "etternavn": "Normann"
                                   }
                                 ]
                              },
                              "code": "ok"
                            }
                            ]
                    }}
                """.trimIndent()
        }
    }

    private fun genererHentPersonRespons(): String {
        return """
            { "data":
            {"hentPerson": {
                    "foedselsdato": [
                        {
                            "foedselsdato": "1990-01-01",
                            "metadata": {
                                "historisk": false
                            }
                        }
                    ]
                }
            }}
        """.trimIndent()
    }

    private fun genererHentAdressebeskytelseOgGeotilknytning(): String {
        return """
            {
              "data": {
                "hentPerson": {
                  "adressebeskyttelse": [{"gradering": "UGRADERT"}]
                },
                "hentGeografiskTilknytning": {
                  "gtType": "KOMMUNE",
                  "gtKommune": "3207",
                  "gtBydel": null,
                  "gtLand": null
                }
              }
            }
        """.trimIndent()
    }

    private fun genererHentIdenterRespons(body: String): String {
        val ident = finnIdentIBody(body)
        return """
            { "data":
            {"hentIdenter": {
                    "identer": [
                        {
                            "ident": "$ident",
                            "historisk": false,
                            "gruppe": "FOLKEREGISTERIDENT"
                        },
                        {
                            "ident": "1234567898",
                            "historisk": false,
                            "gruppe": "AKTORID"
                        }
                    ]
                }
            }}
        """.trimIndent()
    }

    private fun finnIdentIBody(body: String): String {
        return body.substringAfter("\"ident\" :")
            .substringBefore("}")
            .substringBefore(",")
            .replace("\"", "")
            .trim()
    }

    private fun finnIdenterIBody(body: String): List<String> {
        return body.substringAfter("\"identer\" :")
            .substringBefore("}")
            .substringBefore(",")
            .replace("[", "")
            .replace("]", "")
            .replace("\"", "")
            .split(",")
            .map { it.replace("\n", "").trim() }
            .filter { it != "null" }
    }

}