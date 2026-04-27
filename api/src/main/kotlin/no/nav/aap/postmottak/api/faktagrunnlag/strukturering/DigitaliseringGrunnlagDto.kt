package no.nav.aap.postmottak.api.faktagrunnlag.strukturering

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.postmottak.gateway.Klagebehandling
import java.time.LocalDate

data class DigitaliseringvurderingDto(
    val kategori: InnsendingType,
    val strukturertDokumentJson: String?,
    val søknadsdato: LocalDate?
)

data class BrukerDto(
    val fnr: String,
    val navn: String?
)

data class DigitaliseringGrunnlagDto(
    val klagebehandlinger: List<Klagebehandling>,
    val erPapir: Boolean,
    val vurdering: DigitaliseringvurderingDto?,
    val bruker: BrukerDto
)
