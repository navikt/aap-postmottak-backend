package no.nav.aap.postmottak.flyt.flate

import no.nav.aap.postmottak.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import java.time.LocalDate

class Venteinformasjon(val frist: LocalDate, val begrunnelse: String, val grunn: ÅrsakTilSettPåVent)