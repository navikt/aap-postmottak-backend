package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.postmottak.faktagrunnlag.register.PersonService
import no.nav.aap.postmottak.gateway.JournalpostGateway
import no.nav.aap.postmottak.gateway.JournalpostOboGateway
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.gateway.SafDatoType
import no.nav.aap.postmottak.gateway.SafJournalpost
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.AvsenderMottaker
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
        return tilJournalpostMedDokumentTitler(journalpost)
    }
    
    fun tilJournalpostMedDokumentTitler(safJournalpost: SafJournalpost): JournalpostMedDokumentTitler {
        requireNotNull (safJournalpost.bruker?.id) { "Journalpost har ikke brukerid" }
        val person = personService.finnOgOppdaterPerson(safJournalpost.bruker?.id!!)
        return safJournalpost.tilJournalpostMedDokumentTitler(person)
    }

    fun hentSafJournalpost(journalpostId: JournalpostId, token: OidcToken? = null): SafJournalpost {
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
        requireNotNull (journalpost.bruker?.id) { "Journalpost har ikke brukerid" }
        val person = personService.finnOgOppdaterPerson(journalpost.bruker?.id!!)
        return journalpost.tilJournalpost(person)
    }

}

fun SafJournalpost.tilJournalpostMedDokumentTitler(person: Person): JournalpostMedDokumentTitler {
    val journalpost = this

    val mottattTid = journalpost.relevanteDatoer
        ?.find { dato -> dato?.datotype == SafDatoType.DATO_REGISTRERT }
        ?.dato
        ?: error("Fant ikke dato")
    val mottattDato = mottattTid.toLocalDate()

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
        tittel = journalpost.tittel,
        journalførendeEnhet = journalpost.journalfoerendeEnhet,
        mottattDato = mottattDato,
        mottattTid = mottattTid,
        dokumenter = dokumenter,
        kanal = journalpost.kanal,
        saksnummer = sak?.fagsakId,
        fagsystem = sak?.fagsaksystem
    )
}

fun SafJournalpost.tilJournalpost(person: Person): Journalpost {
    val journalpost = this

    val mottattTid = journalpost.relevanteDatoer
        ?.find { dato -> dato?.datotype == SafDatoType.DATO_REGISTRERT }
        ?.dato
        ?: error("Fant ikke dato")
    val mottattDato = mottattTid.toLocalDate()

    val dokumenter = journalpost.dokumenter?.filterNotNull()?.map { dokument ->
        Dokument(
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

    return Journalpost(
        person = person,
        journalpostId = journalpost.journalpostId.let(::JournalpostId),
        status = journalpost.journalstatus ?: Journalstatus.UKJENT,
        tema = journalpost.tema,
        behandlingstema = journalpost.behandlingstema,
        tittel = "Tittel på journalposten",
        journalførendeEnhet = journalpost.journalfoerendeEnhet,
        mottattDato = mottattDato,
        mottattTid = mottattTid,
        avsenderMottaker = journalpost.avsenderMottaker?.let {
            AvsenderMottaker(it.id, it.type?.name, it.navn)
        },
        dokumenter = dokumenter,
        kanal = journalpost.kanal,
        saksnummer = sak?.fagsakId,
        fagsystem = sak?.fagsaksystem
    )
}
