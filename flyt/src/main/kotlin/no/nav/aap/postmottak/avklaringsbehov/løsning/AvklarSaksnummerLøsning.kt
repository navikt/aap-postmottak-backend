package no.nav.aap.postmottak.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.løser.AvklarSakLøser
import no.nav.aap.postmottak.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.postmottak.gateway.AvsenderMottakerDto
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.AVKLAR_SAKSNUMMER_KODE

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_SAKSNUMMER_KODE)
class AvklarSaksnummerLøsning(
    val saksnummer: String?,
    val opprettNySak: Boolean = false,
    val førPåGenerellSak: Boolean = false,
    val journalposttittel: String? = null,
    val avsenderMottaker: AvsenderMottakerDto? = null,
    val dokumenter: List<ForenkletDokument>? = null,
    @JsonProperty(
        "behovstype",
        required = true,
        defaultValue = AVKLAR_SAKSNUMMER_KODE
    ) val behovstype: String = AVKLAR_SAKSNUMMER_KODE
) : AvklaringsbehovLøsning {
    override fun løs(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider, kontekst: AvklaringsbehovKontekst): LøsningsResultat {
        return AvklarSakLøser.konstruer(repositoryProvider, gatewayProvider).løs(kontekst, this)
    }
}

data class ForenkletDokument(
    val dokumentInfoId: String,
    val tittel: String,
)
