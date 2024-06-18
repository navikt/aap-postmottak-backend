package no.nav.aap.behandlingsflyt.flyt.flate

import no.nav.aap.motor.JobbStatus

class JobbDto(
    val type: String,
    val status: JobbStatus,
    val antallFeilendeForsøk: Int = 0,
    val feilmelding: String? = null
)