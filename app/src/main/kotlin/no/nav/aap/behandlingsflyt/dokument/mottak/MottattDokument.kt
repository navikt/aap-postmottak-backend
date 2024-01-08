package no.nav.aap.behandlingsflyt.dokument.mottak

import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.behandling.dokumenter.JournalpostId
import no.nav.aap.behandlingsflyt.sak.SakId
import java.time.LocalDateTime

data class MottattDokument(
    val journalpostId: JournalpostId,
    val sakId: SakId,
    val behandlingId: BehandlingId?,
    val mottattTidspunkt: LocalDateTime,
    val type: DokumentType,
    val status: Status = Status.MOTTATT
) {
}