package no.nav.aap.postmottak.under18

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravInput
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravRegisterdata
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravRepository
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostInformasjonskrav
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

/**
 * Henter og lagrer samtykkekandidatene til en under 18-behandling. Melder endring når det har kommet
 * kandidater siden forrige kjøring av kravet for behandlingen, også når en annen behandling allerede har
 * lagret journalposten.
 */
class Under18JournalposterInformasjonskrav(
    private val behandlingRepository: BehandlingRepository,
    private val journalpostRepository: JournalpostRepository,
    private val informasjonskravRepository: InformasjonskravRepository,
    private val journalpostService: JournalpostService,
    private val samtykkekandidater: Under18Samtykkekandidater,
) : Informasjonskrav<Under18JournalposterInformasjonskrav.Input, Under18JournalposterInformasjonskrav.Registerdata> {
    data class Input(
        val kandidater: List<JournalpostId>,
        val nyeKandidater: List<JournalpostId>,
    ) : InformasjonskravInput

    data class Registerdata(val journalposter: List<Journalpost>) : InformasjonskravRegisterdata

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.UNDER18_JOURNALPOSTER
        override val avhengigheter = setOf(JournalpostInformasjonskrav)

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider,
        ) = Under18JournalposterInformasjonskrav(
            behandlingRepository = repositoryProvider.provide(),
            journalpostRepository = repositoryProvider.provide(),
            informasjonskravRepository = repositoryProvider.provide(),
            journalpostService = JournalpostService.konstruer(repositoryProvider, gatewayProvider),
            samtykkekandidater = Under18Samtykkekandidater.konstruer(repositoryProvider),
        )
    }

    override fun klargjør(kontekst: FlytKontekst): Input {
        val forrigeKandidater = informasjonskravRepository.hentOppdateringer(kontekst.journalpostId, listOf(navn))
            .firstOrNull { it.behandlingId == kontekst.behandlingId }
            ?.forrigeInput<Input>()
            ?.kandidater
            .orEmpty()
        val kandidater = samtykkekandidater.hent(behandlingRepository.hent(kontekst.behandlingId))
            .map { it.journalpostId }

        return Input(kandidater = kandidater, nyeKandidater = kandidater - forrigeKandidater.toSet())
    }

    override fun hentData(input: Input) =
        Registerdata(input.kandidater.map { journalpostService.hentJournalpost(it) })

    override fun oppdater(input: Input, registerdata: Registerdata, kontekst: FlytKontekst): Informasjonskrav.Endret {
        val endredeKandidater = registerdata.journalposter.filter { journalpost ->
            journalpostRepository.hentHvisEksisterer(journalpost.journalpostId) != journalpost
        }
        endredeKandidater.forEach { journalpostRepository.lagre(it) }

        return if (input.nyeKandidater.isNotEmpty()) ENDRET else IKKE_ENDRET
    }
}
