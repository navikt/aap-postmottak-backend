package no.nav.aap.behandlingsflyt.server.prosessering

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.flyt.FlytOrkestrator
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.verdityper.flyt.StegType
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling

class ProssesserDokumentJobbUtfører(
    private val behandlingRepository: BehandlingRepository
) : JobbUtfører {

    override fun utfør(input: JobbInput) {
        val behandling = behandlingRepository.opprettBehandling(input.sakId(), TypeBehandling.DokumentHåndtering)

    }

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return ProssesserDokumentJobbUtfører(BehandlingRepositoryImpl(connection))
        }

        override fun type(): String {
            return "flyt.prosesserBehandling"
        }

        override fun navn(): String {
            return "Prosesser behandling"
        }

        override fun beskrivelse(): String {
            return "Ansvarlig for å drive prosessen på en gitt behandling"
        }
    }
}
