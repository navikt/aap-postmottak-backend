package no.nav.aap.postmottak.api.drift

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.fordeler.arena.AapSystem
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.Tema

data class VurderingerDriftsinfo(
    val fordeling: FordelingsvurderingDriftsinfo? = null,
    val tema: TemavurderingDriftsinfo? = null,
    val sak: SaksvurderingDriftsinfo? = null,
    val digitalisering: DigitaliseringsvurderingDriftsinfo? = null,
    val overlevering: OverleveringsvurderingDriftsinfo? = null,
)

data class FordelingsvurderingDriftsinfo(val system: AapSystem)

data class TemavurderingDriftsinfo(val skalTilAap: Boolean, val tema: Tema)

data class SaksvurderingDriftsinfo(val generellSak: Boolean, val opprettetNy: Boolean)

data class DigitaliseringsvurderingDriftsinfo(
    val kategori: InnsendingType,
    val digitalisertManueltGjennomPostmottak: Boolean? = null,
)

data class OverleveringsvurderingDriftsinfo(val skalOverleveresTilKelvin: Boolean)
