package no.nav.aap.behandlingsflyt.sakogbehandling.behandling.vurdering

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode

data class TemaVurdeirng(val avklaring: Boolean)

data class KategoriVurdering(val avklaring: Brevkode)

data class Saksvurdering(
    val saksnummer: String?,
    val opprettNySak: Boolean,
)

data class Struktureringsvurdering(val vurdering: String)