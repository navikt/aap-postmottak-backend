package no.nav.aap.behandlingsflyt.hendelse.oppgave

import no.nav.aap.behandlingsflyt.hendelse.avløp.AvklaringsbehovHendelseDto
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.JournalpostId
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

