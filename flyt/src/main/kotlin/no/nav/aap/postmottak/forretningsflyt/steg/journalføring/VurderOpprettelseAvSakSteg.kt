package no.nav.aap.postmottak.forretningsflyt.steg.journalføring

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.fordeler.Fordelingsutfall
import no.nav.aap.fordeler.RegelRepository
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.steg.StegType
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(VurderOpprettelseAvSakSteg::class.java)

/**
 * Steg for manuell vurdering av om det skal opprettes en ny sak i Kelvin.
 *
 * Dersom [krevesManuellVurdering] returnerer `true` stopper flyten på et manuelt avklaringsbehov
 * ([Definisjon.VURDER_OPPRETTELSE_AV_SAK]) som løses av en saksbehandler
 * (se `VurderOpprettelseAvSakLøser`). Når saksbehandler har løst behovet, går flyten videre.
 */
class VurderOpprettelseAvSakSteg(
    private val journalpostRepository: JournalpostRepository,
    private val regelRepository: RegelRepository,
    private val avklaringsbehovService: AvklaringsbehovService,
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return VurderOpprettelseAvSakSteg(
                repositoryProvider.provide(JournalpostRepository::class),
                repositoryProvider.provide(RegelRepository::class),
                AvklaringsbehovService(repositoryProvider),
            )
        }

        override fun type(): StegType {
            return StegType.VURDER_OPPRETTELSE_AV_SAK
        }
    }

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val journalpost =
            requireNotNull(journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)) {
                "Journalpost mangler i VurderOpprettelseAvSakSteg."
            }

        avklaringsbehovService.oppdaterAvklaringsbehov(
            definisjon = Definisjon.VURDER_OPPRETTELSE_AV_SAK,
            vedtakBehøverVurdering = { krevesManuellVurdering(kontekst, journalpost) },
            erTilstrekkeligVurdert = { true },
            kontekst = kontekst,
        )

        return Fullført
    }

    /**
     * Avgjør om saksbehandler manuelt må vurdere hvor saken skal behandles.
     *
     * Kilden til sannhet er den maskinelle fordelingsvurderingen: dersom
     * [no.nav.aap.fordeler.Regelresultat.fordelingsutfall] er [Fordelingsutfall.MANUELL] stopper vi
     * for manuell vurdering. For journalposter uten regelresultat (f.eks. seedede/tekniske flyter)
     * eller med utfall Kelvin/Arena går flyten videre automatisk.
     */
    private fun krevesManuellVurdering(kontekst: FlytKontekst, journalpost: Journalpost): Boolean {
        if (journalpost.erUgyldig()) {
            return false
        }
        val trengerManuellVurdering =
            regelRepository.hentRegelresultat(kontekst.journalpostId)?.fordelingsutfall() == Fordelingsutfall.MANUELL
        if (!trengerManuellVurdering) {
            log.info("Fordeling tilsier ikke manuell vurdering av sak i Kelvin, går videre.")
        }
        return trengerManuellVurdering
    }
}
