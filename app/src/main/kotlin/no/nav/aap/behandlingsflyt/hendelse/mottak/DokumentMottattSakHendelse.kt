package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDateTime

class DokumentMottattSakHendelse(
    val journalpost: JournalpostId,
    val mottattTidspunkt: LocalDateTime
) : SakHendelse {

    override fun tilBehandlingHendelse(): BehandlingHendelse {
        return DokumentMottattBehandlingHendelse()
    }
}
