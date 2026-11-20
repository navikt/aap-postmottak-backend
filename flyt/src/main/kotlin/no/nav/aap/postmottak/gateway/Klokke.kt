package no.nav.aap.postmottak.gateway

import no.nav.aap.komponenter.gateway.Gateway
import java.time.LocalDate

interface Klokke : Gateway {
    fun dagensDato(): LocalDate
}
