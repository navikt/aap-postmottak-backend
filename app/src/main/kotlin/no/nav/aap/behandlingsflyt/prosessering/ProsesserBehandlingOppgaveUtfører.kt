package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.flyt.FlytOrkestrator
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.motor.Oppgave
import no.nav.aap.motor.OppgaveInput
import no.nav.aap.motor.OppgaveUtfører
import no.nav.aap.verdityper.flyt.FlytKontekst

class ProsesserBehandlingOppgaveUtfører(
    private val låsRepository: TaSkriveLåsRepository,
    private val kontroller: FlytOrkestrator
) : OppgaveUtfører {

    override fun utfør(input: OppgaveInput) {
        val skrivelås = låsRepository.lås(input.sakId(), input.behandlingId())

        val kontekst = FlytKontekst(
            sakId = input.sakId(),
            behandlingId = input.behandlingId(),
            behandlingType = skrivelås.behandlingSkrivelås.typeBehandling
        )
        kontroller.forberedBehandling(kontekst)
        kontroller.prosesserBehandling(kontekst)

        låsRepository.verifiserSkrivelås(skrivelås)
    }

    companion object : Oppgave {
        override fun konstruer(connection: DBConnection): OppgaveUtfører {
            return ProsesserBehandlingOppgaveUtfører(TaSkriveLåsRepository(connection), FlytOrkestrator(connection))
        }

        override fun type(): String {
            return "flyt.prosesserBehandling"
        }
    }
}
