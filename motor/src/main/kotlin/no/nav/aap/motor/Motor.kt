package no.nav.aap.motor

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

class Motor(
    private val dataSource: DataSource,
    private val antallKammer: Int = 8,
    oppgaver: List<Oppgave>
) {

    init {
        for (oppgave in oppgaver) {
            OppgaveType.leggTil(oppgave)
        }
    }

    private val log = LoggerFactory.getLogger(Motor::class.java)

    // Benytter virtuals threads istedenfor plattform tråder
    private val executor =
        Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("forbrenningskammer-", 1L).factory())

    private var stopped = false

    fun start() {
        log.info("Starter prosessering av oppgaver")
        IntRange(1, antallKammer).forEach { i ->
            executor.execute(Forbrenningskammer(dataSource)) // Legger inn en liten spread så det ikke pumpes på tabellen likt
            if (i != antallKammer) {
                Thread.sleep(100)
            }
        }
    }

    fun stop() {
        log.info("Avslutter prosessering av oppgaver")
        stopped = true
        executor.awaitTermination(10L, TimeUnit.SECONDS)
    }

    inner class Forbrenningskammer(private val dataSource: DataSource) : Runnable {
        private val log = LoggerFactory.getLogger(Forbrenningskammer::class.java)

        private var plukker = true
        override fun run() {
            while (!stopped) {
                log.debug("Starter plukking av oppgaver")
                try {
                    while (plukker) {
                        dataSource.transaction { connection ->
                            val repository = OppgaveRepository(connection)
                            val plukketOppgave = repository.plukkOppgave()
                            if (plukketOppgave != null) {
                                utførOppgave(plukketOppgave, connection)
                            }

                            if (plukker && plukketOppgave == null) {
                                plukker = false
                            }
                        }
                    }
                } catch (excetion: Throwable) {
                    log.warn("Feil under plukking av oppgaver", excetion)
                }
                log.debug("Ingen flere oppgaver å plukke, hviler litt")
                Thread.sleep(500)
                plukker = true
            }
        }

        private fun utførOppgave(oppgaveInput: OppgaveInput, connection: DBConnection) {
            try {
                dataSource.transaction { nyConnection ->
                    MDC.put("oppgaveid", "" + oppgaveInput.id)
                    MDC.put("oppgavetype", oppgaveInput.type())
                    MDC.put("sakId", oppgaveInput.sakIdOrNull().toString())
                    MDC.put("behandlingId", oppgaveInput.behandlingIdOrNull().toString())
                    MDC.put("callId", UUID.randomUUID().toString())

                    log.info("Starter på oppgave")

                    oppgaveInput.oppgave.konstruer(nyConnection).utfør(oppgaveInput)

                    log.info("Fullført oppgave")
                }
                OppgaveRepository(connection).markerKjørt(oppgaveInput)
            } catch (exception: Throwable) {
                // Kjører feil
                log.warn(
                    "Feil under prosessering av oppgave {}",
                    oppgaveInput,
                    exception
                )
                OppgaveRepository(connection).markerFeilet(oppgaveInput, exception)
            }
            MDC.clear()
        }

    }
}
