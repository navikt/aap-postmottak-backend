package no.nav.aap.behandlingsflyt.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import com.papsign.ktor.openapigen.annotations.type.string.example.DiscriminatorAnnotation
import no.nav.aap.behandlingsflyt.avklaringsbehov.FATTE_VEDTAK_KODE
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.FatteVedtakLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekst

@JsonTypeName(value = FATTE_VEDTAK_KODE)
@DiscriminatorAnnotation(fieldName = "behovstype")
class FatteVedtakLøsning(
    @JsonProperty("totrinnsVurderinger", required = true) val vurderinger: List<TotrinnsVurdering>
) : AvklaringsbehovLøsning {
    override fun løs(connection: DBConnection, kontekst: FlytKontekst): LøsningsResultat {
        return FatteVedtakLøser(connection).løs(kontekst, this)
    }
}
