package no.nav.aap.postmottak.hendelse.avløp

import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.postmottak.kontrakt.journalpost.Status
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling
import java.time.LocalDateTime

data class DokumentflytStoppetHendelse(
    val referanse: JournalpostId,
    val behandlingType: TypeBehandling,
    val status: Status,
    val avklaringsbehov: List<AvklaringsbehovHendelseDto>,
    val opprettetTidspunkt: LocalDateTime
)
