package no.nav.aap.postmottak.api.faktagrunnlag.strukturering

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import java.time.LocalDate

data class DigitaliseringvurderingDto(
    val kategori: InnsendingType,
    val strukturertDokumentJson: String?,
    val søknadsdato: LocalDate?
)

data class DigitaliseringGrunnlagDto(
    val erPapir: Boolean,
    val vurdering: DigitaliseringvurderingDto?
)
