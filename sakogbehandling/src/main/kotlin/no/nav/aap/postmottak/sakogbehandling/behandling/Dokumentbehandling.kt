package no.nav.aap.postmottak.sakogbehandling.behandling

import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.postmottak.sakogbehandling.behandling.vurdering.Vurderinger
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class Dokumentbehandling(
    val id: BehandlingId,
    val journalpost: Journalpost,
    val vurderinger: Vurderinger = Vurderinger(),
    val versjon: Long
) {
    fun harBlittStrukturert() = vurderinger.struktureringsvurdering != null
    fun harTemaBlittAvklart() = vurderinger.avklarTemaVurdering != null
    fun harBlittKategorisert() = vurderinger.kategorivurdering != null
    fun harGjortSaksvurdering() = vurderinger.saksvurdering != null
    fun kanBehandlesAutomatisk() = journalpost.kanBehandlesAutomatisk()


    fun erSøknad() = journalpost.erSøknad() || vurderinger.kategorivurdering?.avklaring == Brevkode.SØKNAD


}