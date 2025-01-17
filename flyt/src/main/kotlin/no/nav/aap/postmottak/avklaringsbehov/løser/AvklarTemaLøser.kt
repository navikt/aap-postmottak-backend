package no.nav.aap.postmottak.avklaringsbehov.løser

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.løsning.AvklarTemaLøsning
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklartema.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklartema.Tema
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon

class AvklarTemaLøser(val connection: DBConnection) : AvklaringsbehovsLøser<AvklarTemaLøsning> {
    private val repositoryProvider = RepositoryProvider(connection)
    private val avklarTemaRepository = repositoryProvider.provide(AvklarTemaRepository::class)
    private val journalpostRepository = repositoryProvider.provide(JournalpostRepository::class)
    
    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarTemaLøsning): LøsningsResultat {
        val tema = utledTemaForJournalpost(kontekst.kontekst.behandlingId, løsning)
        avklarTemaRepository.lagreTemaAvklaring(kontekst.kontekst.behandlingId, løsning.skalTilAap, tema)
        
        return LøsningsResultat("Dokument er ${if (løsning.skalTilAap) "" else "ikke"} ment for AAP")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_TEMA
    }
    
    private fun utledTemaForJournalpost(behandlingId: BehandlingId, løsning: AvklarTemaLøsning): Tema{
        if (løsning.skalTilAap) {
            return Tema.AAP
        }
        
        val journalpost = journalpostRepository.hentHvisEksisterer(behandlingId) ?: error("Journalpost kan ikke være null")
        return if (journalpost.erDigitalLegeerklæring()) {
            Tema.OPP
        } else {
            Tema.UKJENT
        }
    }
}
