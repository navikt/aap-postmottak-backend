package no.nav.aap.postmottak.kontrakt.hendelse

import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.kontrakt.behandling.Status
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import java.time.LocalDateTime

data class DokumentflytStoppetHendelse(
    val referanse: JournalpostId,
    val behandlingType: TypeBehandling,
    val status: Status,
    val avklaringsbehov: List<AvklaringsbehovHendelseDto>,
    val opprettetTidspunkt: LocalDateTime,
    val hendelsesTidspunkt: LocalDateTime,
)
