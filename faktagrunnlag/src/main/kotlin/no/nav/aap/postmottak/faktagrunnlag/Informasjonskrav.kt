package no.nav.aap.postmottak.faktagrunnlag

import no.nav.aap.verdityper.flyt.FlytKontekst

interface Informasjonskrav {
    fun harIkkeGjortOppdateringNå(kontekst: FlytKontekst): Boolean
}
