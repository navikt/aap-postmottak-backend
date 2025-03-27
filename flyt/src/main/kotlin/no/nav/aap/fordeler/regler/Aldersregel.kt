package no.nav.aap.fordeler.regler

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.postmottak.gateway.PersondataGateway
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.Period

class Aldersregel : Regel<AldersregelInput> {
    private val log = LoggerFactory.getLogger(Aldersregel::class.java)
    companion object : RegelFactory<AldersregelInput> {
        override val erAktiv = miljøConfig(prod = true, dev = true)
        const val MIN_ALDER = 22
        const val MAX_ALDER = 59
        override fun medDataInnhenting(connection: DBConnection?) =
            RegelMedInputgenerator(Aldersregel(), AldersregelInputGenerator())
    }

    override fun vurder(input: AldersregelInput): Boolean {
        if (input.fødselsdato == null) {
            log.info("Fant ikke fødselsdato for person i PDL - returnerer false")
            return false
        }
        val alder = Period.between(input.fødselsdato, input.nåDato).years
        return alder in MIN_ALDER..MAX_ALDER
    }

    override fun regelNavn(): String {
        return this::class.simpleName!!
    }

}

class AldersregelInputGenerator : InputGenerator<AldersregelInput> {
    override fun generer(input: RegelInput): AldersregelInput {
        val fnr = input.person.aktivIdent().identifikator
        val fødselsdato =
            GatewayProvider.provide(PersondataGateway::class).hentFødselsdato(fnr)
        
        return AldersregelInput(fødselsdato, LocalDate.now())
    }
}

data class AldersregelInput(
    val fødselsdato: LocalDate?,
    val nåDato: LocalDate
)