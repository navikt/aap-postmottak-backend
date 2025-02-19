package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.postmottak.faktagrunnlag.register.PersonService
import no.nav.aap.postmottak.gateway.JournalpostGateway
import no.nav.aap.postmottak.gateway.JournalpostOboGateway
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.gateway.SafDatoType
import no.nav.aap.postmottak.gateway.SafJournalpost
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Dokument
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentMedTittel
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Filtype
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.JournalpostMedDokumentTitler
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Variant
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Variantformat
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

class JournalpostService(
    private val journalpostGateway: JournalpostGateway,
    private val journalpostOboGateway: JournalpostOboGateway,
    private val personService: PersonService
) {
    companion object {
        fun konstruer(connection: DBConnection): JournalpostService {
            return JournalpostService(
                GatewayProvider.provide(JournalpostGateway::class),
                GatewayProvider.provide(JournalpostOboGateway::class),
                PersonService.konstruer(connection)
            )
        }
    }

    fun hentJournalpostMedDokumentTitler(
        journalpostId: JournalpostId,
        token: OidcToken? = null
    ): JournalpostMedDokumentTitler {
        val journalpost = hentSafJournalpost(journalpostId, token)
        val person = personService.finnOgOppdaterPerson(journalpost)
        return journalpost.tilJournalpostMedDokumentTitler(person)
    }

    private fun hentSafJournalpost(journalpostId: JournalpostId, token: OidcToken? = null): SafJournalpost {
        val journalpost = if (token != null) {
            journalpostOboGateway.hentJournalpost(
                journalpostId,
                token
            )
        } else {
            journalpostGateway.hentJournalpost(journalpostId)
        }
        return journalpost
    }

    fun hentJournalpost(journalpostId: JournalpostId, token: OidcToken? = null): Journalpost {
        val journalpost = if (token != null) {
            journalpostOboGateway.hentJournalpost(journalpostId, token)
        } else {
            journalpostGateway.hentJournalpost(journalpostId)
        }
        val person = personService.finnOgOppdaterPerson(journalpost)
        return journalpost.tilJournalpost(person)
    }

}

fun SafJournalpost.tilJournalpostMedDokumentTitler(person: Person): JournalpostMedDokumentTitler {
    val journalpost = this

    val mottattDato = journalpost.relevanteDatoer?.find { dato ->
        dato?.datotype == SafDatoType.DATO_REGISTRERT
    }?.dato?.toLocalDate() ?: error("Fant ikke dato")

    val dokumenter = journalpost.dokumenter?.filterNotNull()?.map { dokument ->
        DokumentMedTittel(
            dokumentInfoId = dokument.dokumentInfoId.let(::DokumentInfoId),
            brevkode = dokument.brevkode ?: "Ukjent",
            tittel = dokument.tittel ?: "Dokument uten tittel",
            varianter = dokument.dokumentvarianter?.map {
                Variant(
                    Filtype.valueOf(it.filtype),
                    Variantformat.valueOf(it.variantformat.name)
                )
            } ?: emptyList()
        )
    } ?: emptyList()

    return JournalpostMedDokumentTitler(
        person = person,
        journalpostId = journalpost.journalpostId.let(::JournalpostId),
        status = journalpost.journalstatus ?: Journalstatus.UKJENT,
        tema = journalpost.tema,
        behandlingstema = journalpost.behandlingstema,
        journalførendeEnhet = journalpost.journalfoerendeEnhet,
        mottattDato = mottattDato,
        dokumenter = dokumenter,
        kanal = journalpost.kanal,
        saksnummer = sak?.fagsakId,
        fagsystem = sak?.fagsaksystem
    )
}

fun SafJournalpost.tilJournalpost(person: Person): Journalpost {
    val journalpost = this

    val mottattDato = journalpost.relevanteDatoer?.find { dato ->
        dato?.datotype == SafDatoType.DATO_REGISTRERT
    }?.dato?.toLocalDate() ?: error("Fant ikke dato")

    val dokumenter = journalpost.dokumenter?.filterNotNull()?.map { dokument ->
        Dokument(
            dokumentInfoId = dokument.dokumentInfoId.let(::DokumentInfoId),
            brevkode = dokument.brevkode ?: "Ukjent",
            varianter = dokument.dokumentvarianter?.map {
                Variant(
                    Filtype.valueOf(it.filtype),
                    Variantformat.valueOf(it.variantformat.name)
                )
            } ?: emptyList()
        )
    } ?: emptyList()

    return Journalpost(
        person = person,
        journalpostId = journalpost.journalpostId.let(::JournalpostId),
        status = journalpost.journalstatus ?: Journalstatus.UKJENT,
        tema = journalpost.tema,
        behandlingstema = journalpost.behandlingstema,
        journalførendeEnhet = journalpost.journalfoerendeEnhet,
        mottattDato = mottattDato,
        dokumenter = dokumenter,
        kanal = journalpost.kanal,
        saksnummer = sak?.fagsakId,
        fagsystem = sak?.fagsaksystem
    )
}
