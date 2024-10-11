package no.nav.aap.postmottak.sakogbehandling.behandling.vurdering


data class Struktureringsvurdering(val vurdering: String)

class Vurderinger(
    val struktureringsvurdering: Struktureringsvurdering? = null,
)
