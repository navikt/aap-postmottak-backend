package no.nav.aap.postmottak.mottak

import libs.kafka.StreamsConfig
import libs.kafka.Topology
import libs.kafka.topology
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.postmottak.server.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.slf4j.LoggerFactory
import javax.sql.DataSource


private val log = LoggerFactory.getLogger(MottakListener::class.java)

private typealias TransactionalFuntion = (BehandlingRepository, FlytJobbRepository) -> Unit
private typealias TransactionWithResources = (DataSource, TransactionalFuntion) -> Unit

class MottakListener(
    config: StreamsConfig,
    private val dataSource: DataSource,
    private val getTransactionWithResources: TransactionWithResources = {dataSource, fn -> dataSource.transaction { fn(BehandlingRepositoryImpl(it), FlytJobbRepository(it)) }} // TODO :poop: vanskelig å teste, vurder å flytte Håndter journalpost til egen klasse
) {
    private val topics = Topics(config)

    operator fun invoke(): Topology = topology {
        consume(topics.journalfoering)
            .filter { record -> record.temaNytt == "AAP" }
            .filter { record -> record.journalpostStatus == "MOTTATT" }
            .filter { record -> record.mottaksKanal !in listOf("EESSI") } // TODO: Det bør også snakkes om MELDEKORT og andre som ikke skal håndteres fordi de alt hånteres av andre
            .forEach { _, record -> håndterJournalpost(record) }
    }

    private fun håndterJournalpost(
        journalpost: JournalfoeringHendelseRecord,
    ) {
        log.info("Mottatt ${journalpost.journalpostId}")
        getTransactionWithResources(dataSource) { behandlingRepository, flytJobbRepository ->
            val journalpostId = JournalpostId(journalpost.journalpostId)
            val behandling = behandlingRepository.opprettBehandling(journalpostId)
            flytJobbRepository.leggTil(
                JobbInput(ProsesserBehandlingJobbUtfører)
                    .forBehandling(null, behandling.id.toLong()).medCallId()
            )
        }
    }

}
