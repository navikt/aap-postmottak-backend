package no.nav.aap.behandlingsflyt.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekst

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "behovstype", visible = true)
sealed interface AvklaringsbehovLøsning {
    fun definisjon(): Definisjon {
        if (this.javaClass.isAnnotationPresent(JsonTypeName::class.java)) {
            return Definisjon.entries.first { it.kode == this.javaClass.getDeclaredAnnotation(JsonTypeName::class.java).value }
        }
        throw IllegalStateException("Utvikler-feil:" + this.javaClass.getSimpleName() + " er uten JsonTypeName annotation.")
    }

    fun løs(connection: DBConnection, kontekst: FlytKontekst): LøsningsResultat
}

fun utledSubtypes(): List<Class<*>> {
    return AvklaringsbehovLøsning::class.sealedSubclasses.map { it.java }.toList()
}