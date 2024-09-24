package no.nav.aap.postmottak.flyt

import no.nav.aap.postmottak.forretningsflyt.behandlingstyper.Dokumentflyt
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling

interface BehandlingType {
    fun flyt(): BehandlingFlyt
}

fun utledType(identifikator: TypeBehandling): BehandlingType {
    return when (identifikator) {
        TypeBehandling.DokumentHåndtering -> Dokumentflyt
    }
}