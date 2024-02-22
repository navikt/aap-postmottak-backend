package no.nav.aap.behandlingsflyt.test

import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.adapter.BARN_RELASJON_QUERY
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.adapter.PERSON_BOLK_QUERY
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.adapter.PERSON_QUERY
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.IDENT_QUERY
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.pdl.HentPersonBolkResult
import no.nav.aap.pdl.PDLDødsfall
import no.nav.aap.pdl.PdlFoedsel
import no.nav.aap.pdl.PdlGruppe
import no.nav.aap.pdl.PdlIdent
import no.nav.aap.pdl.PdlIdenter
import no.nav.aap.pdl.PdlIdenterData
import no.nav.aap.pdl.PdlIdenterDataResponse
import no.nav.aap.pdl.PdlPersoninfo
import no.nav.aap.pdl.PdlPersoninfoData
import no.nav.aap.pdl.PdlPersoninfoDataResponse
import no.nav.aap.pdl.PdlRelasjon
import no.nav.aap.pdl.PdlRelasjonDataResponse
import no.nav.aap.pdl.PdlRequest
import no.nav.aap.verdityper.sakogbehandling.Ident
import java.time.LocalDate
import no.nav.aap.pdl.PdlPerson as BarnPdlPerson
import no.nav.aap.pdl.PdlRelasjonData as BarnPdlData

class Fakes : AutoCloseable {
    private var azure: NettyApplicationEngine =
        embeddedServer(Netty, port = 0, module = Application::azureFake).apply { start() }
    private val pdl = embeddedServer(Netty, port = 0, module = Application::pdlFake).apply { start() }

    init {
        // Azure
        System.setProperty("azure.openid.config.token.endpoint", "http://localhost:${azure.port()}/token")
        System.setProperty("azure.app.client.id", "")
        System.setProperty("azure.app.client.secret", "")
        System.setProperty("azure.openid.config.jwks.uri", "")
        System.setProperty("azure.openid.config.issuer", "")

        // Pdl
        System.setProperty("integrasjon.pdl.url", "http://localhost:${pdl.port()}")
        System.setProperty("integrasjon.pdl.scope", "pdl")

        // Legg til alle testpersoner
        listOf(PERSON_MED_BARN_65ÅR).forEach { leggTil(it) }
    }

    override fun close() {
        pdl.stop(0L, 0L)
        azure.stop(0L, 0L)
    }

    fun leggTil(person: TestPerson) {
        person.barn.forEach { leggTil(it) }
        fakePersoner[person.aktivIdent()] = person
    }

}

fun NettyApplicationEngine.port(): Int =
    runBlocking { resolvedConnectors() }
        .first { it.type == ConnectorType.HTTP }
        .port

val BARNLØS_PERSON_30ÅR =
    TestPerson(setOf(Ident("12345678910", true)), fødselsdato = Fødselsdato(LocalDate.now().minusYears(30)))
val BARNLØS_PERSON_18ÅR =
    TestPerson(setOf(Ident("42346734567", true)), fødselsdato = Fødselsdato(LocalDate.now().minusYears(18).minusDays(10)))
val PERSON_MED_BARN_65ÅR =
    TestPerson(
        setOf(Ident("86322434234", true)), fødselsdato = Fødselsdato(LocalDate.now().minusYears(65)), barn = listOf(
            BARNLØS_PERSON_18ÅR, BARNLØS_PERSON_30ÅR
        )
    )

private val fakePersoner: MutableMap<String, TestPerson> = mutableMapOf()

fun Application.pdlFake() {
    install(ContentNegotiation) {
        jackson()
    }
    routing {
        post {
            val req = call.receive<PdlRequest>()

            when (req.query) {
                IDENT_QUERY -> call.respond(identer(req))
                PERSON_QUERY -> call.respond(personopplysninger(req))
                BARN_RELASJON_QUERY -> call.respond(barnRelasjoner(req))
                PERSON_BOLK_QUERY -> call.respond(barn(req))
                else -> call.respond(HttpStatusCode.BadRequest)
            }
        }
    }
}

private fun barn(req: PdlRequest): PdlRelasjonDataResponse {
    val forespurtIdenter = req.variables.identer ?: emptyList()

    forespurtIdenter.map { mapIdentBolk(it) }

    return PdlRelasjonDataResponse(
        errors = null,
        extensions = null,
        data = BarnPdlData(
            hentPersonBolk = listOf(
                HentPersonBolkResult(
                    ident = "10123456789",
                    person = BarnPdlPerson(
                        foedsel = listOf(PdlFoedsel("2020-01-01"))
                    )
                )
            )
        )
    )
}

private fun mapIdentBolk(it: String): HentPersonBolkResult? {
    val person = fakePersoner[it]
    if (person == null) {
        return null
    }
    return HentPersonBolkResult(
        ident = person.aktivIdent(),
        person = BarnPdlPerson(
            foedsel = listOf(PdlFoedsel(person.fødselsdato.toFormatedString())),
            doedsfall = mapDødsfall(person)
        )
    )
}

fun mapDødsfall(person: TestPerson): Set<PDLDødsfall>? {
    if (person.dødsdato == null) {
        return null
    }
    return setOf(PDLDødsfall(person.dødsdato.toFormatedString()))
}

private fun barnRelasjoner(req: PdlRequest): PdlRelasjonDataResponse {
    val forespurtIdent = req.variables.ident ?: ""
    return PdlRelasjonDataResponse(
        errors = null,
        extensions = null,
        data = BarnPdlData(
            hentPerson = BarnPdlPerson(
                forelderBarnRelasjon = fakePersoner[forespurtIdent]?.barn?.map { PdlRelasjon(it.aktivIdent()) }
                    ?.toList().orEmpty()
            )
        )
    )
}

private fun identer(req: PdlRequest): PdlIdenterDataResponse {
    val forespurtIdent = req.variables.ident ?: ""
    val person = fakePersoner[forespurtIdent]

    return PdlIdenterDataResponse(
        errors = null,
        extensions = null,
        data = PdlIdenterData(
            hentIdenter = PdlIdenter(
                identer = mapIdent(person)
            )
        ),
    )
}

fun mapIdent(person: TestPerson?): List<PdlIdent> {
    if (person == null) {
        return emptyList()
    }
    return listOf(PdlIdent(person.aktivIdent(), false, PdlGruppe.FOLKEREGISTERIDENT))
}

private fun personopplysninger(req: PdlRequest): PdlPersoninfoDataResponse {
    val forespurtIdent = req.variables.ident ?: ""
    val person = fakePersoner[forespurtIdent]
    return PdlPersoninfoDataResponse(
        errors = null,
        extensions = null,
        data = PdlPersoninfoData(
            hentPerson = mapPerson(person)
        ),
    )
}

fun mapPerson(person: TestPerson?): PdlPersoninfo? {
    if (person == null) {
        return null
    }
    return PdlPersoninfo(person.fødselsdato.toFormatedString())
}

fun Application.azureFake() {
    install(ContentNegotiation) {
        jackson()
    }
    routing {
        post("/token") {
            call.respond(TestToken())
        }
    }
}

internal data class TestToken(
    val access_token: String = "very.secure.token",
    val refresh_token: String = "very.secure.token",
    val id_token: String = "very.secure.token",
    val token_type: String = "token-type",
    val scope: String? = null,
    val expires_in: Int = 3599,
)
