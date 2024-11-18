package no.nav.aap.postmottak.fordeler.arena

import no.nav.aap.postmottak.fordeler.HendelsesRepository
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

const val ARENA_VIDERESEND_TOPIC = "aap.journalpost-til-arena"

typealias HendelsesId = String

class ArenaProducer(
    private val producer: Producer<String, String>, private val hendelsesRepository: HendelsesRepository
) {

    private val log = LoggerFactory.getLogger(ArenaProducer::class.java)

    fun sendJournalpostTilArena(hendelsesId: HendelsesId) {
        val hendelse = hendelsesRepository.hentHendelse(hendelsesId)
        val record = ProducerRecord(ARENA_VIDERESEND_TOPIC, hendelse.hendelsesid, hendelse.hendelse)
        log.info("Publiserer til topic aap.journalpost-til-arena: Videresender melding $hendelsesId fra Joark til Arena")
        producer.send(record)
    }
}