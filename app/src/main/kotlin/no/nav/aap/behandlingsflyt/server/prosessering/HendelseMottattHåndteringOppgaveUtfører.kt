package no.nav.aap.behandlingsflyt.server.prosessering

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.hendelse.mottak.HåndterMottattDokumentService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.json.DefaultJsonMapper
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.verdityper.Periode

const val BREVKODE = "brevkode"
const val JOURNALPOST_ID = "journalpostId"
const val MOTTATT_TIDSPUNKT = "mottattTidspunkt"
const val PERIODE = "periode"

class HendelseMottattHåndteringOppgaveUtfører(connection: DBConnection) : JobbUtfører {
    private val låsRepository = TaSkriveLåsRepository(connection)
    private val hånderMottattDokumentService = HåndterMottattDokumentService(connection)

    override fun utfør(input: JobbInput) {
        val sakId = input.sakId()
        val sakSkrivelås = låsRepository.låsSak(sakId)

        val brevkode = Brevkode.valueOf(input.parameter(BREVKODE))

        hånderMottattDokumentService.håndterMottatteDokumenter(sakId, brevkode, utledPeriode(input.parameter(PERIODE)))

        låsRepository.verifiserSkrivelås(sakSkrivelås)
    }

    private fun utledPeriode(parameter: String): Periode? {
        if (parameter.isEmpty()) {
            return null
        }

        return DefaultJsonMapper.fromJson(parameter)
    }

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return HendelseMottattHåndteringOppgaveUtfører(
                connection
            )
        }

        override fun type(): String {
            return "hendelse.håndterer"
        }

        override fun navn(): String {
            return "Hendelses håndterer"
        }

        override fun beskrivelse(): String {
            return "Håndterer hendelser på en gitt sak. Knytter de nye opplysningene til rett behandling og oppretter behandling hvis det er behov for det."
        }
    }
}