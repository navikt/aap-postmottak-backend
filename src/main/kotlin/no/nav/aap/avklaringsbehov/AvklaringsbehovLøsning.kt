package no.nav.aap.avklaringsbehov

import no.nav.aap.domene.behandling.avklaringsbehov.Definisjon

abstract class AvklaringsbehovLøsning(val definisjon: Definisjon, val begrunnelse: String, val endretAv: String)
