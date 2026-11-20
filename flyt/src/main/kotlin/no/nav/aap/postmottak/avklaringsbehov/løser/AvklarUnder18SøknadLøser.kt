package no.nav.aap.postmottak.avklaringsbehov.løser

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.løsning.AvklarUnder18SøknadLøsning
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.under18.Under18Repository
import no.nav.aap.postmottak.under18.Under18Søknadsavklaring
import no.nav.aap.postmottak.under18.Under18Søknadssituasjon

class AvklarUnder18SøknadLøser(
    private val under18Repository: Under18Repository,
) : AvklaringsbehovsLøser<AvklarUnder18SøknadLøsning> {

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarUnder18SøknadLøsning): LøsningsResultat {
        val avklaring = Under18Søknadsavklaring(løsning.situasjon, løsning.søkerVilFortsette)
        under18Repository.lagreSøknadsavklaring(kontekst.kontekst.behandlingId, avklaring)

        return LøsningsResultat(
            when {
                løsning.situasjon == Under18Søknadssituasjon.SØKER_FOR_UNG -> "Henleggelse bekreftet: søker er for ung"
                løsning.søkerVilFortsette -> "Søker har fylt 18 år og vil fortsette søknaden"
                else -> "Søker har fylt 18 år og vil ikke fortsette søknaden"
            }
        )
    }

    override fun forBehov(): Definisjon = Definisjon.AVKLAR_UNDER18_SØKNAD

    companion object : LøserKonstruktør<AvklarUnder18SøknadLøsning> {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): AvklaringsbehovsLøser<AvklarUnder18SøknadLøsning> = AvklarUnder18SøknadLøser(repositoryProvider.provide())
    }
}
