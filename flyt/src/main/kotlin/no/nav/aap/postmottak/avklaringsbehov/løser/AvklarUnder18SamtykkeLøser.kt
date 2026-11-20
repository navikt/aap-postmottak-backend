package no.nav.aap.postmottak.avklaringsbehov.løser

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.postmottak.avklaringsbehov.løsning.AvklarUnder18SamtykkeLøsning
import no.nav.aap.postmottak.gateway.Klokke
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.under18.Under18Aldersgruppe
import no.nav.aap.postmottak.under18.Under18Repository
import no.nav.aap.postmottak.under18.Under18SamtykkeVurdering
import no.nav.aap.postmottak.under18.hentFødselsdatoFor

class AvklarUnder18SamtykkeLøser(
    private val under18Repository: Under18Repository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val klokke: Klokke,
) : AvklaringsbehovsLøser<AvklarUnder18SamtykkeLøsning> {

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarUnder18SamtykkeLøsning): LøsningsResultat {
        under18Repository.lagreSamtykkevurdering(
            kontekst.kontekst.behandlingId,
            Under18SamtykkeVurdering(
                beggeForeldreSamtykket = løsning.beggeForeldreSamtykket,
                samtykkeJournalpostIder = løsning.samtykkeJournalpostIder.map(::JournalpostId),
            )
        )

        if (!løsning.beggeForeldreSamtykket) {
            val fødselsdato = under18Repository.hentFødselsdatoFor(kontekst.kontekst.behandlingId)
            if (Under18Aldersgruppe.vurder(fødselsdato, klokke.dagensDato()) == Under18Aldersgruppe.SAMTYKKE) {
                val vent = Definisjon.VENT_PA_UNDER18_SAMTYKKE
                avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.kontekst.behandlingId).leggTil(
                    definisjon = vent,
                    stegType = vent.løsesISteg,
                    grunn = ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER,
                    frist = minOf(vent.utledFrist(null), fødselsdato.plusYears(18)),
                )
            }
        }

        return LøsningsResultat(
            if (løsning.beggeForeldreSamtykket) "Begge foreldre har samtykket" else "Samtykke mangler"
        )
    }

    override fun forBehov(): Definisjon = Definisjon.AVKLAR_UNDER18_SAMTYKKE

    companion object : LøserKonstruktør<AvklarUnder18SamtykkeLøsning> {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): AvklaringsbehovsLøser<AvklarUnder18SamtykkeLøsning> =
            AvklarUnder18SamtykkeLøser(
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                gatewayProvider.provide(),
            )
    }
}
