package no.nav.aap.postmottak.hendelse.oppgave

import no.nav.aap.postmottak.hendelse.avløp.AvklaringsbehovHendelseDto
import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.postmottak.kontrakt.journalpost.Status
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling
import java.time.LocalDateTime

data class BehandlingsFlytStoppetHendelseDTO(
    val referanse: JournalpostId,
    val behandlingType: TypeBehandling,
    val status: Status,
    val avklaringsbehov: List<AvklaringsbehovHendelseDto>,
    val opprettetTidspunkt: LocalDateTime
)

