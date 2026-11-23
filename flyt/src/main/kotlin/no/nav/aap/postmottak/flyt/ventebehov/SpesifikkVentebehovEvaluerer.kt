package no.nav.aap.postmottak.flyt.ventebehov

import no.nav.aap.postmottak.avklaringsbehov.Avklaringsbehov
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon

/**
 * Avgjør om et bestemt ventebehov er løst av noe annet enn at fristen er ute, for eksempel at et dokument er mottatt.
 * Nye evaluerere legges til i listen i `VentebehovEvaluererServiceImpl`. Bruk bare repositoryer som er registrert i
 * produksjon, siden alle evaluererne lages for hver prosessering av en behandling.
 */
interface SpesifikkVentebehovEvaluerer {
    fun definisjon(): Definisjon
    fun ansesSomLøst(kontekst: FlytKontekst, avklaringsbehov: Avklaringsbehov): Boolean
}
