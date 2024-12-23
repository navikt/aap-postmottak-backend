package no.nav.aap.postmottak.avklaringsbehov.løser

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.løsning.AvklarSaksnummerLøsning
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.SaksnummerRepository
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.Saksvurdering

class AvklarSakLøser(val connection: DBConnection) : AvklaringsbehovsLøser<AvklarSaksnummerLøsning> {

    private val repositoryProvider = RepositoryProvider(connection)
    private val saksnummerRepository = repositoryProvider.provide(SaksnummerRepository::class)
    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarSaksnummerLøsning): LøsningsResultat {

        val saksvurdering = Saksvurdering(løsning.saksnummer, løsning.opprettNySak, løsning.førPåGenerellSak)

        saksnummerRepository.lagreSakVurdering(kontekst.kontekst.behandlingId, saksvurdering)

        return LøsningsResultat("Dokument er tildelt sak ${løsning.saksnummer}")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SAK
    }
}
