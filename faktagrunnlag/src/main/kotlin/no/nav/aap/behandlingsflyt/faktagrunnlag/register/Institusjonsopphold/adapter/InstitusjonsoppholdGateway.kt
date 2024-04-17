package no.nav.aap.behandlingsflyt.faktagrunnlag.register.Institusjonsopphold.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.Institusjonsopphold.Institusjonsopphold
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.Institusjonsopphold.InstitusjonsoppholdGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.httpclient.ClientConfig
import no.nav.aap.httpclient.RestClient
import no.nav.aap.httpclient.request.GetRequest
import no.nav.aap.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.institusjon.InstitusjonoppholdRequest
import no.nav.aap.institusjon.InstitusjonoppholdRespons
import no.nav.aap.requiredConfigForKey
import java.net.URI

object InstitusjonsoppholdGateway : InstitusjonsoppholdGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.institusjonsopphold.url"))
    val config = ClientConfig(scope = requiredConfigForKey("integrassjon.institusjonsopphold.scope"))
    private val client = RestClient(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )

    private fun query(request: InstitusjonoppholdRequest): InstitusjonoppholdRespons {
        val httpRequest = GetRequest(
            responseClazz = InstitusjonoppholdRespons::class.java,
            additionalHeaders = listOf(
                Pair("Nav-Personident",request.foedselsnumre.first().toString()),
                Pair("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Pair("Accept", "application/json")
            )
        )
        return requireNotNull(client.get(uri = url, request = httpRequest))
    }

    override fun innhent(person: Person): List<Institusjonsopphold> {
        val request = InstitusjonoppholdRequest(person.identer().map { it.identifikator })
        val oppholdRes = query(request)

        val institusjonsopphold = oppholdRes.institusjonsopphold.map { opphold ->
            Institusjonsopphold.nyttOpphold(
                requireNotNull(opphold.institusjonstype),
                requireNotNull(opphold.kategori),
                requireNotNull(opphold.startdato),
                opphold.faktiskSluttdato ?: opphold.forventetSluttdato
            )
        }
        return institusjonsopphold
    }
}