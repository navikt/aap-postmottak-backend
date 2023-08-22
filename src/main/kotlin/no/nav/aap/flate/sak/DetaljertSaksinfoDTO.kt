package no.nav.aap.flate.sak

import no.nav.aap.domene.typer.Periode

data class DetaljertSaksinfoDTO(val saksnummer: String, val periode: Periode, val behandlinger: List<BehandlinginfoDTO>)