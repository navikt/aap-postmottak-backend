package no.nav.aap.behandlingsflyt.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import com.papsign.ktor.openapigen.annotations.type.string.example.DiscriminatorAnnotation
import no.nav.aap.behandlingsflyt.avklaringsbehov.VURDER_SYKEPENGEERSTATNING_KODE
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.AvklarSykepengerErstatningLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerVurdering
import no.nav.aap.verdityper.flyt.FlytKontekst

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = VURDER_SYKEPENGEERSTATNING_KODE)
@DiscriminatorAnnotation(fieldName = "behovstype")
class AvklarSykepengerErstatningLøsning(
    @JsonProperty(
        "sykepengeerstatningVurdering",
        required = true
    ) val sykepengeerstatningVurdering: SykepengerVurdering
) : AvklaringsbehovLøsning {
    override fun løs(connection: DBConnection, kontekst: FlytKontekst): LøsningsResultat {
        return AvklarSykepengerErstatningLøser(connection).løs(kontekst, this)
    }
}
