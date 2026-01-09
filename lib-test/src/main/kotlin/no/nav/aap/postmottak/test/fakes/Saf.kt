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
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.StudentStatus
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadStudentDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.postmottak.gateway.Fagsystem
import no.nav.aap.postmottak.gateway.Sakstype
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk

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
                TestJournalposter.DIGITAL_SØKNAD_ID.referanse -> {
                    call.response.header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    call.respondText(
                        DefaultJsonMapper.toJson(
                            SøknadV0(
                                student = SøknadStudentDto(erStudent = StudentStatus.Nei),
                                yrkesskade = "nei",
                                oppgitteBarn = null,
                                medlemskap = null,
                            )
                        )
                    )
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
                            "id": "${finnBruker(journalpostId.toLong())}",
                            "type": "FNR"
                          },
                          ${getAvsenderMottaker(journalpostId.toLong())}
                          "tittel": "Søknad om AAP",
                          "journalstatus": "${finnStatus(journalpostId.toLong())}",
                          "journalførendeEnhet": {"nr": 3001},
                          "mottattDato": "2021-12-01",
                          "tema": "${finnTema(journalpostId.toLong())}",
                          "kanal": "${finnKanal(journalpostId.toLong())}",
                          "relevanteDatoer": [
                            {
                            "dato": "2020-12-01T10:00:00",
                            "datotype": "DATO_REGISTRERT"
                            }
                          ], 
                          "sak": ${finnSak(journalpostId.toLong())},
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
        TestJournalposter.UTEN_AVSENDER_MOTTAKER.referanse -> ""
        TestJournalposter.LEGEERKLÆRING_TRUKKET_SAK.referanse -> """"avsenderMottaker": {
            "id": "21345345210",
            "type": "FNR",
            "navn": "Test Testesen"
        },"""

        else -> """"avsenderMottaker": {
            "id": "0000000444",
            "type": "FNR",
            "navn": "Test Testesen"
        },"""
    }

private fun finnKanal(journalpostId: Long) =
    when (journalpostId) {
        TestJournalposter.DIGITAL_SØKNAD_ID.referanse -> KanalFraKodeverk.NAV_NO.name
        TestJournalposter.PAPIR_SØKNAD.referanse -> KanalFraKodeverk.SKAN_NETS.name
        else -> KanalFraKodeverk.UKJENT.name
    }

private fun finnTema(journalpostId: Long) =
    when (journalpostId) {
        TestJournalposter.ANNET_TEMA.referanse -> "ANNET"
        else -> "AAP"
    }

private fun getDokumenter(journalpostId: Long): String {
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

    return when (journalpostId) {
        TestJournalposter.DIGITAL_SØKNAD_ID.referanse -> """
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

        TestJournalposter.SØKNAD_ETTERSENDELSE.referanse -> """       
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

        TestJournalposter.LEGEERKLÆRING.referanse, TestJournalposter.LEGEERKLÆRING_TRUKKET_SAK.referanse, TestJournalposter.LEGEERKLÆRING_IKKE_TIL_KELVIN.referanse -> legeerklæring

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
}

private fun finnSak(journalpostId: Long) =
    when (journalpostId) {
        TestJournalposter.STATUS_JOURNALFØRT_ANNET_FAGSYSTEM.referanse -> """{
            "fagsakId": "123456",
            "fagsaksystem": "${Fagsystem.FS22.name}",
            "sakstype": "${Sakstype.GENERELL_SAK.name}"
        }"""

        else -> "null"
    }

private fun ingenSakerRespons() =
    """
        {
            "data": {
                "saker": []
            }
        }
    """

private fun finnStatus(journalpostId: Long) =
    when (journalpostId) {
        TestJournalposter.UGYLDIG_STATUS.referanse -> "UTGAAR"
        TestJournalposter.STATUS_JOURNALFØRT.referanse, TestJournalposter.STATUS_JOURNALFØRT_ANNET_FAGSYSTEM.referanse -> "JOURNALFOERT"
        else -> "MOTTATT"
    }

private fun finnBruker(journalpostId: Long) =
    when (journalpostId) {
        TestJournalposter.SØKNAD_ETTERSENDELSE.referanse,
        TestJournalposter.PERSON_UTEN_SAK_I_BEHANDLINGSFLYT.referanse,
        TestJournalposter.LEGEERKLÆRING_IKKE_TIL_KELVIN.referanse -> TestIdenter.IDENT_UTEN_SAK_I_KELVIN.identifikator

        TestJournalposter.PERSON_MED_SAK_I_ARENA.referanse -> TestIdenter.IDENT_MED_SAK_I_ARENA.identifikator
        TestJournalposter.LEGEERKLÆRING_TRUKKET_SAK.referanse -> TestIdenter.IDENT_MED_TRUKKET_SAK_I_KELVIN.identifikator
        else -> TestIdenter.DEFAULT_IDENT.identifikator
    }
