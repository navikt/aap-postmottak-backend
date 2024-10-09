package no.nav.aap.postmottak.sakogbehandling.behandling.vurdering

import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.Brevkode


data class KategoriVurdering(val avklaring: Brevkode)

data class Struktureringsvurdering(val vurdering: String)

class Vurderinger(
    val kategorivurdering: KategoriVurdering? = null,
    val struktureringsvurdering: Struktureringsvurdering? = null,
)
