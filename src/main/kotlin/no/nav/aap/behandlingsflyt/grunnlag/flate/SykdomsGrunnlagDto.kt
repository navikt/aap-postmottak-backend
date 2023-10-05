package no.nav.aap.behandlingsflyt.grunnlag.flate

import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.domene.Periode

data class SykdomsGrunnlagDto(
    val opplysninger: InnhentetSykdomsOpplysninger,
    val yrkesskadevurdering: Yrkesskadevurdering?,
    val sykdomsvurdering: Sykdomsvurdering?
)

data class InnhentetSykdomsOpplysninger(
    val oppgittYrkesskadeISøknad: Boolean,
    val innhentedeYrkesskader: List<RegistrertYrkesskade>
)


data class SykdomSykdomsGrunnlagDto(
    val opplysninger: SykdomInnhentetSykdomsOpplysninger,
    val sykdomsvurdering: Sykdomsvurdering?
)

data class SykdomInnhentetSykdomsOpplysninger(
    val oppgittYrkesskadeISøknad: Boolean,
    val innhentedeYrkesskader: List<RegistrertYrkesskade>,
    val erÅrsakssammenheng: Boolean?
)

data class SykdomYrkesskadeGrunnlagDto(
    val opplysninger: SykdomInnhentetYrkesskadeOpplysninger,
    val yrkesskadevurdering: Yrkesskadevurdering?,
)

data class SykdomInnhentetYrkesskadeOpplysninger(
    val oppgittYrkesskadeISøknad: Boolean,
    val innhentedeYrkesskader: List<RegistrertYrkesskade>,
)

data class RegistrertYrkesskade(val ref: String, val periode: Periode, val kilde: String)
