package no.nav.aap.postmottak.avklaringsbehov.løser

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.løsning.SattPåVentLøsning
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon

class SattPåVentLøser(private val repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) :
    AvklaringsbehovsLøser<SattPåVentLøsning> {

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: SattPåVentLøsning): LøsningsResultat {
        return LøsningsResultat("Tatt av vent")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.MANUELT_SATT_PÅ_VENT
    }

    companion object : LøserKonstruktør<SattPåVentLøsning> {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): AvklaringsbehovsLøser<SattPåVentLøsning> {
            return SattPåVentLøser(repositoryProvider, gatewayProvider)
        }
    }
}
