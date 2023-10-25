package no.nav.aap.behandlingsflyt.behandling

import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt

interface BehandlingType {
    fun flyt(): BehandlingFlyt
    fun identifikator(): String
}

