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
val SØKNAD_ETTERSENDELSE = JournalpostId(1000)
val UTEN_AVSENDER_MOTTAKER = JournalpostId(11)
val LEGEERKLÆRING = JournalpostId(120)
val ANNET_TEMA = JournalpostId(121)
val UGYLDIG_STATUS = JournalpostId(122)


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
            val journalpost = call.parameters["journalpostId"]?.toLong()
            when (journalpost) {
                DIGITAL_SØKNAD_ID.referanse -> {
                    call.response.header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    call.respondText("""{"yrkesskade":"nei", "student":{"erStudent": "nei"}}""")
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

            if (body.contains("saker")) {
                call.respondText(sakerRespons)
            } else {

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
                          "journalstatus": "${finnStatus(journalpostId.toLong())}",
                          "journalførendeEnhet": {"nr": 3001},
                          "mottattDato": "2021-12-01",
                          "tema": "${finnTema(journalpostId.toLong())}",
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
}

private fun getAvsenderMottaker(journalpostId: Long) =
    when (journalpostId) {
        UTEN_AVSENDER_MOTTAKER.referanse -> ""
        else -> """"avsenderMottaker": {
            "id": "213453452",
            "type": "FNR"
        },"""
    }

private fun finnTema(journalpostId: Long) = 
    when (journalpostId) {
        ANNET_TEMA.referanse -> "ANNET"
        else -> "AAP"
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

        SØKNAD_ETTERSENDELSE.referanse -> """       
        {
            "tittel": "Dokumenttittel",
            "dokumentInfoId": "4542685451",
            "brevkode": "NAVe 11-13.05",
            "dokumentvarianter": [
                {
                    "variantformat": "ORIGINAL",
                    "filtype": "JSON"
                }
            ]
        }
        """

        LEGEERKLÆRING.referanse -> """       
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

        else -> """ {
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
        "dokumentInfoId": "2",
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

    }

private fun ingenSakerRespons() =
    """
        {
            "data": {
                "saker": []
            }
        }
    """

fun arenaSakerRespons() =
    """
        {
            "data": {
                "saker": [
                    {
                        "fagsaksystem": "AO01",
                        "tema": "AAP"
                    }
                ]
            }
        }
    """

private fun finnStatus(journalpostId: Long) =
    when (journalpostId) {
        UGYLDIG_STATUS.referanse -> "UTGAAR"
        else -> "MOTTATT"
    }