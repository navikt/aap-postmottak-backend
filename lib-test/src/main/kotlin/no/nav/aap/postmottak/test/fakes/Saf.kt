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
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId


val DIGITAL_SØKNAD_ID = JournalpostId(999)
val UTEN_AVSENDER_MOTTAKER = JournalpostId(11)


fun Application.safFake() {

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
            val journalpost = call.parameters["journalpostId"]?.toLong()
            when (journalpost) {
                DIGITAL_SØKNAD_ID.referanse -> {
                    call.response.header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    call.respondText("{}")
                }

                else -> {
                    call.response.header(HttpHeaders.ContentType, ContentType.Application.Pdf.toString())
                    call.respondOutputStream {
                        this.javaClass.classLoader.getResourceAsStream("sample.pdf").copyTo(this)
                    }
                }
            }

        }
        post("/graphql") {
            val body = call.receive<String>()
            val journalpostId = body.substringAfter("\"journalpostId\" : \"").substringBefore("\"").trim()
            this@safFake.log.info("Henter dokumenter for journalpost {}", journalpostId)

            call.respondText(
                """
                    { "data":
                    {"journalpost":
                        {
                          "journalpostId": "$journalpostId",
                          "tittel": "Overordnet tittel",
                          "personident": "3",
                          "bruker": {
                            "id": "213453452",
                            "type": "FNR"
                          },
                          ${getAvsenderMottaker(journalpostId.toLong())}
                          "status": "MOTTATT",
                          "journalførendeEnhet": {"nr": 3001},
                          "mottattDato": "2021-12-01",
                          "tema": "AAP",
                          "kanal": "UKJENT",
                          "relevanteDatoer": [
                            {
                            "dato": "2020-12-01T10:00:00",
                            "datotype": "DATO_REGISTRERT"
                            }
                          ], 
                          "dokumenter": [
                            ${getDokumenter(journalpostId.toLong())}
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

private fun getAvsenderMottaker(journalpostId: Long) =
    when (journalpostId) {
        UTEN_AVSENDER_MOTTAKER.referanse -> ""
        else -> """"avsenderMottaker": {
            "id": "213453452",
            "type": "FNR"
        },"""
    }


private fun getDokumenter(journalpostId: Long) =
    when (journalpostId) {
        DIGITAL_SØKNAD_ID.referanse -> """
        {
            "tittel": "Dokumenttittel",
            "dokumentInfoId": "4542685451",
            "brevkode": "NAV 11-13.05",
            "dokumentvarianter": [
            {
                "variantformat": "ORIGINAL",
                "filtype": "JSON"
            }
            ]
        }
    """

        else -> """ {
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
        "brevkode": null,
        "dokumentvarianter": [
        {
            "variantformat": "ARKIV",
            "filtype": "PDF"
        }
        ]
    } """

    }