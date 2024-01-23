package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Status
import no.nav.aap.verdityper.Periode

data class UtvidetSaksinfoDTO(
    val saksnummer: String,
    val status: Status,
    val periode: Periode,
    val behandlinger: List<BehandlinginfoDTO>,
    val ident: String
)