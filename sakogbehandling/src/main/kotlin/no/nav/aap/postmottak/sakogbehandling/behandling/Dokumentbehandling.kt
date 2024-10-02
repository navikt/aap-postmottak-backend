package no.nav.aap.postmottak.sakogbehandling.behandling

import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.behandling.vurdering.Vurderinger
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class Dokumentbehandling(
    val id: BehandlingId,
    val journalpostId: JournalpostId,
    val vurderinger: Vurderinger = Vurderinger(),
    val versjon: Long
) {
    fun harBlittStrukturert() = vurderinger.struktureringsvurdering != null
    fun harTemaBlittAvklart() = vurderinger.avklarTemaVurdering != null
    fun harBlittKategorisert() = vurderinger.kategorivurdering != null
    fun harGjortSaksvurdering() = vurderinger.saksvurdering != null

}