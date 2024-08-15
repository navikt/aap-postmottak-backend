package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import org.slf4j.LoggerFactory
import java.io.IOException

data class BistandVurdering(
    val begrunnelse: String,
    val erBehovForBistand: Boolean,
    val erBehovForAktivBehandling: Boolean? = null,
    val erBehovForArbeidsrettetTiltak: Boolean? = null,
    val erBehovForAnnenOppfølging: Boolean? = null
)

object BistandGrunner {
    const val ARBEIDSRETTET_TILTAK = "Behov for arbeidsrettet tiltak"
    const val AKTIV_BEHANDLING = "Behov for aktiv behandling"
    const val ANNEN_OPPFØLGING = "Etter å ha prøvd tiltakene etter bokstav a eller b fortsatt anses for å ha en viss mulighet for å komme i arbeid, og får annen oppfølging fra Arbeids- og velferdsetaten"
}
class BistandGrunnerSerializer : JsonSerializer<BistandVurdering?>() {
    @Throws(IOException::class)
    override fun serialize(value: BistandVurdering?, gen: JsonGenerator, serializers: SerializerProvider?) {
        if(value == null) return
        val grunner = ArrayList<String>()
        if(value.erBehovForArbeidsrettetTiltak == true) {
            grunner.add(BistandGrunner.ARBEIDSRETTET_TILTAK)
        } else if(value.erBehovForAktivBehandling == true) {
            grunner.add(BistandGrunner.AKTIV_BEHANDLING)
        } else if(value.erBehovForAnnenOppfølging == true) {
            grunner.add(BistandGrunner.ANNEN_OPPFØLGING)
        }
        with(gen) {
            writeStartObject()
            writeStringField("begrunnelse", value.begrunnelse)
            writeBooleanField("erBehovForBistand", value.erBehovForBistand)
            writeFieldName("grunner")
            writeStartArray()
            for(grunn: String in grunner){
                writeString(grunn)
            }
            writeEndArray()
            writeEndObject()
        }
    }
}
open class BistandGrunnerDeserializer : JsonDeserializer<BistandVurdering?>() {
    private val log = LoggerFactory.getLogger(VilkårsresultatRepository::class.java)
    @Throws(IOException::class)
    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): BistandVurdering? {
        val node = p?.readValueAsTree<JsonNode>()
        var erBehovForAktivBehandling: Boolean? = null
        var erBehovForArbeidsrettetTiltak: Boolean? = null
        var erBehovForAnnenOppfølging: Boolean? = null
        val grunner = node!!.get("grunner").asIterable()
        for (grunn: JsonNode in grunner) {
            val grunnText = grunn.asText()
            if(grunnText == BistandGrunner.ARBEIDSRETTET_TILTAK) {
                erBehovForArbeidsrettetTiltak = true
            } else if(grunnText == BistandGrunner.AKTIV_BEHANDLING) {
                erBehovForAktivBehandling = true
            } else if(grunnText == BistandGrunner.ANNEN_OPPFØLGING) {
                erBehovForAnnenOppfølging = true
            }
        }
        return BistandVurdering(
            node.get("begrunnelse").asText(),
            node.get("erBehovForBistand").asBoolean(),
            erBehovForAktivBehandling,
            erBehovForArbeidsrettetTiltak,
            erBehovForAnnenOppfølging);
    }
}
