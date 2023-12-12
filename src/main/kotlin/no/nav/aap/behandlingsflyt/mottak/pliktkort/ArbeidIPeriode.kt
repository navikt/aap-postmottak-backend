package no.nav.aap.behandlingsflyt.mottak.pliktkort

import no.nav.aap.behandlingsflyt.Periode
import no.nav.aap.behandlingsflyt.underveis.regler.TimerArbeid

data class ArbeidIPeriode(val periode: Periode, val timerArbeid: TimerArbeid)
