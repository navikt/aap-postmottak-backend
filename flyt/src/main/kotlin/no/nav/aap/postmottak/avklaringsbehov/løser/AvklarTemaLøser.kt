package no.nav.aap.postmottak.avklaringsbehov.løser

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.postmottak.avklaringsbehov.løsning.AvklarTemaLøsning
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.Tema
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.unleash.PostmottakFeature
import no.nav.aap.unleash.UnleashGateway

class AvklarTemaLøser(
    private val avklarTemaRepository: AvklarTemaRepository,
    private val avklaringsbehovOrkestrator: AvklaringsbehovOrkestrator,
    private val unleashGateway: UnleashGateway,
) :
    AvklaringsbehovsLøser<AvklarTemaLøsning> {

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarTemaLøsning): LøsningsResultat {
        val tema = utledTema(løsning)
        avklarTemaRepository.lagreTemaAvklaring(kontekst.kontekst.behandlingId, løsning.skalTilAap, tema)

        if (tema != Tema.AAP && unleashGateway.isEnabled(PostmottakFeature.LukkPostmottakEndreTemaBehandlinger)) {
            // Vi setter behandling på vent inntil den løses i GOSYS
            avklaringsbehovOrkestrator.settBehandlingPåVentForTemaEndring(
                kontekst.kontekst.behandlingId,
            )
        }

        return LøsningsResultat("Dokument er ${if (løsning.skalTilAap) "" else "ikke"} ment for AAP")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_TEMA
    }

    private fun utledTema(løsning: AvklarTemaLøsning): Tema {
        return if (løsning.skalTilAap) {
            Tema.AAP
        } else {
            Tema.UKJENT
        }
    }

    companion object : LøserKonstruktør<AvklarTemaLøsning> {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): AvklaringsbehovsLøser<AvklarTemaLøsning> {
            return AvklarTemaLøser(
                repositoryProvider.provide(),
                AvklaringsbehovOrkestrator(repositoryProvider, gatewayProvider),
                gatewayProvider.provide(UnleashGateway::class)
            )
        }
    }
}
