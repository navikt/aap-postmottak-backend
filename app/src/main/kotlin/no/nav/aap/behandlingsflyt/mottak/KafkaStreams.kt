package no.nav.aap.behandlingsflyt.mottak

import libs.kafka.StreamsConfig
import libs.kafka.Topology
import libs.kafka.topology
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.server.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.slf4j.LoggerFactory


private val log = LoggerFactory.getLogger(MottakListener::class.java)


class MottakListener(
    config: StreamsConfig,
    private val behandlingRepository: BehandlingRepository,
    private val flytJobbRepository: FlytJobbRepository
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
        val journalpostId = JournalpostId(journalpost.journalpostId)
        val behandling =
            behandlingRepository.opprettBehandling(journalpostId)
        flytJobbRepository.leggTil(
            JobbInput(ProsesserBehandlingJobbUtfører)
                .forBehandling(behandling.id).medCallId()
        )
    }

}
