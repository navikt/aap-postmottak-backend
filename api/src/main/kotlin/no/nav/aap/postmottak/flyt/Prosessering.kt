package no.nav.aap.postmottak.flyt

import no.nav.aap.motor.api.JobbInfoDto
import no.nav.aap.postmottak.flyt.flate.visning.ProsesseringStatus

data class Prosessering(val status: ProsesseringStatus, val ventendeOppgaver: List<JobbInfoDto>)
