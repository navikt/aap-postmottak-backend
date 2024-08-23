package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.verdityper.flyt.FlytKontekst

interface Informasjonskrav {
    fun harIkkeGjortOppdateringNå(kontekst: FlytKontekst): Boolean
}
