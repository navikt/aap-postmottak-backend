package no.nav.aap.postmottak.flyt.ventebehov

import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.SYSTEMBRUKER
import no.nav.aap.postmottak.avklaringsbehov.Avklaringsbehov
import no.nav.aap.postmottak.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst

class VentebehovEvaluererServiceImpl(
    private val evaluerere: List<SpesifikkVentebehovEvaluerer>,
) : VentebehovEvaluererService {

    constructor(repositoryProvider: RepositoryProvider) : this(
        evaluerere = listOf(Under18SamtykkeVurderingVentebehovEvaluerer(repositoryProvider.provide()))
    )

    private val fristEvaluerer = FristUtløptVentebehovEvaluerer

    private fun ansesSomLøst(kontekst: FlytKontekst, avklaringsbehov: Avklaringsbehov): Boolean {
        if (fristEvaluerer.ansesSomLøst(avklaringsbehov)) {
            return true
        }

        return evaluerere.filter { it.definisjon() == avklaringsbehov.definisjon }
            .any { it.ansesSomLøst(kontekst, avklaringsbehov) }
    }

    override fun løsVentebehov(
        kontekst: FlytKontekst,
        avklaringsbehovene: Avklaringsbehovene
    ): List<Avklaringsbehov> {
        val ventebehovSomErLøst = avklaringsbehovene.hentVentepunkter()
            .filter { behov -> ansesSomLøst(kontekst, behov) }

        for (ventebehov in ventebehovSomErLøst) {
            avklaringsbehovene.løsAvklaringsbehov(
                definisjon = ventebehov.definisjon,
                begrunnelse = "Ventebehov løst.",
                endretAv = SYSTEMBRUKER.ident
            )
        }

        return ventebehovSomErLøst
    }
}
