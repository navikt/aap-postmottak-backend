package no.nav.aap.postmottak.fordeler.arena

import no.nav.aap.postmottak.fordeler.HendelsesRepository
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.errors.TopicAuthorizationException
import org.slf4j.LoggerFactory

const val ARENA_VIDERESEND_TOPIC = "aap.journalpost-til-arena"

typealias HendelsesId = String

class AuthorizationException(override val message: String?, override val cause: Throwable): Exception()
class TopicWriteException(override val message: String?, override val cause: Throwable): Exception()

class ArenaProducer(
    private val producer: Producer<String, String>, private val hendelsesRepository: HendelsesRepository
) {

    private val log = LoggerFactory.getLogger(ArenaVideresender::class.java)

    fun sendJournalpostTilArena(hendelsesId: HendelsesId) {
        val hendelse = hendelsesRepository.hentHendelse(hendelsesId)
        val record = ProducerRecord(ARENA_VIDERESEND_TOPIC, hendelse.hendelsesid, hendelse.hendelse)
        log.info("Videresender medling $hendelsesId fra Joark til Arena")
        try {
            producer.send(record).get()
        } catch (e: TopicAuthorizationException) {
            throw AuthorizationException("Autoriseringsfeil ved sending av melding på topic: $ARENA_VIDERESEND_TOPIC", e)
        } catch (e: Exception) {
            throw TopicWriteException("Kunne ikke skrive melding til topic: $ARENA_VIDERESEND_TOPIC", e)
        }
    }
}