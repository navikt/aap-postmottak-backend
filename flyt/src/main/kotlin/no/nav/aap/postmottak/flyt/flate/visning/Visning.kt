package no.nav.aap.postmottak.flyt.flate.visning

import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling

data class Visning(
    val visVentekort: Boolean,
    val readOnly: Boolean,
    val typeBehandling: TypeBehandling
)
