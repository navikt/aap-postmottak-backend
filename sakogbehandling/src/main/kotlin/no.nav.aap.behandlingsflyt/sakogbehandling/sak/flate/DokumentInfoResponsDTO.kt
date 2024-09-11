package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

data class DokumentInfoResponsDTO(val søker: DokumentIdent, val tittel: String)
data class DokumentIdent(val ident: String, val navn: String)