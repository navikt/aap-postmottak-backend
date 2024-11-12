package no.nav.aap.postmottak.fordeler

import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

const val topic = "team-aap.journalpost-til-arena"

typealias HendelsesId = String

class ArenaVideresender(
    private val producer: Producer<String, String>, private val hendelsesRepository: HendelsesRepository
) {

    fun sendJournalpostTilArena(hendelsesId: HendelsesId) {
        val hendelse = hendelsesRepository.hentHendelse(hendelsesId)
        val record = ProducerRecord(topic, hendelse.key, hendelse.value)
        producer.send(record)
    }
}