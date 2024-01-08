package no.nav.aap.behandlingsflyt.faktagrunnlag.barn

import no.nav.aap.behandlingsflyt.sak.Ident
import no.nav.aap.verdityper.Periode

data class Barn(val ident: Ident, val perioder: List<Periode>)
