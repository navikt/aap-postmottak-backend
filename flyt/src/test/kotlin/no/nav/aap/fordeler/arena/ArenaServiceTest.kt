package no.nav.aap.fordeler.arena

import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
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
    fun `søknaden er ikke relevant når det ikke finnes en siste sak`() = runBlocking {
        assertEquals(IkkeRelevantSøknad, vurder(null))
    }

    @ParameterizedTest
    @CsvSource(
        "UA, true", "UA, false", "UA, null",
        "AU, true", "AU, false", "AU, null",
        "UVUP, true", "UVUP, false", "UVUP, null",
        "FA, true", "FA, false", "FA, null",
        "SPE, true", "SPE, false", "SPE, null",
        "IKKE, true", "IKKE, false", "IKKE, null",
        nullValues = ["null"]
    )
    fun `bare siste vedtak i UA eller AU kan gi oppbrukt rettighet`(
        aktivitetsfase: String,
        unntaksvilkaarInnvilget: Boolean?
    ) = runBlocking {
        val sak = sisteSak().let {
            it.copy(
                unntaksvilkaarInnvilget = unntaksvilkaarInnvilget,
                sisteVedtak = it.sisteVedtak.copy(aktfaseKode = aktivitetsfase, maxdatoUnntak = maksdato),
                vedtakene = it.vedtakene.map { vedtak -> vedtak.copy(aktivitetsfaseKode = aktivitetsfase) }
            )
        }

        assertEquals(
            when {
                aktivitetsfase !in listOf("UA", "AU") -> IkkeRelevantSøknad
                unntaksvilkaarInnvilget == true -> HarBruktOppForlengelsen
                unntaksvilkaarInnvilget == false -> VedtattAtIkkeForlenges
                else -> IkkeRelevantSøknad
            },
            vurder(sak)
        )
    }

    @Test
    fun `avslått forlengelse gir oppbrukt rettighet når sluttdato samsvarer med ordinær maksdato`() = runBlocking {
        val sak = sisteSak().copy(unntaksvilkaarInnvilget = false)

        assertEquals(VedtattAtIkkeForlenges, vurder(sak))
    }

    @Test
    fun `innvilget forlengelse uten maksdato for unntak gir ikke oppbrukt rettighet`() = runBlocking {
        val sak = sisteSak().copy(unntaksvilkaarInnvilget = true)

        assertEquals(VedtattForlenget, vurder(sak))
    }

    @Test
    fun `søknaden er ikke relevant når både vurdering av unntak og maksdato for unntak mangler`() = runBlocking {
        assertEquals(IkkeRelevantSøknad, vurder(sisteSak()))
    }

    @ParameterizedTest
    @CsvSource(
        "null, true, true", "-1, true, true", "0, true, true", "1, true, true",
        "null, true, false", "-1, true, false", "0, true, false", "1, true, false",
        "null, false, true", "-1, false, true", "0, false, true", "1, false, true",
        "null, false, false", "-1, false, false", "0, false, false", "1, false, false",
        "null, null, true", "-1, null, true", "0, null, true", "1, null, true",
        "null, null, false", "-1, null, false", "0, null, false", "1, null, false",
        nullValues = ["null"]
    )
    fun `bare startdato etter mottaksdato avviser søknaden som fremtidig`(
        dagerTilFraDato: Long?,
        unntaksvilkaarInnvilget: Boolean?,
        sluttdatoSamsvarerMedMaksdatoForUnntak: Boolean
    ) = runBlocking {
        val mottattDato = maksdato.minusDays(30)
        val sak = sisteSak().let {
            it.copy(
                unntaksvilkaarInnvilget = unntaksvilkaarInnvilget,
                sisteVedtak = it.sisteVedtak.copy(
                    fra = dagerTilFraDato?.let(mottattDato::plusDays),
                    maxdatoUnntak = if (sluttdatoSamsvarerMedMaksdatoForUnntak) maksdato else null
                )
            )
        }

        val resultat = vurder(sak, mottattDato)
        assertEquals(
            when {
                dagerTilFraDato != null && dagerTilFraDato > 0 -> IkkeRelevantSøknad
                unntaksvilkaarInnvilget == false -> VedtattAtIkkeForlenges
                unntaksvilkaarInnvilget == true && sluttdatoSamsvarerMedMaksdatoForUnntak -> HarBruktOppForlengelsen
                unntaksvilkaarInnvilget == true -> VedtattForlenget
                else -> IkkeRelevantSøknad
            },
            resultat
        )
        assertEquals(
            (dagerTilFraDato == null || dagerTilFraDato <= 0) &&
                    (unntaksvilkaarInnvilget == false ||
                            (unntaksvilkaarInnvilget == true && sluttdatoSamsvarerMedMaksdatoForUnntak)),
            resultat.skalTilKelvin()
        )
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
        "-6, false", "-5, false", "0, false", "5, false", "6, false",
        "-6, null", "-5, null", "0, null", "5, null", "6, null",
        nullValues = ["null"]
    )
    fun `uten innvilget forlengelse krever oppbrukt rettighet sluttdato innen fem dager fra ordinær maksdato`(
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
                unntaksvilkaarInnvilget == false -> VedtattAtIkkeForlenges
                else -> IkkeRelevantSøknad
            },
            vurder(sak)
        )
    }

    @ParameterizedTest
    @CsvSource(
        "null, null", "null, -6", "null, -5", "null, 0", "null, 5", "null, 6",
        "-365, null", "-365, -6", "-365, -5", "-365, 0", "-365, 5", "-365, 6",
        "-6, null", "-6, -6", "-6, -5", "-6, 0", "-6, 5", "-6, 6",
        "0, null", "0, -6", "0, -5", "0, 0", "0, 5", "0, 6",
        "6, null", "6, -6", "6, -5", "6, 0", "6, 5", "6, 6",
        nullValues = ["null"]
    )
    fun `innvilget forlengelse vurderes mot maksdato for unntak uavhengig av ordinær maksdato`(
        avvikFraOrdinærMaksdato: Long?,
        avvikFraMaksdatoForUnntak: Long?
    ) = runBlocking {
        val sak = sisteSak().let {
            it.copy(
                unntaksvilkaarInnvilget = true,
                sisteVedtak = it.sisteVedtak.copy(
                    maxdatoOrdinaer = avvikFraOrdinærMaksdato?.let(maksdato::plusDays),
                    maxdatoUnntak = avvikFraMaksdatoForUnntak?.let(maksdato::plusDays)
                )
            )
        }

        assertEquals(
            if (avvikFraMaksdatoForUnntak != null && avvikFraMaksdatoForUnntak in -5L..5L) {
                HarBruktOppForlengelsen
            } else {
                VedtattForlenget
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
    fun `manglende datoer vurderes ut fra om forlengelse er innvilget`(
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

        assertEquals(
            when {
                unntaksvilkaarInnvilget != true || manglendeDato == "aap" -> IkkeRelevantSøknad
                manglendeDato == "til" -> VedtattForlenget
                else -> HarBruktOppForlengelsen
            },
            vurder(sak)
        )
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `innvilget forlengelse uten sluttdato gir vedtatt forlenget uansett ordinær maksdato`(
        harOrdinærMaksdato: Boolean
    ) = runBlocking {
        val sak = sisteSak().let {
            it.copy(
                unntaksvilkaarInnvilget = true,
                sisteVedtak = it.sisteVedtak.copy(
                    til = null,
                    maxdatoOrdinaer = if (harOrdinærMaksdato) maksdato else null,
                    maxdatoUnntak = null
                )
            )
        }

        assertEquals(VedtattForlenget, vurder(sak))
    }

    @Test
    fun `oppbrukt forlengelse krever ikke at sluttdato samsvarer med ordinær maksdato`() = runBlocking {
        val sak = SisteSakDetaljer(
            sakId = 1234,
            saknummer = "2026-1234",
            sakStatus = "AKTIV",
            sakRegistrert = LocalDate.of(2026, 11, 3),
            sakAvsluttet = null,
            unntaksvilkaarInnvilget = true,
            unntaksvilkaarGjelderFra = LocalDate.of(2026, 10, 28),
            sisteVedtak = VedtakMedMaksdato(
                vedtakId = 2,
                aktfaseKode = "UA",
                vedtaktypeKode = "E",
                vedtakstatuskode = "IVERK",
                fra = LocalDate.of(2026, 10, 28),
                til = LocalDate.of(2026, 11, 29),
                maxdatoOrdinaer = LocalDate.of(2026, 10, 27),
                maxdatoUnntak = LocalDate.of(2026, 11, 29),
                maxdatoAap = LocalDate.of(2026, 11, 29),
            ),
            vedtakene = listOf(
                ArenaVedtak(1234, "IVERK", "O", LocalDate.of(2026, 10, 1), null, "AA115", "JA", 1, "IKKE"),
                ArenaVedtak(1234, "IVERK", "E", LocalDate.of(2026, 10, 28), LocalDate.of(2026, 11, 29), "AAP", "JA", 2, "UA"),
                ArenaVedtak(1234, "AVSLU", "O", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 27), "AAP", "JA", 3, "UA"),
            ),
        )

        assertEquals(HarBruktOppForlengelsen, vurder(sak, LocalDate.of(2026, 11, 4)))
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

    @Test
    fun `feil fra Arena-oppslag videreføres`() {
        val arena = gatewayProvider.provide(ArenaoppslagGateway::class)
        val person = mockk<Person>()
        val feil = IllegalStateException("Arena-oppslag feilet")
        coEvery { arena.historikkStatusDetalj(person, maksdato) } throws feil

        val kastetFeil = assertThrows(IllegalStateException::class.java) {
            runBlocking { arenaService.harOppbruktRettighet(person, maksdato, 1) }
        }

        assertSame(feil, kastetFeil)
        coVerify(exactly = 1) { arena.historikkStatusDetalj(person, maksdato) }
    }

    private suspend fun vurder(
        sisteSak: SisteSakDetaljer?,
        mottattDato: LocalDate = maksdato
    ): ArenaService.OppbruktRettighetResultat {
        val arena = gatewayProvider.provide(ArenaoppslagGateway::class)
        val person = mockk<Person>()
        coEvery { arena.historikkStatusDetalj(person, mottattDato) } returns
                HistorikkStatusDetaljResponse(null, sisteSak, null)

        val resultat = arenaService.harOppbruktRettighet(person, mottattDato, 1)
        coVerify(exactly = 1) { arena.historikkStatusDetalj(person, mottattDato) }
        return resultat
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
