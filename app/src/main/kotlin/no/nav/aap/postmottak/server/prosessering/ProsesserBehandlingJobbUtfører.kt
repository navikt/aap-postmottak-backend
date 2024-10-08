package no.nav.aap.postmottak.server.prosessering

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.postmottak.flyt.FlytOrkestrator
import no.nav.aap.postmottak.sakogbehandling.lås.TaSkriveLåsRepository

class ProsesserBehandlingJobbUtfører(
    private val låsRepository: TaSkriveLåsRepository,
    private val kontroller: FlytOrkestrator
) : JobbUtfører {

    override fun utfør(input: JobbInput) {
        val skrivelås = låsRepository.lås(input.behandlingId())

        val kontekst = kontroller.opprettKontekst(skrivelås.behandlingSkrivelås.id)

        kontroller.forberedBehandling(kontekst)
        kontroller.prosesserBehandling(kontekst)

        låsRepository.verifiserSkrivelås(skrivelås)
    }

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return ProsesserBehandlingJobbUtfører(TaSkriveLåsRepository(connection), FlytOrkestrator(connection), )
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
