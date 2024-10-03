package no.nav.aap.postmottak.server.prosessering

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.postmottak.flyt.FlytOrkestrator
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class ProsesserBehandlingJobbUtfører(
    private val kontroller: FlytOrkestrator
) : JobbUtfører {

    override fun utfør(input: JobbInput) {

        val kontekst = kontroller.opprettKontekst(BehandlingId(input.behandlingId()))

        kontroller.forberedBehandling(kontekst)
        kontroller.prosesserBehandling(kontekst)

    }

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return ProsesserBehandlingJobbUtfører(FlytOrkestrator(connection), )
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
