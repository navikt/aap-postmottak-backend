package no.nav.aap.postmottak.api.faktagrunnlag.sak

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksvurdering
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.AvsenderMottaker
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Dokument

data class SaksInfoDto(
    val saksnummer: String,
    val periode: Periode
)

data class AvklarSakVurderingDto(
    val saksnummer: String?,
    val førPåGenerellSak: Boolean,
) {
    companion object {
        fun toDto(saksvurdering: Saksvurdering) = AvklarSakVurderingDto(
            saksvurdering.saksnummer, saksvurdering.generellSak
        )
    }
}

data class AvklarSakGrunnlagDto(
    val vurdering: AvklarSakVurderingDto?,
    val saksinfo: List<SaksInfoDto>,
    val brevkode: String,
    val journalposttittel: String? = null,
    val dokumenter: List<Dokument>,
    val kanEndreAvsenderMottaker: Boolean,
    val avsenderMottaker: AvsenderMottaker?,
)
