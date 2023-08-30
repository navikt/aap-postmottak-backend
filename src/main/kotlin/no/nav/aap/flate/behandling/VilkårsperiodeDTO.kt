package no.nav.aap.flate.behandling

import no.nav.aap.domene.behandling.Utfall
import no.nav.aap.domene.typer.Periode

data class VilkårsperiodeDTO(val periode: Periode, val utfall: Utfall)
