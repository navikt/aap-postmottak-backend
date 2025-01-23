package no.nav.aap.fordeler.arena.jobber

import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

open class ArenaBaseKontekst(open val journalpostId: JournalpostId)

fun JobbInput.getBaseKOntekst() = DefaultJsonMapper.fromJson(this.payload(), ArenaBaseKontekst::class.java)