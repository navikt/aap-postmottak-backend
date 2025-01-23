package no.nav.aap.postmottak.journalpostogbehandling.behandling

import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.journalpostogbehandling.flyt.StegStatus
import no.nav.aap.postmottak.kontrakt.behandling.Status
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.kontrakt.steg.StegType
import java.time.LocalDateTime


class Behandling(
    val id: BehandlingId,
    val journalpostId: JournalpostId,
    private var status: Status = Status.OPPRETTET,
    private var stegHistorikk: List<StegTilstand> = mutableListOf(),
    val opprettetTidspunkt: LocalDateTime,
    val versjon: Long = 0,
    val referanse: BehandlingsreferansePathParam,
    val typeBehandling: TypeBehandling
) : Comparable<Behandling> {

    fun flytKontekst(): FlytKontekst {
        return FlytKontekst(journalpostId, id, typeBehandling)
    }

    fun visit(stegTilstand: StegTilstand) {
        check(stegTilstand.aktiv) {"Utviklerfeil, prøver legge til steg med aktivtflagg false."}

        if (stegHistorikk.isEmpty() || aktivtStegTilstand() != stegTilstand) {
            stegHistorikk.stream().filter { tilstand -> tilstand.aktiv }.forEach { tilstand -> tilstand.deaktiver() }
            stegHistorikk += stegTilstand
            stegHistorikk = stegHistorikk.sorted()
        }
        validerStegTilstand()

        oppdaterStatus(stegTilstand)
    }

    private fun validerStegTilstand() {
        if (stegHistorikk.isNotEmpty() && stegHistorikk.stream().noneMatch { tilstand -> tilstand.aktiv }) {
            throw IllegalStateException("Utvikler feil, mangler aktivt steg når steghistorikk ikke er tom.")
        }
    }

    private fun oppdaterStatus(stegTilstand: StegTilstand) {
        val stegStatus = stegTilstand.steg().status
        if (status != stegStatus) {
            status = stegStatus
        }
    }

    fun status(): Status = status

    fun stegHistorikk(): List<StegTilstand> = stegHistorikk.toList()

    fun harIkkeVærtAktivitetIDetSiste(): Boolean {
        return aktivtStegTilstand().tidspunkt().isBefore(LocalDateTime.now().minusMinutes(15))
    }

    fun aktivtSteg(): StegType {
        return aktivtStegTilstand().steg()
    }

    private fun aktivtStegTilstand(): StegTilstand {
        return stegHistorikk.stream()
            .filter { tilstand -> tilstand.aktiv }
            .findAny()
            .orElse(
                StegTilstand(
                    stegType = StegType.START_BEHANDLING,
                    stegStatus = StegStatus.START
                )
            )
    }

    override fun compareTo(other: Behandling): Int {
        return this.opprettetTidspunkt.compareTo(other.opprettetTidspunkt)
    }

    override fun toString(): String {
        return "Behandling(id=$id, referanse=$referanse, typeBehandling=$typeBehandling, status=$status, opprettetTidspunkt=$opprettetTidspunkt, versjon=$versjon)"
    }
}