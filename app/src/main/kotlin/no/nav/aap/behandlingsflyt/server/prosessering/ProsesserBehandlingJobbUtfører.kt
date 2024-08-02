package no.nav.aap.behandlingsflyt.server.prosessering

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.flyt.FlytOrkestrator
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.verdityper.flyt.FlytKontekst

class ProsesserBehandlingJobbUtfører(
    private val behandlingRepository: BehandlingRepository,
    private val sakRepository: SakRepository,
    private val kontroller: FlytOrkestrator
) : JobbUtfører {

    override fun utfør(input: JobbInput) {
        behandlingRepository.hentMedLås(input.behandlingId())
        sakRepository.låsSak(input.sakId())
        val behandling = behandlingRepository.hent(input.behandlingId())

        val kontekst = FlytKontekst(
            sakId = input.sakId(),
            behandlingId = input.behandlingId(),
            behandlingType = behandling.typeBehandling()
        )
        kontroller.forberedBehandling(kontekst)
        kontroller.prosesserBehandling(kontekst)

        behandlingRepository.bumpVersjon(input.behandlingId())
        sakRepository.bumpVersjon(input.sakId())
    }

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return ProsesserBehandlingJobbUtfører(
                BehandlingRepositoryImpl(connection),
                SakRepositoryImpl(connection),
                FlytOrkestrator(connection))
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
