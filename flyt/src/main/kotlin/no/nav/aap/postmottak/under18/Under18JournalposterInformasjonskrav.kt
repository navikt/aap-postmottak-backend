package no.nav.aap.postmottak.under18

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.IngenRegisterData
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravInput
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravRepository
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostInformasjonskrav
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

/**
 * Melder endring når det har kommet samtykkekandidater siden forrige kjøring av kravet for behandlingen.
 */
class Under18JournalposterInformasjonskrav(
    private val behandlingRepository: BehandlingRepository,
    private val informasjonskravRepository: InformasjonskravRepository,
    private val samtykkekandidater: Under18Samtykkekandidater,
) : Informasjonskrav<Under18JournalposterInformasjonskrav.Input, IngenRegisterData> {
    data class Input(
        val kandidater: List<JournalpostId>,
        val nyeKandidater: List<JournalpostId>,
    ) : InformasjonskravInput

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.UNDER18_JOURNALPOSTER
        override val avhengigheter = setOf(JournalpostInformasjonskrav)

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider,
        ) = Under18JournalposterInformasjonskrav(
            behandlingRepository = repositoryProvider.provide(),
            informasjonskravRepository = repositoryProvider.provide(),
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

    override fun hentData(input: Input) = IngenRegisterData

    override fun oppdater(input: Input, registerdata: IngenRegisterData, kontekst: FlytKontekst) =
        if (input.nyeKandidater.isNotEmpty()) ENDRET else IKKE_ENDRET
}
