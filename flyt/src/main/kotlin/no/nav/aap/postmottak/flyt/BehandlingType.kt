package no.nav.aap.postmottak.flyt

import no.nav.aap.postmottak.forretningsflyt.behandlingstyper.Dokumentflyt
import no.nav.aap.postmottak.forretningsflyt.behandlingstyper.Journalføringsflyt
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling

interface BehandlingType {
    fun flyt(): BehandlingFlyt
}

fun utledType(identifikator: TypeBehandling): BehandlingType {
    return when (identifikator) {
        TypeBehandling.DokumentHåndtering -> Dokumentflyt
        TypeBehandling.Journalføring -> Journalføringsflyt
    }
}