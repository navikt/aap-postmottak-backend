package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.register.PersonService
import no.nav.aap.postmottak.gateway.BrukerIdType
import no.nav.aap.postmottak.gateway.JournalpostGateway
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.gateway.SafDatoType
import no.nav.aap.postmottak.gateway.SafJournalpost
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.AvsenderMottaker
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Dokument
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Filtype
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Variant
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Variantformat
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

class JournalpostService(
    private val journalpostGateway: JournalpostGateway,
    private val personService: PersonService
) {
    companion object {
        fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JournalpostService {
            return JournalpostService(
                gatewayProvider.provide(),
                PersonService.konstruer(repositoryProvider, gatewayProvider)
            )
        }
    }

    fun hentJournalpost(
        journalpostId: JournalpostId
    ): Journalpost {
        val journalpost = hentSafJournalpost(journalpostId)
        return tilJournalpostMedDokumentTitler(journalpost)
    }

    fun tilJournalpostMedDokumentTitler(safJournalpost: SafJournalpost): Journalpost {
        requireNotNull(safJournalpost.bruker?.id) { "Journalpost ${safJournalpost.journalpostId} har ikke brukerid" }
        if (safJournalpost.bruker.type == BrukerIdType.ORGNR) {
            throw IllegalStateException(
                "Kan ikke kjøre finnOgOppdaterPerson for brukertype ORGNR " +
                        "(orgnr=${safJournalpost.bruker.id}, status=${safJournalpost.journalstatus})"
            )
        }

        val person = personService.finnOgOppdaterPerson(safJournalpost.bruker.id)
        return safJournalpost.tilJournalpost(person)
    }

    fun hentSafJournalpost(journalpostId: JournalpostId): SafJournalpost {
        return journalpostGateway.hentJournalpost(journalpostId)
    }
}

fun SafJournalpost.tilJournalpost(person: Person): Journalpost {
    val journalpost = this

    val mottattTid = journalpost.relevanteDatoer
        ?.find { dato -> dato?.datotype == SafDatoType.DATO_REGISTRERT }
        ?.dato
        ?: error("Fant ikke dato for journalpost ${journalpost.journalpostId}. Tilgjengelige datoer: ${journalpost.relevanteDatoer}")
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
            }.orEmpty()
        )
    }.orEmpty()

    return Journalpost(
        person = person,
        journalpostId = journalpost.journalpostId.let(::JournalpostId),
        status = journalpost.journalstatus ?: Journalstatus.UKJENT,
        tema = journalpost.tema,
        behandlingstema = journalpost.behandlingstema,
        tittel = journalpost.tittel,
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
