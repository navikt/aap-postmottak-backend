package no.nav.aap.behandlingsflyt.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import com.papsign.ktor.openapigen.annotations.type.string.example.DiscriminatorAnnotation
import no.nav.aap.behandlingsflyt.avklaringsbehov.FORESLÅ_VEDTAK_KODE
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.ForeslåVedtakLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekst

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = FORESLÅ_VEDTAK_KODE)
@DiscriminatorAnnotation(fieldName = "behovstype")
class ForeslåVedtakLøsning(
    @JsonProperty("foreslåvedtakVurdering", required = true) val foreslåvedtakVurdering: String
) : AvklaringsbehovLøsning {
    override fun løs(connection: DBConnection, kontekst: FlytKontekst): LøsningsResultat {
        return ForeslåVedtakLøser(connection).løs(kontekst, this)
    }
}
