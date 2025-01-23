package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.postmottak.gateway.Fagsystem
import no.nav.aap.postmottak.gateway.JournalpostGateway
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.gateway.PersondataGateway
import no.nav.aap.postmottak.gateway.SafDatoType
import no.nav.aap.postmottak.gateway.SafJournalpost
import no.nav.aap.postmottak.journalpostogbehandling.db.PersonRepository
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentMedTittel
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Filtype
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.JournalpostMedDokumentTitler
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Variant
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Variantformat
import org.slf4j.LoggerFactory

class JournalpostService private constructor(
    private val journalpostRepository: JournalpostRepository,
    private val journalpostGateway: JournalpostGateway,
    private val pdlGraphqlKlient: PersondataGateway,
    private val personRepository: PersonRepository
) : Informasjonskrav {
    private val log = LoggerFactory.getLogger(JournalpostService::class.java)


    companion object : Informasjonskravkonstruktør {
        override fun konstruer(connection: DBConnection): JournalpostService {
            val repositoryProvider = RepositoryProvider(connection)
            val journalpostGateway = GatewayProvider.provide(JournalpostGateway::class)
            return JournalpostService(
                repositoryProvider.provide(JournalpostRepository::class),
                journalpostGateway,
                GatewayProvider.provide(PersondataGateway::class),
                repositoryProvider.provide(PersonRepository::class)
            )
        }
    }

    override fun oppdater(kontekst: FlytKontekst): Informasjonskrav.Endret {
        val persistertJournalpost = journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)

        val journalpostId = kontekst.journalpostId

        val safJournalpost = hentSafJournalpost(journalpostId)

        require(safJournalpost.sak?.fagsaksystem != Fagsystem.AO01.name)

        val internJournalpost = tilInternJournalpost(safJournalpost)


        if (persistertJournalpost != internJournalpost) {
            log.info("Fant endringer i journalpost")
            journalpostRepository.lagre(internJournalpost)
            return ENDRET
        }

        return IKKE_ENDRET
    }

    fun hentjournalpost(journalpostId: JournalpostId): JournalpostMedDokumentTitler {
        val journalpost = hentSafJournalpost(journalpostId)
        return tilInternJournalpost(journalpost)
    }

    private fun hentSafJournalpost(journalpostId: JournalpostId): SafJournalpost {
        val journalpost = journalpostGateway.hentJournalpost(journalpostId)
        return journalpost
    }

    private fun tilInternJournalpost(safJournalpost: SafJournalpost): JournalpostMedDokumentTitler {
        require(safJournalpost.bruker?.id != null) { "journalpost må ha ident" }
        val identliste = pdlGraphqlKlient.hentAlleIdenterForPerson(safJournalpost.bruker?.id!!)

        if (identliste.isEmpty()) {
            throw IllegalStateException("Fikk ingen treff på ident i PDL")
        }
        val person = personRepository.finnEllerOpprett(identliste)

        return safJournalpost.tilJournalpost(person)
    }
}

fun SafJournalpost.tilJournalpost(person: Person): JournalpostMedDokumentTitler {
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
                    Variant(Filtype.valueOf(it.filtype),
                        Variantformat.valueOf(it.variantformat.name)) } ?: emptyList()
            )
    } ?: emptyList()
    return JournalpostMedDokumentTitler(
        person = person,
        journalpostId = journalpost.journalpostId.let(::JournalpostId),
        status = journalpost.journalstatus ?: Journalstatus.UKJENT,
        tema = journalpost.tema,
        journalførendeEnhet = journalpost.journalfoerendeEnhet,
        mottattDato = mottattDato,
        dokumenter = dokumenter,
        kanal = journalpost.kanal,

    )
}
