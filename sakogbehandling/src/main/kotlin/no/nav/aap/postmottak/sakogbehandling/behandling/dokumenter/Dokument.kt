package no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter

import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import java.time.LocalDateTime

class Dokument(private val id: Long,
               private val sakId: Long,
               private val behandlingId: Long,
               private val innsendingstidspunkt: LocalDateTime,
               private val brevkode: Brevkode,
               private val journalpostId: JournalpostId
)
