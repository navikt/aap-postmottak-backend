package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate

import HelseinstitusjonVurderingDto

data class HelseinstitusjonGrunnlagResponse (
    val helseinstitusjonOpphold: List<InstitusjonsoppholdDto>,
    val helseinstitusjonGrunnlag: HelseinstitusjonVurderingDto?
)