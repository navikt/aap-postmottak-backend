package no.nav.aap.behandlingsflyt.test

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
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.joark.FerdigstillRequest
import no.nav.aap.behandlingsflyt.joark.OppdaterJournalpostRequest
import no.nav.aap.behandlingsflyt.overlevering.behandlingsflyt.Periode
import no.nav.aap.behandlingsflyt.overlevering.behandlingsflyt.Saksinfo
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.intellij.lang.annotations.Language
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.time.LocalDate
import java.util.*

class Fakes(azurePort: Int = 0) : AutoCloseable {
    private val log: Logger = LoggerFactory.getLogger(Fakes::class.java)
    private val azure = embeddedServer(Netty, port = azurePort, module = { azureFake() }).start()
    private val oppgavestyring = embeddedServer(Netty, port = 0, module = { oppgavestyringFake() }).start()
    private val fakePersoner: MutableMap<String, TestPerson> = mutableMapOf()
    private val saf = embeddedServer(Netty, port = 0, module = { safFake() }).apply { start() }
    private val medl = embeddedServer(Netty, port = 0, module = { medlFake() }).apply { start() }
    private val joark = embeddedServer(Netty, port = 0, module = { joarkFake() }).apply { start() }
    private val pesysFake = embeddedServer(Netty, port = 0, module = { pesysFake() }).apply { start() }
    private val behandlkingsflyt = embeddedServer(Netty, port = 0, module = { behandlingsflytFake() }).apply { start() }


    init {
        Thread.currentThread().setUncaughtExceptionHandler { _, e -> log.error("Uhåndtert feil", e) }
        // Azure
        System.setProperty("azure.openid.config.token.endpoint", "http://localhost:${azure.port()}/token")
        System.setProperty("azure.app.client.id", "postmottak-backend")
        System.setProperty("azure.app.client.secret", "")
        System.setProperty("azure.openid.config.jwks.uri", "http://localhost:${azure.port()}/jwks")
        System.setProperty("azure.openid.config.issuer", "postmottak-backend")

        // Oppgavestyring
        System.setProperty("integrasjon.oppgavestyring.scope", "oppgavestyring")
        System.setProperty("integrasjon.oppgavestyring.url", "http://localhost:${oppgavestyring.port()}")

        // Behandlingsflyt
        System.setProperty("integrasjon.behandlingsflyt.scope", "behandlingsflyt")
        System.setProperty("integrasjon.behandlingsflyt.url", "http://localhost:${behandlkingsflyt.port()}")


        // Saf
        System.setProperty("integrasjon.saf.url.graphql", "http://localhost:${saf.port()}/graphql")
        System.setProperty("integrasjon.saf.scope", "saf")
        System.setProperty("integrasjon.saf.url.rest", "http://localhost:${saf.port()}/rest")

        // MEDL
        System.setProperty("integrasjon.medl.url", "http://localhost:${medl.port()}")
        System.setProperty("integrasjon.medl.scope", "medl")


        // Pesys
        System.setProperty("integrasjon.pesys.url", "http://localhost:${pesysFake.port()}")
        System.setProperty("integrasjon.pesys.scope", "scope")

        // Joark
        System.setProperty("integrasjon.joark.url", "http://localhost:${joark.port()}")
        System.setProperty("integrasjon.joark.scope", "scope")

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
        azure.stop(0L, 0L)
        oppgavestyring.stop(0L, 0L)
        saf.stop(0L, 0L)
        medl.stop(0L, 0L)
        joark.stop(0, 0)
    }

    fun leggTil(person: TestPerson) {
        person.identer.forEach { fakePersoner[it.identifikator] = person }
        person.barn.forEach { leggTil(it) }
    }

    private fun NettyApplicationEngine.port(): Int =
        runBlocking { resolvedConnectors() }
            .first { it.type == ConnectorType.HTTP }
            .port

    private fun Application.oppgavestyringFake() {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
            }
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@oppgavestyringFake.log.info(
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
    }

    private fun Application.pesysFake() {
        install(ContentNegotiation) {
            jackson()
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@pesysFake.log.info("Inntekt :: Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                call.respond(status = HttpStatusCode.InternalServerError, message = ErrorRespons(cause.message))
            }
        }
        routing() {
            get("/vedtak/gradalderellerufore?fom={ting1}&sakstype={ting2}") {
                val ident = requireNotNull(call.request.header("Nav-Personident"))
                val uføregrad = fakePersoner[ident]?.uføre?.prosentverdi() ?: 0

                call.respond(HttpStatusCode.OK, uføregrad)
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
                call.respond(HttpStatusCode.NoContent)
            }
            patch("/rest/journalpostapi/v1/journalpost/{journalpostId}/ferdigstill") {
                call.receive<FerdigstillRequest>()
                call.respond(HttpStatusCode.NoContent)
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
                call.respond(Saksinfo(
                    (Math.random() * 9999999999).toLong().toString(),
                    Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2024, 1, 31)),
                ))
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
                // Smallest possible PDF
                // https://stackoverflow.com/a/17280876/1013553
                val samplePdf = this.javaClass.classLoader.getResourceAsStream("sample.pdf")
                call.respondOutputStream {
                    samplePdf.copyTo(this)
                }
            }
            post("/graphql") {
                val body = call.receive<String>()

                call.respondText(
                    """
                    { "data":
                    {"journalpost":
                        {
                          "journalpostId": "1",
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
                              "dokumentInfoId": "454268545",
                              "brevkode": "NAV 11-13.05",
                              "dokumentvarianter": [
                                {
                                "variantformat": "ORIGINAL",
                                "filtype": "JSON"
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

    private fun Application.medlFake() {
        install(ContentNegotiation) {
            jackson()
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@medlFake.log.info("MEDL :: Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                call.respond(status = HttpStatusCode.InternalServerError, message = ErrorRespons(cause.message))
            }
        }

        routing {
            get {
                call.response.header(HttpHeaders.ContentType, ContentType.Application.Json.toString())

                @Language("JSON") val respons =
                    """[
  {
    "unntakId": 100087727,
    "ident": "02429118789",
    "fraOgMed": "2021-07-08",
    "tilOgMed": "2022-07-07",
    "status": "GYLD",
    "statusaarsak": null,
    "medlem": true,
    "grunnlag": "grunnlag",
    "lovvalg": "lovvalg"
  },
  {
    "unntakId": 100087729,
    "ident": "02429118789",
    "fraOgMed": "2014-07-10",
    "tilOgMed": "2016-07-14",
    "status": "GYLD",
    "statusaarsak": null,
    "medlem": false,
    "grunnlag": "grunnlag",
    "lovvalg": "lovvalg"
  }
]"""

                call.respond(
                    respons
                )
            }
        }
    }


    private fun hentEllerGenererTestPerson(forespurtIdent: String): TestPerson {
        val person = fakePersoner[forespurtIdent]
        if (person == null) {
            fakePersoner[forespurtIdent] = TestPerson(
                identer = setOf(Ident(forespurtIdent)),
                fødselsdato = Fødselsdato(LocalDate.now().minusYears(30)),
            )
        }

        return fakePersoner[forespurtIdent]!!
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


    internal data class TestToken(
        val access_token: String,
        val refresh_token: String = "very.secure.token",
        val id_token: String = "very.secure.token",
        val token_type: String = "token-type",
        val scope: String? = null,
        val expires_in: Int = 3599,
    )
}