package no.nav.aap.postmottak.flyt.ventebehov

import no.nav.aap.postmottak.avklaringsbehov.Avklaringsbehov
import no.nav.aap.postmottak.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst

interface VentebehovEvaluererService {
    fun løsVentebehov(kontekst: FlytKontekst, avklaringsbehovene: Avklaringsbehovene): List<Avklaringsbehov>
}
