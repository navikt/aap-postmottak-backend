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
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate

class ArenaServiceTest {

    private val gatewayProvider = createGatewayProvider {
        register<ArenaoppslagGatewayMock>()
    }

    private val arenaService = ArenaService(gatewayProvider)
    private val maksdato = LocalDate.of(2026, 12, 12)

    @Test
    fun `maksdato må være innen rimelig tid`() = runBlocking {
        val sak = sakMedId(1234).let {
            it.copy(sisteVedtak = it.sisteVedtak.copy(maxdatoUnntak = maksdato))
        }

        for (dagerFørMaksdato in listOf(181L, 182L, 183L, 0L, -1L)) {
            assertEquals(
                if (dagerFørMaksdato < 182) HarBruktOppForlengelsen else IkkeRelevantSøknad,
                vurder(sak, maksdato.minusDays(dagerFørMaksdato)),
                "$dagerFørMaksdato dager før maksdato"
            )
        }
    }

    @Test
    fun `søknaden er ikke relevant når det ikke finnes en siste sak`() = runBlocking {
        assertEquals(IkkeRelevantSøknad, vurder(null))
    }

    @Test
    fun `avslått forlengelse gir oppbrukt rettighet`() = runBlocking {
        val sak = sakMedId(1234).copy(unntaksvilkaarInnvilget = false)

        assertEquals(VedtattAtIkkeForlenges, vurder(sak))
    }

    @Test
    fun `innvilget forlengelse gir ikke oppbrukt rettighet`() = runBlocking {
        val sak = sakMedId(1234).copy(unntaksvilkaarInnvilget = true)

        assertEquals(VedtattForlenget, vurder(sak))
    }

    @Test
    fun `forlengelsen er oppbrukt når sluttdato er lik begge maksdatoene uten vurdering av unntak`() =
        runBlocking {
            val sak = sakMedId(1234).let {
                it.copy(sisteVedtak = it.sisteVedtak.copy(maxdatoUnntak = maksdato))
            }

            assertEquals(HarBruktOppForlengelsen, vurder(sak))
        }

    @Test
    fun `søknaden er ikke relevant uten vurdering av unntak eller maksdato for unntak`() = runBlocking {
        assertEquals(IkkeRelevantSøknad, vurder(sakMedId(1234)))
    }

    @ParameterizedTest
    @CsvSource("true, false", "false, false", "true, true", "false, true")
    fun `vurderingen av unntak har forrang selv når sluttdato er lik maksdato for unntak`(
        unntaksvilkaarInnvilget: Boolean,
        søkerForTidlig: Boolean
    ) = runBlocking {
        val sak = sakMedId(1234).let {
            it.copy(
                unntaksvilkaarInnvilget = unntaksvilkaarInnvilget,
                sisteVedtak = it.sisteVedtak.copy(maxdatoUnntak = maksdato)
            )
        }
        val mottattDato = if (søkerForTidlig) maksdato.minusWeeks(26) else maksdato

        assertEquals(
            when {
                søkerForTidlig -> IkkeRelevantSøknad
                unntaksvilkaarInnvilget -> VedtattForlenget
                else -> VedtattAtIkkeForlenges
            },
            vurder(sak, mottattDato)
        )
    }

    @ParameterizedTest
    @ValueSource(longs = [-6, -5, 0, 5, 6])
    fun `sluttdato må være innen fem dager fra ordinær maksdato`(avvikIDager: Long) = runBlocking {
        val sak = sakMedId(1234).let {
            it.copy(
                unntaksvilkaarInnvilget = false,
                sisteVedtak = it.sisteVedtak.copy(maxdatoOrdinaer = maksdato.plusDays(avvikIDager))
            )
        }

        assertEquals(
            if (avvikIDager in -5L..5L) VedtattAtIkkeForlenges else IkkeRelevantSøknad,
            vurder(sak)
        )
    }

    @ParameterizedTest
    @ValueSource(longs = [-6, -5, 0, 5, 6])
    fun `sluttdato må være innen fem dager fra maksdato for unntak`(avvikIDager: Long) = runBlocking {
        val sak = sakMedId(1234).let {
            it.copy(sisteVedtak = it.sisteVedtak.copy(maxdatoUnntak = maksdato.plusDays(avvikIDager)))
        }

        assertEquals(
            if (avvikIDager in -5L..5L) HarBruktOppForlengelsen else IkkeRelevantSøknad,
            vurder(sak)
        )
    }

    @ParameterizedTest
    @CsvSource("til, true", "til, false", "ordinær, true", "ordinær, false", "aap, true", "aap, false")
    fun `søknaden er ikke relevant når en nødvendig dato mangler`(
        manglendeDato: String,
        unntaksvilkaarInnvilget: Boolean
    ) = runBlocking {
        val sak = sakMedId(1234).let {
            it.copy(
                unntaksvilkaarInnvilget = unntaksvilkaarInnvilget,
                sisteVedtak = when (manglendeDato) {
                    "til" -> it.sisteVedtak.copy(til = null)
                    "ordinær" -> it.sisteVedtak.copy(maxdatoOrdinaer = null)
                    "aap" -> it.sisteVedtak.copy(maxdatoAap = null)
                    else -> error("Ukjent dato: $manglendeDato")
                }
            )
        }

        assertEquals(IkkeRelevantSøknad, vurder(sak))
    }

    @Test
    fun `søknaden er ikke relevant når bare maksdato for unntak samsvarer med sluttdato`() = runBlocking {
        val sak = sakMedId(1234).let {
            it.copy(
                sisteVedtak = it.sisteVedtak.copy(
                    maxdatoOrdinaer = maksdato.minusYears(2),
                    maxdatoUnntak = maksdato
                )
            )
        }

        assertEquals(IkkeRelevantSøknad, vurder(sak))
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

    private fun sakMedId(sakId: Int): SisteSakDetaljer {
        return SisteSakDetaljer(
            sakId = sakId,
            saknummer = "$sakId-25",
            sakStatus = "AKTIV",
            sakRegistrert = LocalDate.of(2025, 1, 1),
            sakAvsluttet = null,
            sisteVedtak = VedtakMedMaksdato(
                vedtakId = sakId,
                aktfaseKode = "AU",
                vedtaktypeKode = "O",
                vedtakstatuskode = "IVERK",
                fra = LocalDate.of(2025, 1, 1),
                til = maksdato,
                maxdatoOrdinaer = maksdato,
                maxdatoUnntak = null,
                maxdatoAap = maksdato,
            ),
            unntaksvilkaarGjelderFra = null,
            unntaksvilkaarInnvilget = null,
            vedtakene = listOf(
                ArenaVedtak(
                    sakId = sakId,
                    statusKode = "IVERK",
                    vedtaktypeKode = "O",
                    fraOgMed = LocalDate.of(2025, 1, 1),
                    tilDato = LocalDate.of(2026, 1, 1),
                    rettighetkode = "AAP",
                    utfallkode = "JA",
                    1,
                    "AU"
                )
            )
        )
    }
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
