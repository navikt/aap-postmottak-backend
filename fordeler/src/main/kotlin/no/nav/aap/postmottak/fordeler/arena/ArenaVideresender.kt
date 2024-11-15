package no.nav.aap.postmottak.fordeler.arena

import no.nav.aap.postmottak.fordeler.HendelsesRepository
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

const val ARENA_VIDERESEND_TOPIC = "aap.journalpost-til-arena"

typealias HendelsesId = String

class ArenaVideresender(
    private val producer: Producer<String, String>, private val hendelsesRepository: HendelsesRepository
) {

    fun sendJournalpostTilArena(hendelsesId: HendelsesId) {
        val hendelse = hendelsesRepository.hentHendelse(hendelsesId)
        val record = ProducerRecord(ARENA_VIDERESEND_TOPIC, hendelse.hendelsesid, hendelse.hendelse)
        producer.send(record)
    }
}