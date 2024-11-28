package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.postmottak.klient.pdl.PdlGraphqlKlient
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.saf.graphql.Journalstatus
import no.nav.aap.postmottak.saf.graphql.SafDatoType
import no.nav.aap.postmottak.saf.graphql.SafGraphqlKlient
import no.nav.aap.postmottak.saf.graphql.SafJournalpost
import no.nav.aap.postmottak.sakogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.sakogbehandling.journalpost.DokumentMedTittel
import no.nav.aap.postmottak.sakogbehandling.journalpost.Filtype
import no.nav.aap.postmottak.sakogbehandling.journalpost.JournalpostMedDokumentTitler
import no.nav.aap.postmottak.sakogbehandling.journalpost.JournalpostStatus
import no.nav.aap.postmottak.sakogbehandling.journalpost.Person
import no.nav.aap.postmottak.sakogbehandling.journalpost.Variantformat
import no.nav.aap.verdityper.flyt.FlytKontekst
import org.slf4j.LoggerFactory

class JournalpostService private constructor(
    private val journalpostRepository: JournalpostRepository,
    private val safGraphqlKlient: SafGraphqlKlient,
    private val pdlGraphqlKlient: PdlGraphqlKlient,
    private val personRepository: PersonRepository
) : Informasjonskrav {
    private val log = LoggerFactory.getLogger(JournalpostService::class.java)


    companion object : Informasjonskravkonstruktør {
        override fun konstruer(connection: DBConnection): JournalpostService {
            return JournalpostService(
                JournalpostRepositoryImpl(connection),
                SafGraphqlKlient.withClientCredentialsRestClient(),
                PdlGraphqlKlient.withClientCredentialsRestClient(),
                PersonRepository(connection)
            )
        }
    }

    override fun oppdater(kontekst: FlytKontekst): Informasjonskrav.Endret {
        val persistertJournalpost = journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)

        val journalpostId = kontekst.journalpostId
        val internJournalpost = hentjournalpost(journalpostId)

        if (persistertJournalpost != internJournalpost) {
            log.info("Fant endringer i journalpost")
            journalpostRepository.lagre(internJournalpost)
            return ENDRET
        }

        return IKKE_ENDRET
    }

    fun hentjournalpost(journalpostId: JournalpostId): JournalpostMedDokumentTitler {
        val journalpost = safGraphqlKlient.hentJournalpost(journalpostId)

        require(journalpost.bruker?.id != null) { "journalpost må ha ident" }
        val identliste = pdlGraphqlKlient.hentAlleIdenterForPerson(journalpost.bruker?.id!!)
        if (identliste.isEmpty()) {
            throw IllegalStateException("Fikk ingen treff på ident i PDL")
        }

        val person = personRepository.finnEllerOpprett(identliste)

        return journalpost.tilJournalpost(person)

    }

}

fun SafJournalpost.tilJournalpost(person: Person): JournalpostMedDokumentTitler {
    val journalpost = this

    fun finnJournalpostStatus(status: Journalstatus?) = when (status) {
        Journalstatus.MOTTATT -> JournalpostStatus.MOTTATT
        else -> JournalpostStatus.UKJENT
    }

    val mottattDato = journalpost.relevanteDatoer?.find { dato ->
        dato?.datotype == SafDatoType.DATO_REGISTRERT
    }?.dato?.toLocalDate() ?: error("Fant ikke dato")

    val dokumenter = journalpost.dokumenter?.filterNotNull()?.flatMap { dokument ->
        dokument.dokumentvarianter.filterNotNull().map { variant ->
            DokumentMedTittel(
                dokument.dokumentInfoId.let(::DokumentInfoId),
                Variantformat.valueOf(variant.variantformat.name),
                Filtype.valueOf(variant.filtype),
                dokument.brevkode ?: "Ukjent",
                dokument.tittel ?: "Dokument uten tittel"
            )
        }
    } ?: emptyList()
    return JournalpostMedDokumentTitler(
        person = person,
        journalpostId = journalpost.journalpostId.let(::JournalpostId),
        status = finnJournalpostStatus(journalpost.journalstatus),
        tema = journalpost.tema,
        journalførendeEnhet = journalpost.journalfoerendeEnhet,
        mottattDato = mottattDato,
        dokumenter = dokumenter,
        kanal = journalpost.kanal
    )
}
