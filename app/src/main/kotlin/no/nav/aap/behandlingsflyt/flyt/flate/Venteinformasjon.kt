package no.nav.aap.behandlingsflyt.flyt.flate

import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.ÅrsakTilSettPåVent
import java.time.LocalDate

class Venteinformasjon(val frist: LocalDate, val begrunnelse: String, val grunn: ÅrsakTilSettPåVent)