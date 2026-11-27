package no.nav.aap.postmottak.flyt.ventebehov

import no.nav.aap.postmottak.avklaringsbehov.Avklaringsbehov
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.under18.Under18Repository

class Under18SamtykkeVurderingVentebehovEvaluerer(
    private val under18Repository: Under18Repository,
) : SpesifikkVentebehovEvaluerer {
    override fun definisjon(): Definisjon = Definisjon.VENT_PA_UNDER18_SAMTYKKE

    override fun ansesSomLøst(kontekst: FlytKontekst, avklaringsbehov: Avklaringsbehov): Boolean =
        under18Repository.hentSøknadsavklaring(kontekst.behandlingId)?.fylt17ÅrOg9Måneder == false ||
            under18Repository.hentAktivSamtykkevurdering(kontekst.behandlingId)?.beggeForeldreSamtykket == true
}
