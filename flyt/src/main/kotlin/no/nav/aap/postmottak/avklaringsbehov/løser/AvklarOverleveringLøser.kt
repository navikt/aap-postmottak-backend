package no.nav.aap.postmottak.avklaringsbehov.løser

import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.løsning.AvklarOverleveringLøsning
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.overlever.OverleveringVurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.overlever.OverleveringVurderingRepository
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon

class AvklarOverleveringLøser(
    private val avklarOverleveringRepository: OverleveringVurderingRepository
) : AvklaringsbehovsLøser<AvklarOverleveringLøsning> {

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarOverleveringLøsning): LøsningsResultat {
        avklarOverleveringRepository.lagre(
            kontekst.kontekst.behandlingId,
            OverleveringVurdering(løsning.skalOverleveres)
        )
        return LøsningsResultat("Dokument skal ${if (løsning.skalOverleveres) "" else "ikke"} overleveres til Kelvin")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_OVERLEVERING
    }

    companion object : LøserKonstruktør<AvklarOverleveringLøsning> {
        override fun konstruer(repositoryProvider: RepositoryProvider): AvklaringsbehovsLøser<AvklarOverleveringLøsning> {
            return AvklarOverleveringLøser(repositoryProvider
                .provide())
        }
    }
}