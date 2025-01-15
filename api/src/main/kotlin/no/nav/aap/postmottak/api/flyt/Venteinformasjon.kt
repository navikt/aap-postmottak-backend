package no.nav.aap.postmottak.api.flyt

import no.nav.aap.postmottak.avklaringsbehov.løser.ÅrsakTilSettPåVent
import java.time.LocalDate

class Venteinformasjon(val frist: LocalDate, val begrunnelse: String, val grunn: ÅrsakTilSettPåVent)