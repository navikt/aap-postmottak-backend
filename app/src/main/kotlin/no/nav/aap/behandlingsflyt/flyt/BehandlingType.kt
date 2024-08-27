package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper.Dokumentflyt
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling

interface BehandlingType {
    fun flyt(): BehandlingFlyt
}

fun utledType(identifikator: TypeBehandling): BehandlingType {
    return when (identifikator) {
        TypeBehandling.DokumentHåndtering -> Dokumentflyt
    }
}