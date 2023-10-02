package no.nav.aap.behandlingsflyt.domene.vilkår

import no.nav.aap.behandlingsflyt.domene.behandling.Avslagsårsak
import no.nav.aap.behandlingsflyt.domene.behandling.Beslutningstre
import no.nav.aap.behandlingsflyt.domene.behandling.Utfall

class VurderingsResultat(val utfall: Utfall, val avslagsårsak: Avslagsårsak?, val beslutningstre: Beslutningstre) {
}
