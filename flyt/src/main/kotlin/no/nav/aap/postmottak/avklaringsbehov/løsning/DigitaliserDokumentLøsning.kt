package no.nav.aap.postmottak.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.løser.DigitaliserDokumentLøser
import no.nav.aap.postmottak.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.DIGITALISER_DOKUMENT_KODE
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = DIGITALISER_DOKUMENT_KODE)
class DigitaliserDokumentLøsning(
    @param:JsonProperty(required = true)
    val kategori: InnsendingType,
    @param:JsonProperty(required = true)
    val strukturertDokument: String?,
    @param:JsonProperty(required = true)
    val søknadsdato: LocalDate?,
    @param:JsonProperty(
        "behovstype",
        required = true,
        defaultValue = DIGITALISER_DOKUMENT_KODE
    ) val behovstype: String = DIGITALISER_DOKUMENT_KODE
) : AvklaringsbehovLøsning {
    override fun løs(
        repositoryProvider: RepositoryProvider,
        gatewayProvider: GatewayProvider,
        kontekst: AvklaringsbehovKontekst
    ): LøsningsResultat {
        return DigitaliserDokumentLøser.konstruer(repositoryProvider, gatewayProvider).løs(kontekst, this)
    }
}


