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
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.gateway.BrukerIdType
import no.nav.aap.postmottak.journalpostogbehandling.Ident
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
    private val journalpostRepository: JournalpostRepository,
    private val journalpostService: JournalpostService,
) : Informasjonskrav<Under18JournalposterInformasjonskrav.Input, IngenRegisterData> {
    data class Input(
        val kandidater: List<JournalpostId>,
        val nyeKandidater: List<JournalpostId>,
        val identer: List<Ident> = emptyList(),
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
            journalpostRepository = repositoryProvider.provide(),
            journalpostService = JournalpostService.konstruer(repositoryProvider, gatewayProvider),
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

        val person = checkNotNull(journalpostRepository.hentHvisEksisterer(kontekst.journalpostId)?.person) {
            "Fant ikke person for hovedjournalposten til behandling ${kontekst.behandlingId}"
        }
        return Input(
            kandidater = kandidater,
            nyeKandidater = kandidater - forrigeKandidater.toSet(),
            identer = person.identer(),
        )
    }

    override fun hentData(input: Input): IngenRegisterData {
        input.kandidater.forEach { journalpostId ->
            val journalpost = journalpostService.hentSafJournalpost(journalpostId)
            check(
                !journalpost.bruker?.id.isNullOrBlank() &&
                    journalpost.bruker?.type in setOf(BrukerIdType.FNR, BrukerIdType.AKTOERID)
            ) {
                "Samtykkekandidat $journalpostId mangler en person"
            }
            check(journalpostService.hentIdenter(journalpost).any { it in input.identer }) {
                "Samtykkekandidat $journalpostId tilhører ikke søkeren"
            }
        }
        return IngenRegisterData
    }

    override fun oppdater(input: Input, registerdata: IngenRegisterData, kontekst: FlytKontekst) =
        if (input.nyeKandidater.isNotEmpty()) ENDRET else IKKE_ENDRET
}
