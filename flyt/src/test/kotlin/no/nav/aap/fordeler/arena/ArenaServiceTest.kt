package no.nav.aap.fordeler.arena

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaVedtak
import no.nav.aap.arenaoppslag.kontrakt.apiv1.HistorikkStatusDetaljResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SignifikantHistorikkResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SisteSakDetaljer
import no.nav.aap.arenaoppslag.kontrakt.apiv1.VedtakMedMaksdato
import no.nav.aap.fordeler.arena.ArenaService.OppbruktRettighetResultat.HarIkkeOppbruktRettighet.IkkeRelevantSøknad
import no.nav.aap.fordeler.arena.ArenaService.OppbruktRettighetResultat.HarIkkeOppbruktRettighet.VedtattForlenget
import no.nav.aap.fordeler.arena.ArenaService.OppbruktRettighetResultat.HarOppbruktRettighet.HarBruktOppForlengelsen
import no.nav.aap.fordeler.arena.ArenaService.OppbruktRettighetResultat.HarOppbruktRettighet.VedtattAtIkkeForlenges
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.postmottak.gateway.ArenaoppslagGateway
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.klient.createGatewayProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate

class ArenaServiceTest {

    private val gatewayProvider = createGatewayProvider {
        register<ArenaoppslagGatewayMock>()
    }

    private val arenaService = ArenaService(gatewayProvider)
    private val maksdato = LocalDate.of(2026, 12, 12)

    @Test
    fun `søknaden er ikke relevant når det ikke finnes en siste sak`() = runBlocking {
        assertEquals(IkkeRelevantSøknad, vurder(null))
    }

    @Test
    fun `avslått forlengelse gir oppbrukt rettighet`() = runBlocking {
        val sak = sisteSak().copy(unntaksvilkaarInnvilget = false)

        assertEquals(VedtattAtIkkeForlenges, vurder(sak))
    }

    @Test
    fun `innvilget forlengelse uten maksdato for unntak gir ikke oppbrukt rettighet`() = runBlocking {
        val sak = sisteSak().copy(unntaksvilkaarInnvilget = true)

        assertEquals(VedtattForlenget, vurder(sak))
    }

    @Test
    fun `søknaden er ikke relevant uten vurdering av unntak eller maksdato for unntak`() = runBlocking {
        assertEquals(IkkeRelevantSøknad, vurder(sisteSak()))
    }

    @ParameterizedTest
    @CsvSource(
        "null, true", "-6, true", "-5, true", "0, true", "5, true", "6, true",
        "null, false", "-6, false", "-5, false", "0, false", "5, false", "6, false",
        "null, null", "-6, null", "-5, null", "0, null", "5, null", "6, null",
        nullValues = ["null"]
    )
    fun `forlengelsen er bare oppbrukt når den er innvilget og sluttdato er innen fem dager fra maksdato for unntak`(
        avvikIDager: Long?,
        unntaksvilkaarInnvilget: Boolean?
    ) = runBlocking {
        val sak = sisteSak().let {
            it.copy(
                unntaksvilkaarInnvilget = unntaksvilkaarInnvilget,
                sisteVedtak = it.sisteVedtak.copy(maxdatoUnntak = avvikIDager?.let(maksdato::plusDays))
            )
        }

        assertEquals(
            when (unntaksvilkaarInnvilget) {
                true -> if (avvikIDager != null && avvikIDager in -5L..5L) {
                    HarBruktOppForlengelsen
                } else {
                    VedtattForlenget
                }
                false -> VedtattAtIkkeForlenges
                null -> IkkeRelevantSøknad
            },
            vurder(sak)
        )
    }

    @ParameterizedTest
    @CsvSource(
        "-6, true", "-5, true", "0, true", "5, true", "6, true",
        "-6, false", "-5, false", "0, false", "5, false", "6, false",
        "-6, null", "-5, null", "0, null", "5, null", "6, null",
        nullValues = ["null"]
    )
    fun `sluttdato må være innen fem dager fra ordinær maksdato uansett vurdering av unntak`(
        avvikIDager: Long,
        unntaksvilkaarInnvilget: Boolean?
    ) = runBlocking {
        val sak = sisteSak().let {
            it.copy(
                unntaksvilkaarInnvilget = unntaksvilkaarInnvilget,
                sisteVedtak = it.sisteVedtak.copy(
                    maxdatoOrdinaer = maksdato.plusDays(avvikIDager),
                    maxdatoUnntak = maksdato
                )
            )
        }

        assertEquals(
            when {
                avvikIDager !in -5L..5L -> IkkeRelevantSøknad
                unntaksvilkaarInnvilget == true -> HarBruktOppForlengelsen
                unntaksvilkaarInnvilget == false -> VedtattAtIkkeForlenges
                else -> IkkeRelevantSøknad
            },
            vurder(sak)
        )
    }

    @ParameterizedTest
    @CsvSource(
        "til, true", "til, false", "til, null",
        "ordinær, true", "ordinær, false", "ordinær, null",
        "aap, true", "aap, false", "aap, null",
        nullValues = ["null"]
    )
    fun `søknaden er ikke relevant når en nødvendig dato mangler`(
        manglendeDato: String,
        unntaksvilkaarInnvilget: Boolean?
    ) = runBlocking {
        val sak = sisteSak().let {
            val vedtak = it.sisteVedtak.copy(maxdatoUnntak = maksdato)
            it.copy(
                unntaksvilkaarInnvilget = unntaksvilkaarInnvilget,
                sisteVedtak = when (manglendeDato) {
                    "til" -> vedtak.copy(til = null)
                    "ordinær" -> vedtak.copy(maxdatoOrdinaer = null)
                    "aap" -> vedtak.copy(maxdatoAap = null)
                    else -> error("Ukjent dato: $manglendeDato")
                }
            )
        }

        assertEquals(IkkeRelevantSøknad, vurder(sak))
    }

    @ParameterizedTest
    @CsvSource(
        "-1, true", "0, true", "181, true", "182, true", "183, true",
        "-1, false", "0, false", "181, false", "182, false", "183, false",
        "-1, null", "0, null", "181, null", "182, null", "183, null",
        nullValues = ["null"]
    )
    fun `effektiv maksdato må være mindre enn 26 uker etter mottatt dato uansett vurdering av unntak`(
        dagerTilEffektivMaksdato: Long,
        unntaksvilkaarInnvilget: Boolean?
    ) = runBlocking {
        val sak = sisteSak().let {
            it.copy(
                unntaksvilkaarInnvilget = unntaksvilkaarInnvilget,
                sisteVedtak = it.sisteVedtak.copy(
                    maxdatoAap = maksdato.plusDays(dagerTilEffektivMaksdato),
                    maxdatoUnntak = maksdato
                )
            )
        }

        assertEquals(
            when {
                dagerTilEffektivMaksdato >= 182 -> IkkeRelevantSøknad
                unntaksvilkaarInnvilget == true -> HarBruktOppForlengelsen
                unntaksvilkaarInnvilget == false -> VedtattAtIkkeForlenges
                else -> IkkeRelevantSøknad
            },
            vurder(sak)
        )
    }

    private suspend fun vurder(
        sisteSak: SisteSakDetaljer?,
        mottattDato: LocalDate = maksdato
    ): ArenaService.OppbruktRettighetResultat {
        val arena = gatewayProvider.provide(ArenaoppslagGateway::class)
        val person = mockk<Person>()
        coEvery { arena.historikkStatusDetalj(person, mottattDato) } returns
                HistorikkStatusDetaljResponse(null, sisteSak, null)

        return arenaService.harOppbruktRettighet(person, mottattDato, 1)
    }

    private fun sisteSak() = SisteSakDetaljer(
        sakId = 1234,
        saknummer = "1234-25",
        sakStatus = "AKTIV",
        sakRegistrert = LocalDate.of(2025, 1, 1),
        sakAvsluttet = null,
        sisteVedtak = VedtakMedMaksdato(
            vedtakId = 1,
            aktfaseKode = "AU",
            vedtaktypeKode = "O",
            vedtakstatuskode = "IVERK",
            fra = LocalDate.of(2025, 1, 1),
            til = maksdato,
            maxdatoOrdinaer = maksdato,
            maxdatoUnntak = null,
            maxdatoAap = maksdato
        ),
        unntaksvilkaarGjelderFra = null,
        unntaksvilkaarInnvilget = null,
        vedtakene = listOf(
            ArenaVedtak(
                sakId = 1234,
                statusKode = "IVERK",
                vedtaktypeKode = "O",
                fraOgMed = LocalDate.of(2025, 1, 1),
                tilDato = maksdato,
                rettighetkode = "AAP",
                utfallkode = "JA",
                vedtakId = 1,
                aktivitetsfaseKode = "AU"
            )
        )
    )
}

class ArenaoppslagGatewayMock : ArenaoppslagGateway {
    companion object : Factory<ArenaoppslagGatewayMock> {
        private val klient = mockk<ArenaoppslagGatewayMock>(relaxed = true)
        override fun konstruer() = klient
    }

    override suspend fun harHistorikk(person: Person): Boolean = false

    override suspend fun harSignifikantHistorikk(person: Person, mottattDato: LocalDate): SignifikantHistorikkResponse {
        return SignifikantHistorikkResponse.ingen
    }

    override suspend fun historikkStatusDetalj(
        person: Person,
        mottattDato: LocalDate
    ): HistorikkStatusDetaljResponse {
        return HistorikkStatusDetaljResponse.ingen
    }
}
