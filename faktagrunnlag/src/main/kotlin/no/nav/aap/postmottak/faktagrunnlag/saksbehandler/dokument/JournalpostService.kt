package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument

import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.postmottak.saf.graphql.SafGraphqlClient
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.klient.pdl.PdlGraphQLClient
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.saf.graphql.Journalstatus
import no.nav.aap.postmottak.saf.graphql.SafDatoType
import no.nav.aap.postmottak.saf.graphql.SafJournalpost
import no.nav.aap.postmottak.sakogbehandling.journalpost.Dokument
import no.nav.aap.postmottak.sakogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.sakogbehandling.journalpost.Filtype
import no.nav.aap.postmottak.sakogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.sakogbehandling.journalpost.JournalpostStatus
import no.nav.aap.postmottak.sakogbehandling.journalpost.Person
import no.nav.aap.postmottak.sakogbehandling.journalpost.Variantformat
import no.nav.aap.verdityper.flyt.FlytKontekst
import org.slf4j.LoggerFactory

class JournalpostService private constructor(
    private val journalpostRepository: JournalpostRepository,
    private val safGraphqlClient: SafGraphqlClient,
    private val pdlGraphQLClient: PdlGraphQLClient,
    private val personRepository: PersonRepository
) : Informasjonskrav {
    private val log = LoggerFactory.getLogger(JournalpostService::class.java)


    companion object : Informasjonskravkonstruktør {
        override fun konstruer(connection: DBConnection): JournalpostService {
            return JournalpostService(
                JournalpostRepositoryImpl(connection),
                SafGraphqlClient.withClientCredentialsRestClient(),
                PdlGraphQLClient.withClientCredentialsRestClient(),
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

    fun hentjournalpost(journalpostId: JournalpostId): Journalpost {
        val journalpost = safGraphqlClient.hentJournalpost(journalpostId)

        require(journalpost.bruker?.id != null) { "journalpost må ha ident" }
        val identliste = pdlGraphQLClient.hentAlleIdenterForPerson(journalpost.bruker?.id!!)
        if (identliste.isEmpty()) {
            throw IllegalStateException("Fikk ingen treff på ident i PDL")
        }

        val person = personRepository.finnEllerOpprett(identliste)

        return journalpost.tilJournalpost(person)

    }

}

fun SafJournalpost.tilJournalpost(person: Person): Journalpost {
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
            Dokument(
                dokument.dokumentInfoId.let(::DokumentInfoId),
                Variantformat.valueOf(variant.variantformat.name),
                Filtype.valueOf(variant.filtype),
                dokument.brevkode,
            )
        }
    } ?: emptyList()
    return Journalpost(
        person = person,
        journalpostId = journalpost.journalpostId.let(::JournalpostId),
        status = finnJournalpostStatus(journalpost.journalstatus),
        tema = journalpost.tema,
        journalførendeEnhet = journalpost.journalfoerendeEnhet,
        mottattDato = mottattDato,
        dokumenter = dokumenter
    )
}
