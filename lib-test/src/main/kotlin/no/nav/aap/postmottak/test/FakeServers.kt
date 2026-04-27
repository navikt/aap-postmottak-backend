package no.nav.aap.postmottak.test

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.nimbusds.jwt.JWTParser
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.engine.ConnectorType
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import no.nav.aap.fordeler.arena.ArenaOpprettOppgaveForespørsel
import no.nav.aap.fordeler.arena.ArenaOpprettOppgaveRespons
import no.nav.aap.postmottak.gateway.FerdigstillRequest
import no.nav.aap.postmottak.gateway.OppdaterJournalpostRequest
import no.nav.aap.postmottak.test.fakes.aapInternApiFake
import no.nav.aap.postmottak.test.fakes.behandlingsflytFake
import no.nav.aap.postmottak.test.fakes.gosysOppgaveFake
import no.nav.aap.postmottak.test.fakes.nomFake
import no.nav.aap.postmottak.test.fakes.norgFake
import no.nav.aap.postmottak.test.fakes.safFake
import no.nav.aap.postmottak.test.fakes.unleashFake
import no.nav.aap.postmottak.test.fakes.veilarbarena
import no.nav.aap.postmottak.test.modell.TestPerson
import no.nav.aap.tilgang.JournalpostTilgangRequest
import no.nav.aap.tilgang.PersonTilgangRequest
import no.nav.aap.tilgang.TilgangResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

private const val POSTMOTTAK_BACKEND = "postmottak-backend"
private val logger = LoggerFactory.getLogger(FakesExtension::class.java)

class FakePersoner(val fakePersoner: MutableMap<String, TestPerson> = mutableMapOf()) {
    fun leggTil(testPerson: TestPerson) {
        fakePersoner[testPerson.aktivIdent().identifikator] = testPerson
    }
}

class FakeServers : AutoCloseable {
    private val log: Logger = LoggerFactory.getLogger(FakeServers::class.java)

    private val texas = embeddedServer(Netty, port = 0) { texasFakes() }
    private val oppgave = embeddedServer(Netty, port = 0, module = { oppgaveFake() })

    val fakePersoner: FakePersoner = FakePersoner()
    val saf = embeddedServer(Netty, port = 0, module = { safFake() })
    val joark = embeddedServer(Netty, port = 0, module = { joarkFake() })
    val behandlingsflyt = embeddedServer(Netty, port = 0, module = { behandlingsflytFake() })
    val tilgang = embeddedServer(Netty, port = 0, module = { tilgangFake() })
    val gosysOppgave = embeddedServer(Netty, port = 0, module = { gosysOppgaveFake() })
    val aapInternApi = embeddedServer(Netty, port = 0, module = { aapInternApiFake() })
    val pdl = embeddedServer(Netty, port = 0, module = { pdlFake() })
    val fssProxy = embeddedServer(Netty, port = 0, module = { fssProxy() })
    val nomFake = embeddedServer(Netty, port = 0, module = { nomFake() })
    val norgFake = embeddedServer(Netty, port = 0, module = { norgFake() })
    val staistikkFake = embeddedServer(Netty, port = 0, module = { statistikkFake() })
    val veilarbarena = embeddedServer(Netty, port = 0, module = { veilarbarena() })
    val eregFake = embeddedServer(Netty, port = 0, module = { eregFake() })
    val unleash = embeddedServer(Netty, port = 0, module = { unleashFake() })

    private val started = AtomicBoolean(false)

    private fun setProperties() {
        Thread.currentThread().setUncaughtExceptionHandler { _, e -> log.error("Uhåndtert feil", e) }

        System.setProperty("NAIS_CLUSTER_NAME", "LOCAL")
        System.setProperty("NAIS_APP_NAME", "postmottak-backend")

        // Texas
        System.setProperty("nais.token.endpoint", "http://localhost:${texas.port()}/token")
        System.setProperty("nais.token.exchange.endpoint", "http://localhost:${texas.port()}/token/exchange")
        System.setProperty("nais.token.introspection.endpoint", "http://localhost:${texas.port()}/introspect")

        // Gosys
        System.setProperty("gosys.url", "http://localhost:1234/")

        // Unleash
        System.setProperty("unleash.server.api.url", "http://localhost:${unleash.port()}")
        System.setProperty("unleash.server.api.token", "dummy")

        // Oppgave
        System.setProperty("integrasjon.oppgave.scope", "oppgave")
        if (System.getenv("INTEGRASJON_OPPGAVE_URL").isNullOrEmpty()) {
            System.setProperty("integrasjon.oppgave.url", "http://localhost:${oppgave.port()}")
        }

        // Gosys-oppgave
        System.setProperty("integrasjon.oppgaveapi.scope", "gosysOppgave")
        System.setProperty("integrasjon.oppgaveapi.url", "http://localhost:${gosysOppgave.port()}")

        // Behandlingsflyt
        System.setProperty("integrasjon.behandlingsflyt.scope", "behandlingsflyt")
        if (System.getenv("INTEGRASJON_BEHANDLINGSFLYT_URL").isNullOrEmpty()) {
            System.setProperty("integrasjon.behandlingsflyt.url", "http://localhost:${behandlingsflyt.port()}")
        }

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

        // Ereg
        System.setProperty("integrasjon.ereg.url", "http://localhost:${eregFake.port()}")
        System.setProperty("integrasjon.ereg.scope", "scope")
    }

    fun start() {
        log.info("STARTER")
        if (started.get()) {
            return
        }

        texas.start()
        unleash.start()
        oppgave.start()
        saf.start()
        joark.start()
        behandlingsflyt.start()
        tilgang.start()
        gosysOppgave.start()
        aapInternApi.start()
        pdl.start()
        fssProxy.start()
        nomFake.start()
        norgFake.start()
        staistikkFake.start()
        veilarbarena.start()
        eregFake.start()

        setProperties()

        started.set(true)
    }

    override fun close() {
        logger.info("Closing Servers.")
        if (!started.get()) {
            return
        }
        texas.stop()
        unleash.stop()
        oppgave.stop()
        saf.stop()
        joark.stop()
        nomFake.stop()
        behandlingsflyt.stop()
        tilgang.stop()
        gosysOppgave.stop()
        aapInternApi.stop()
        pdl.stop()
        fssProxy.stop()
        norgFake.stop()
        staistikkFake.stop()
        veilarbarena.stop()
        eregFake.stop()
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
                call.respond(HttpStatusCode.NoContent)
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

    private fun Application.texasFakes() {
        install(ContentNegotiation) {
            jackson()
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@texasFakes.log.info("TEXAS :: Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }
        routing {
            post("/token") {
                val token = AzureTokenGen("postmottak", "postmottak")
                    .generate(isApp = true, azp = "postmottak")
                call.respond(TestToken(access_token = token))
            }

            post("/token/exchange") {
                val body = call.receive<JsonNode>()
                val NAVident = JWTParser.parse(body["user_token"].asText())
                    .jwtClaimsSet
                    .getClaimAsString("NAVident")

                val token = AzureTokenGen(
                    issuer = body["identity_provider"].asText(),
                    audience = body["target"].asText(),
                ).generate(isApp = false, azp = "postmottak", navIdent = NAVident)

                call.respond(TestToken(access_token = token))
            }

            post("/introspect") {
                call.respond(mapOf("active" to true))
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
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }
        routing {
            post("/tilgang/journalpost") {
                call.receive<JournalpostTilgangRequest>()
                call.respond(TilgangResponse(true))
            }
            post("/tilgang/person") {
                call.receive<PersonTilgangRequest>()
                call.respond(TilgangResponse(true))
            }
        }
    }

    private fun Application.fssProxy() {
        install(ContentNegotiation) {
            jackson()
        }
        routing {
            post("/arena/nyesteaktivesak") {
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

    private fun Application.eregFake() {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
            }
        }
        routing {
            get("/api/v2/organisasjon") {
                call.respondText(ContentType.Text.Plain) {
                    ""
                }
            }
        }
    }


    @Suppress("PropertyName")
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
                                 ],
                                 "folkeregisteridentifikator": [
                                    {
                                      "identifikasjonsnummer": "${identer[0]}"
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
                                 ],
                                 "folkeregisteridentifikator": [
                                    {
                                      "identifikasjonsnummer": "${if (identer.isEmpty()) "1234568" else identer[0]}"
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
                    "navn": [
                       {
                         "fornavn": "Ola",
                         "mellomnavn": null,
                         "etternavn": "Normann"
                       }
                    ],
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

private fun EmbeddedServer<*, *>.port(): Int {
    return runBlocking {
        this@port.engine.resolvedConnectors()
    }.first { it.type == ConnectorType.HTTP }
        .port
}
