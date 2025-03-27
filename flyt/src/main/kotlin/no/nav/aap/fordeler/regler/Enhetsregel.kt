package no.nav.aap.fordeler.regler

import no.nav.aap.fordeler.EnhetMedOppfølgingsKontor
import no.nav.aap.fordeler.Enhetsutreder
import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(Enhetsregel::class.java)
class Enhetsregel : Regel<EnhetsregelInput> {
    private val godkjenteEnheter = listOf(
        Enhet.NAV_ASKER,
        Enhet.NAV_UTLAND,
        Enhet.SYFA_INNLANDET
    )

    companion object : RegelFactory<EnhetsregelInput> {
        override val erAktiv = miljøConfig(prod = false, dev = true)
        override fun medDataInnhenting(connection: DBConnection?) = RegelMedInputgenerator(Enhetsregel(), EnhetsregelInputGenerator())
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


class EnhetsregelInputGenerator : InputGenerator<EnhetsregelInput> {
    override fun generer(input: RegelInput): EnhetsregelInput {
        val enheter = Enhetsutreder.konstruer().finnEnhetMedOppfølgingskontor(input.person)
        return EnhetsregelInput(enheter)
    }
}

data class EnhetsregelInput(
    val enheter: EnhetMedOppfølgingsKontor
)

enum class Enhet(val enhetNr: String) {
    NAV_ASKER("0220"),
    NAV_UTLAND("0393"),
    SYFA_INNLANDET("0491")
}