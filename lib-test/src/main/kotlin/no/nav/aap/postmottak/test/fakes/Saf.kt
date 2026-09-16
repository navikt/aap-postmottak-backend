package no.nav.aap.postmottak.test.fakes

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Dokument
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Filtype

fun Application.safFake(
    sakerRespons: String = ingenSakerRespons()
) {

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
            val journalpostId = call.parameters["journalpostId"]?.toLong()
            if (journalpostId == null) {
                call.respond(HttpStatusCode.BadRequest, "Mangler eller ugyldig journalpostId")
                return@get
            }
            val dokumentInfoId = call.parameters["dokumentInfoId"]

            val testJournalpost = TestJournalposter.hentJournalpost(journalpostId)
            // Om dokumentet som etterspørres er JSON avgjøres av variantene på dokumentet i
            // testJournalpost.dokumenter (satt sammen med digitalSøknad, se TestJournalPostBuilder).
            val erJsonDokument = testJournalpost?.dokumenter
                ?.find { it.dokumentInfoId.dokumentInfoId == dokumentInfoId }
                ?.varianter?.any { it.filtype == Filtype.JSON }
                ?: false

            if (erJsonDokument) {
                call.response.header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                call.respondText(
                    testJournalpost.digitalSøknad?.let { DefaultJsonMapper.toJson(it) } ?: "{}"
                )
                return@get
            }

            call.response.header(HttpHeaders.ContentType, ContentType.Application.Pdf.toString())
            call.respondOutputStream {
                val ressurs = this.javaClass.classLoader.getResourceAsStream("sample.pdf")
                    ?: error("Fant ikke sample.pdf på classpath")
                ressurs.copyTo(this)
            }
        }
        post("/graphql") {
            val body = call.receive<String>()

            if (body.contains("saker")) {
                call.respondText(sakerRespons)
            } else {
                val journalpostId = body.substringAfter("\"journalpostId\" : \"").substringBefore("\"").trim()
                this@safFake.log.info("Henter dokumenter for journalpost {}", journalpostId)

                val testJournalpost = TestJournalposter.hentJournalpost(journalpostId.toLong()) ?: TestJournalPost(
                    journalpostId = journalpostId.toLong()
                )

                call.respondText(
                    """
                    { "data":
                    {"journalpost":
                        {
                          "journalpostId": "$journalpostId",
                          "personident": "3",
                          "bruker": {
                            "id": "${finnBrukerId(testJournalpost)}",
                            "type": "${finnBrukerType(testJournalpost)}"
                          },
                          ${getAvsenderMottaker(testJournalpost)}
                          "tittel": "Søknad om AAP",
                          "journalstatus": "${testJournalpost.status.name}",
                          "journalfoerendeEnhet": ${finnJournalførendeEnhet(testJournalpost)},
                          "mottattDato": "2021-12-01",
                          "tema": "${testJournalpost.tema}",
                          "kanal": "${testJournalpost.kanal.name}",
                          "relevanteDatoer": [
                            {
                            "dato": "2020-12-01T10:00:00",
                            "datotype": "DATO_REGISTRERT"
                            }
                          ], 
                          "sak": ${finnSak(testJournalpost)},
                          "dokumenter": [
                            ${getDokumenter(testJournalpost)}
                           ]
                          }
                        }
                    }}
                """.trimIndent(),
                    contentType = ContentType.Application.Json
                )
            }
        }
    }
}

private fun getAvsenderMottaker(testJournalpost: TestJournalPost): String {
    val avsenderMottaker = testJournalpost.avsenderMottaker ?: return ""
    return """"avsenderMottaker": {
            "id": "${avsenderMottaker.id}",
            "type": "${avsenderMottaker.type}",
            "navn": "${avsenderMottaker.navn}"
        },"""
}

private fun dokumentTilGraphqlJson(dokument: Dokument): String {
    val varianter = dokument.varianter.joinToString(",\n") { variant ->
        """
            {
                "variantformat": "${variant.variantformat.name}",
                "filtype": "${variant.filtype.name}"
            }
        """
    }
    val tittel = dokument.tittel?.let { "\"$it\"" } ?: "null"
    return """
        {
            "tittel": $tittel,
            "dokumentInfoId": "${dokument.dokumentInfoId.dokumentInfoId}",
            "brevkode": "${dokument.brevkode}",
            "dokumentvarianter": [
                $varianter
            ]
        }
    """
}

private fun getDokumenter(testJournalpost: TestJournalPost): String {
    testJournalpost.dokumenter?.let { dokumenter ->
        return dokumenter.joinToString(",\n") { dokumentTilGraphqlJson(it) }
    }

    val legeerklæring = """       
        {
            "tittel": "Legeeerklæring",
            "dokumentInfoId": "4542685451",
            "brevkode": "NAV 08-07.08",
            "dokumentvarianter": [
                {
                    "variantformat": "ORIGINAL",
                    "filtype": "JSON"
                }
            ]
        }
        """
    val søknadjson = """
        {
            "tittel": "Dokumenttittel",
            "dokumentInfoId": "4542685451",
            "brevkode": "${testJournalpost.brevkode.kode}",
            "dokumentvarianter": [
            {
                "variantformat": "ORIGINAL",
                "filtype": "JSON"
            }
            ]
        }
    """

    val klage = """       
        {
            "tittel": "Ettersendelse til klage",
            "dokumentInfoId": "4542685451",
            "brevkode": "NAVe 90-00.08 K",
            "dokumentvarianter": [
                {
                    "variantformat": "ARKIV",
                    "filtype": "PDF"
                }
            ]
        }
        """

    val ensøknadogenpdf = """ {
            "tittel": "Dokumenttittel",
            "dokumentInfoId": "45426854351",
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
        "dokumentInfoId": "45426854352",
        "brevkode": null,
        "dokumentvarianter": [
        {
            "variantformat": "ARKIV",
            "filtype": "PDF"
        }
        ]
    } """

    if (testJournalpost.kanal == KanalFraKodeverk.SKAN_NETS) {
        return ensøknadogenpdf
    }

    return when (testJournalpost.brevkode) {
        Brevkoder.SØKNAD,
        Brevkoder.STANDARD_ETTERSENDING,
        Brevkoder.SØKNAD_OM_REISESTØNAD,
        Brevkoder.SØKNAD_OM_REISESTØNAD_ETTERSENDELSE
            -> søknadjson

        Brevkoder.LEGEERKLÆRING -> legeerklæring
        Brevkoder.KLAGE,
        Brevkoder.KLAGE_ETTERSENDELSE -> klage

        // todo, gjør også dette programmerbart
        Brevkoder.ANKE,
        Brevkoder.ANKE_ETTERSENDELSE,
        Brevkoder.BREV_UTLAND,
        Brevkoder.EGENERKLÆRING_AAP_EØS,
        Brevkoder.MELDEKORT,
        Brevkoder.MELDEKORT_KORRIGERING,
        Brevkoder.ANNEN -> ensøknadogenpdf
    }
}

private fun finnSak(testJournalpost: TestJournalPost): String {
    val fagsak = testJournalpost.fagsak
    return when {
        fagsak != null -> """{
            "fagsakId": "${fagsak.fagsakId}",
            "fagsaksystem": "${fagsak.fagsaksystem?.name}",
            "sakstype": "${fagsak.sakstype.name}"
        }"""

        else -> "null"
    }
}

private fun ingenSakerRespons() =
    """
        {
            "data": {
                "saker": []
            }
        }
    """

private fun finnBrukerId(testJournalpost: TestJournalPost) = testJournalpost.brukerId

private fun finnBrukerType(testJournalpost: TestJournalPost) = testJournalpost.brukerType

private fun finnJournalførendeEnhet(testJournalpost: TestJournalPost) =
    testJournalpost.journalførendeEnhet?.let { "\"$it\"" } ?: "null"
