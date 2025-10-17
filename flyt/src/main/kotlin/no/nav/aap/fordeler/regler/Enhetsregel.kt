package no.nav.aap.fordeler.regler

import no.nav.aap.fordeler.EnhetMedOppfølgingsKontor
import no.nav.aap.fordeler.Enhetsutreder
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.kontrakt.enhet.GodkjentEnhet
import no.nav.aap.unleash.PostmottakFeature
import no.nav.aap.unleash.UnleashGateway
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(Enhetsregel::class.java)

class Enhetsregel(
    unleashGateway: UnleashGateway
) : Regel<EnhetsregelInput> {

    private val godkjenteEnheter =
        if (unleashGateway.isEnabled(PostmottakFeature.Oppskalering13oktober)) {
            GodkjentEnhet.entries
        } else {
            listOf(
                // Vest-Viken
                GodkjentEnhet.NAV_BÆRUM,
                GodkjentEnhet.NAV_ASKER,
                GodkjentEnhet.NAV_JEVNAKER,
                GodkjentEnhet.NAV_DRAMMEN,
                GodkjentEnhet.NAV_KONGSBERG,
                GodkjentEnhet.NAV_HOLE,
                GodkjentEnhet.NAV_HALLINGDAL,
                GodkjentEnhet.NAV_MIDTBUSKERUD,
                GodkjentEnhet.NAV_ØVRE_EIKER,
                GodkjentEnhet.NAV_LIER,
                GodkjentEnhet.NAV_NUMEDAL,
                GodkjentEnhet.NAV_RINGERIKE,
                GodkjentEnhet.EGEN_ANSATT_VEST_VIKEN,

                // Innlandet
                GodkjentEnhet.SYFA_INNLANDET,
                GodkjentEnhet.NAV_KONGSVINGER,
                GodkjentEnhet.NAV_HAMAR,
                GodkjentEnhet.NAV_RINGSAKER,
                GodkjentEnhet.NAV_LØTEN,
                GodkjentEnhet.NAV_STANGE,
                GodkjentEnhet.NAV_ODAL,
                GodkjentEnhet.NAV_EIDSKOG,
                GodkjentEnhet.NAV_SOLØR,
                GodkjentEnhet.NAV_ELVERUM,
                GodkjentEnhet.NAV_TRYSIL,
                GodkjentEnhet.NAV_ÅMOT,
                GodkjentEnhet.NAV_STORELVDAL,
                GodkjentEnhet.NAV_ENGERDAL,
                GodkjentEnhet.NAV_NORDØSTERDAL,
                GodkjentEnhet.NAV_LILLEHAMMER_GAUSDAL,
                GodkjentEnhet.NAV_GJØVIK,
                GodkjentEnhet.NAV_LESJA_DOVRE,
                GodkjentEnhet.NAV_LOM_SKJÅK,
                GodkjentEnhet.NAV_VÅGÅ,
                GodkjentEnhet.NAV_MIDTGUDBRANDSDAL,
                GodkjentEnhet.NAV_SEL,
                GodkjentEnhet.NAV_ØYER,
                GodkjentEnhet.NAV_ØSTRE_TOTEN,
                GodkjentEnhet.NAV_VESTRE_TOTEN,
                GodkjentEnhet.NAV_HADELAND,
                GodkjentEnhet.NAV_LAND,
                GodkjentEnhet.NAV_VALDRES,
                GodkjentEnhet.EGNE_ANSATTE_INNLANDET
            )
        }

    companion object : RegelFactory<EnhetsregelInput> {
        override val erAktiv = miljøConfig(prod = true, dev = false)
        override fun medDataInnhenting(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) =
            RegelMedInputgenerator(
                Enhetsregel(gatewayProvider.provide()),
                EnhetsregelInputGenerator(gatewayProvider)
            )
    }

    override fun vurder(input: EnhetsregelInput): Boolean {
        if (input.enheter.norgEnhet == null && input.enheter.oppfølgingsenhet == null) {
            log.info("Fant ikke enheter for person")
        }
        return (input.enheter.oppfølgingsenhet ?: input.enheter.norgEnhet) in godkjenteEnheter.map { it.enhetNr }
    }

    override fun regelNavn(): String {
        return this::class.simpleName!!
    }
}


class EnhetsregelInputGenerator(private val gatewayProvider: GatewayProvider) : InputGenerator<EnhetsregelInput> {
    override fun generer(input: RegelInput): EnhetsregelInput {
        val enheter = Enhetsutreder.konstruer(gatewayProvider).finnEnhetMedOppfølgingskontor(input.person)
        return EnhetsregelInput(enheter)
    }
}

data class EnhetsregelInput(
    val enheter: EnhetMedOppfølgingsKontor
)