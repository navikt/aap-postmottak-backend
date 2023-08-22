package no.nav.aap.flate.behandling

import no.nav.aap.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.domene.behandling.avklaringsbehov.Status

data class AvklaringsbehovDTO(val definisjon: Definisjon, val status: Status)
