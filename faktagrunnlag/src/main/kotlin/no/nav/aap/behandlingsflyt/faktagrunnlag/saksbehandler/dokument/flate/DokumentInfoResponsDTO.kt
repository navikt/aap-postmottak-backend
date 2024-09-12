package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.flate

import no.nav.aap.behandlingsflyt.saf.Dokument

data class DokumentInfoResponsDTO(val søker: DokumentIdent, val tittel: String, val dokumenter: List<Dokument>)
data class DokumentIdent(val ident: String, val navn: String)