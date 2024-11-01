package no.nav.aap.postmottak.kontrakt.hendelse

import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.kontrakt.behandling.Status
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import java.time.LocalDateTime
import java.util.UUID

data class DokumentflytStoppetHendelse(
    val journalpostId: JournalpostId,
    val referanse: UUID,
    val behandlingType: TypeBehandling,
    val status: Status,
    val avklaringsbehov: List<AvklaringsbehovHendelseDto>,
    val opprettetTidspunkt: LocalDateTime,
    val hendelsesTidspunkt: LocalDateTime,
)
