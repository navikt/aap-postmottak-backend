package no.nav.aap.postmottak.avklaringsbehov.løser

import no.nav.aap.fordeler.arena.AapSystem
import no.nav.aap.fordeler.arena.AvklarFordelingRepository
import no.nav.aap.fordeler.arena.AvklarFordelingVurdering
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.httpclient.error.BadRequestHttpResponsException
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.løsning.AvklarFordelingLøsning
import no.nav.aap.postmottak.avklaringsbehov.løsning.FordelingSystemValg
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

private val log = LoggerFactory.getLogger(AvklarFordelingLøser::class.java)

class AvklarFordelingLøser(
    private val avklarFordelingRepository: AvklarFordelingRepository,
) : AvklaringsbehovsLøser<AvklarFordelingLøsning> {

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarFordelingLøsning): LøsningsResultat {
        val system = when (løsning.valgtSystem) {
            FordelingSystemValg.ARENA -> AapSystem.ARENA
            FordelingSystemValg.KELVIN -> AapSystem.KELVIN
            FordelingSystemValg.BEGGE ->
                // Ruting til både Arena og Kelvin er ikke implementert enda – blokker på løsing.
                throw BadRequestHttpResponsException(
                    "Fordeling til både Arena og Kelvin er ikke støttet enda"
                )
        }

        log.info("Manuell fordeling vurdert til $system for behandling ${kontekst.kontekst.behandlingId}")

        avklarFordelingRepository.lagreVurdering(
            kontekst.kontekst.behandlingId,
            AvklarFordelingVurdering(
                system = system,
                vurdertAv = kontekst.bruker.ident,
                vurdertTidspunkt = LocalDateTime.now(),
                kommentar = løsning.kommentar,
            )
        )

        return LøsningsResultat("Søknaden er manuelt vurdert til $system")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_FORDELING
    }

    companion object : LøserKonstruktør<AvklarFordelingLøsning> {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): AvklaringsbehovsLøser<AvklarFordelingLøsning> {
            return AvklarFordelingLøser(
                repositoryProvider.provide(AvklarFordelingRepository::class)
            )
        }
    }
}

