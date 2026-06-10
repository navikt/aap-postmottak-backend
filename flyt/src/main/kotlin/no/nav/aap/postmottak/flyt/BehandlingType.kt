package no.nav.aap.postmottak.flyt

import no.nav.aap.postmottak.forretningsflyt.behandlingstyper.Dokumentflyt
import no.nav.aap.postmottak.forretningsflyt.behandlingstyper.Journalføringsflyt
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling

interface BehandlingType {
    fun flyt(): BehandlingFlyt
}

fun utledType(identifikator: TypeBehandling): BehandlingType {
    return when (identifikator) {
        TypeBehandling.Fordeling -> throw RuntimeException("Fordelingsflyt er ikke implementert enda, ingen behandlinger være av typen Fordeling!")
        TypeBehandling.DokumentHåndtering -> Dokumentflyt
        TypeBehandling.Journalføring -> Journalføringsflyt
    }
}