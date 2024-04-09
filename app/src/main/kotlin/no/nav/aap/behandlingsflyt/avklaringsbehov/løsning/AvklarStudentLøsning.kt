package no.nav.aap.behandlingsflyt.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import com.papsign.ktor.openapigen.annotations.type.string.example.DiscriminatorAnnotation
import no.nav.aap.behandlingsflyt.avklaringsbehov.AVKLAR_STUDENT_KODE
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.AvklarStudentLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.verdityper.flyt.FlytKontekst

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_STUDENT_KODE)
@DiscriminatorAnnotation(fieldName = "behovstype")
class AvklarStudentLøsning(
    @JsonProperty("studentvurdering", required = true) val studentvurdering: StudentVurdering
) :
    AvklaringsbehovLøsning {
    override fun løs(connection: DBConnection, kontekst: FlytKontekst): LøsningsResultat {
        return AvklarStudentLøser(connection).løs(kontekst, this)
    }

}
