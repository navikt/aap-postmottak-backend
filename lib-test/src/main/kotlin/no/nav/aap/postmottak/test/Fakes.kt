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
import no.nav.aap.postmottak.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.postmottak.joark.FerdigstillRequest
import no.nav.aap.postmottak.joark.OppdaterJournalpostRequest
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.Saksinfo
import no.nav.aap.postmottak.test.modell.TestPerson
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tilgang.JournalpostTilgangRequest
import tilgang.TilgangResponse
import java.time.LocalDate


class FakeServer(port: Int = 0, module: Application.() -> Unit) {
    private val server: NettyApplicationEngine = embeddedServer(Netty, port = port, module = module).start()

    private var throwOnNextCall: HttpStatusCode? = null
    private var exceptionPath: String? = null

    init {
        server.application.install(createApplicationPlugin("exceptionThrower") {
            onCall { call ->
                val status = throwOnNextCall
                val path  = exceptionPath
                if (status != null && (path == null || call.request.path().contains(path))) {
                    call.respond(status)
                }
            }
        })
    }

    fun stop() {
        server.stop()
    }

    fun clean() {
        throwOnNextCall = null
        exceptionPath = null
    }

    fun port(): Int = server.port()

    fun throwException(status: HttpStatusCode = HttpStatusCode.BadRequest, path: String) {
        throwOnNextCall = status
        exceptionPath = path
    }

    private fun NettyApplicationEngine.port(): Int =
        runBlocking { resolvedConnectors() }
            .first { it.type == ConnectorType.HTTP }
            .port

}

class Fakes(azurePort: Int = 0) : AutoCloseable {
    private val log: Logger = LoggerFactory.getLogger(Fakes::class.java)
    val azure = FakeServer(azurePort, { azureFake() })
    private val oppgave = FakeServer(module = { oppgaveFake() })
    val fakePersoner: MutableMap<String, TestPerson> = mutableMapOf()
    val saf = FakeServer(module = { safFake() })
    val joark = FakeServer(module = { joarkFake() })
    val behandlkingsflyt = FakeServer(module = { behandlingsflytFake() })
    val tilgang = FakeServer(module = { tilgangFake() })

    init {
        Thread.currentThread().setUncaughtExceptionHandler { _, e -> log.error("Uhåndtert feil", e) }
        // Azure
        System.setProperty("azure.openid.config.token.endpoint", "http://localhost:${azure.port()}/token")
        System.setProperty("azure.app.client.id", "postmottak-backend")
        System.setProperty("azure.app.client.secret", "")
        System.setProperty("azure.openid.config.jwks.uri", "http://localhost:${azure.port()}/jwks")
        System.setProperty("azure.openid.config.issuer", "postmottak-backend")

        // Oppgave
        System.setProperty("integrasjon.oppgave.scope", "oppgave")
        System.setProperty("integrasjon.oppgave.url", "http://localhost:${oppgave.port()}")

        // Behandlingsflyt
        System.setProperty("integrasjon.behandlingsflyt.scope", "behandlingsflyt")
        System.setProperty("integrasjon.behandlingsflyt.url", "http://localhost:${behandlkingsflyt.port()}")
        
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

    private fun Application.behandlingsflytFake() {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
            }
        }

        routing {
            post("/api/sak/finnEllerOpprett") {
                call.respond(
                    Saksinfo(
                        (Math.random() * 9999999999).toLong().toString(),
                        Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2024, 1, 31)),
                    )
                )
            }

            post("/api/sak/finn") {
                call.respond(
                    listOf(
                        Saksinfo(
                            (Math.random() * 9999999999).toLong().toString(),
                            Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2024, 1, 31)),
                        )
                    )
                )
            }

            post("/api/soknad/send") {
                call.respond(HttpStatusCode.NoContent)
            }
        }

    }

    private fun Application.safFake() {

        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
        }

        routing {
            get("/rest/hentdokument/{journalpostId}/{dokumentInfoId}/{variantFormat}") {
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "ktor_logo.pdf")
                        .toString()
                )
                call.response.header(HttpHeaders.ContentType, ContentType.Application.Pdf.toString())
                val samplePdf = this.javaClass.classLoader.getResourceAsStream("sample.pdf")
                call.respondOutputStream {
                    samplePdf.copyTo(this)
                }
            }
            post("/graphql") {
                val body = call.receive<String>()
                val journalpostId = body.substringAfter("\"journalpostId\" :").substringBefore("}").trim()
                this@safFake.log.info("Henter dokumenter for journalpost {}", journalpostId)

                call.respondText(
                    """
                    { "data":
                    {"journalpost":
                        {
                          "journalpostId": $journalpostId,
                          "tittel": "Overordnet tittel",
                          "personident": "3",
                          "bruker": {
                            "id": "213453452",
                            "type": "FNR"
                          },
                          "status": "MOTTATT",
                          "journalførendeEnhet": {"nr": 3001},
                          "mottattDato": "2021-12-01",
                          "relevanteDatoer": [
                            {
                            "dato": "2020-12-01T10:00:00",
                            "datotype": "DATO_REGISTRERT"
                            }
                          ], 
                          "dokumenter": [
                            {
                            "tittel": "Dokumenttittel",
                              "dokumentInfoId": "4542685451",
                              "brevkode": "NAV 11-13.05",
                              "dokumentvarianter": [
                                {
                                "variantformat": "ARKIV",
                                "filtype": "PDF"
                                }
                                ]
                            },
                            {
                            "tittel": "Dokument2",
                              "dokumentInfoId": "45426854351",
                              "brevkode": "Ukjent",
                              "dokumentvarianter": [
                                {
                                "variantformat": "ARKIV",
                                "filtype": "PDF"
                                }
                                ]
                            }
                          ]
                        }
                    }}
                """.trimIndent(),
                    contentType = ContentType.Application.Json
                )
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
                val token = AzureTokenGen("postmottak-backend", "postmottak-backend").generate()
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


    internal data class TestToken(
        val access_token: String,
        val refresh_token: String = "very.secure.token",
        val id_token: String = "very.secure.token",
        val token_type: String = "token-type",
        val scope: String? = null,
        val expires_in: Int = 3599,
    )
}