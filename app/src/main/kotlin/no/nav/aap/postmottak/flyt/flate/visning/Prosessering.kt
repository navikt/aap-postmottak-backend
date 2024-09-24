package no.nav.aap.postmottak.flyt.flate.visning

import no.nav.aap.motor.api.JobbInfoDto

data class Prosessering(val status: ProsesseringStatus, val ventendeOppgaver: List<JobbInfoDto>)
