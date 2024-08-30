package no.nav.aap.behandlingsflyt.sakogbehandling.behandling

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.verdityper.flyt.StegStatus
import no.nav.aap.verdityper.flyt.StegType
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.SakId
import no.nav.aap.verdityper.sakogbehandling.Status
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling
import java.time.LocalDateTime


class Vurdering<T>(val vurdering: T)

class Vurderinger(
    val grovkategorivurdering: Vurdering<Boolean>? = null,
    val kategorivurdering: Vurdering<Brevkode>? = null,
    val struktureringsvurdering: Vurdering<String>? = null
)

class Behandling(
    val id: BehandlingId,
    val journalpostId: JournalpostId,
    val referanse: BehandlingReferanse = BehandlingReferanse(),
    val sakId: SakId? = null,
    private var status: Status = Status.OPPRETTET,
    private var stegHistorikk: List<StegTilstand> = mutableListOf(),
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
    val versjon: Long = 1,
    val vurderinger: Vurderinger = Vurderinger()
) : Comparable<Behandling> {

    val typeBehandling = TypeBehandling.DokumentHåndtering

    fun harBlittStrukturert() = vurderinger.struktureringsvurdering != null
    fun harBlittgrovkategorisert() = vurderinger.grovkategorivurdering != null

    fun flytKontekst(): FlytKontekst {
        return FlytKontekst(id, typeBehandling)
    }

    fun visit(stegTilstand: StegTilstand) {
        check(!stegTilstand.aktiv) {"Utvikler feil, prøver legge til steg med aktivtflagg false."}

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
        return "Behandling(id=$id, referanse=$referanse, sakId=$sakId, typeBehandling=$typeBehandling, status=$status, opprettetTidspunkt=$opprettetTidspunkt, versjon=$versjon)"
    }
}