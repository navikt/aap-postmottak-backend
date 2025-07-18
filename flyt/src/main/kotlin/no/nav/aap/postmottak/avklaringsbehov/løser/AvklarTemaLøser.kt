package no.nav.aap.postmottak.avklaringsbehov.løser

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.løsning.AvklarTemaLøsning
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.Tema
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon

class AvklarTemaLøser(private val avklarTemaRepository: AvklarTemaRepository) :
    AvklaringsbehovsLøser<AvklarTemaLøsning> {

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarTemaLøsning): LøsningsResultat {
        avklarTemaRepository.lagreTemaAvklaring(kontekst.kontekst.behandlingId, løsning.skalTilAap, utledTema(løsning))
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
        override fun konstruer(connection: DBConnection): AvklaringsbehovsLøser<AvklarTemaLøsning> {
            return AvklarTemaLøser(RepositoryProvider(connection).provide(AvklarTemaRepository::class))
        }
    }
}
