package no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.httpclient.ClientConfig
import no.nav.aap.httpclient.Header
import no.nav.aap.httpclient.RestClient
import no.nav.aap.httpclient.request.GetRequest
import no.nav.aap.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.json.DefaultJsonMapper
import no.nav.aap.medlemskap.MedlemskapRequest
import no.nav.aap.medlemskap.MedlemskapResponse
import no.nav.aap.requiredConfigForKey
import java.net.URI
import java.time.LocalDate

data class Medlemskap(val unntak: List<Unntak>)

data class Unntak(
    val unntakId: Number,
    val ident: String,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val status: String,
    val statusaarsak: String?,
    val medlem: Boolean,
)

object MedlemskapGateway : MedlemskapGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.medl.url"))
    val config = ClientConfig(scope = requiredConfigForKey("integrasjon.medl.scope"))

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider
    )

    private fun query(request: MedlemskapRequest): List<MedlemskapResponse> {
        val httpRequest = GetRequest(
            additionalHeaders = listOf(
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Nav-Personident", request.fodselsnumre.first()),
                Header("Accept", "application/json"),
            )
        )

        return requireNotNull(
            client.get(
                uri = url,
                request = httpRequest,
                mapper = { body, _ ->
                    DefaultJsonMapper.fromJson(body)
                }
            )
        )
    }

    override fun innhent(person: Person): Medlemskap {
        val request = MedlemskapRequest(person.identer().map { it.identifikator })
        val medlemskapResultat = query(request)

        return Medlemskap(unntak = medlemskapResultat.map {
            Unntak(
                unntakId = it.unntakId,
                ident = it.ident,
                fraOgMed = LocalDate.parse(it.fraOgMed),
                tilOgMed = LocalDate.parse(it.tilOgMed),
                status = it.status,
                statusaarsak = it.statusaarsak,
                medlem = it.medlem
            )
        })
    }

}