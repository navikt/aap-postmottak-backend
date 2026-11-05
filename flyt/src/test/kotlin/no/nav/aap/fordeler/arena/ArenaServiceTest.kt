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
import no.nav.aap.fordeler.arena.ArenaService.OppbruktRettighetResultat
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
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate

private const val TOLERANSE_I_DAGER = 5L
private const val PERIODE_FØR_MAKSDATO_I_UKER = 26L
private const val PERIODE_FØR_MAKSDATO_I_DAGER = PERIODE_FØR_MAKSDATO_I_UKER * 7

class ArenaServiceTest {

    private val gatewayProvider = createGatewayProvider {
        register<ArenaoppslagGatewayMock>()
    }

    private val arenaService = ArenaService(gatewayProvider)
    private val maksdato = LocalDate.of(2026, 12, 12)

    @Test
    fun `søker uten en sak i Arena har ikke en kant-i-kant-søknad`() = runBlocking {
        assertVurderingAvOppbruktRettighet(null, IkkeRelevantSøknad, skalTilKelvin = false)
    }

    @ParameterizedTest(name = "{displayName}: {0}")
    @ValueSource(strings = [
        "UA",
        "AU"
    ])
    fun `søker i arbeidsavklaring eller arbeidsutprøving kan ha brukt opp ordinær kvote og fått avslag på forlengelse`(
        aktivitetsfase: String
    ) = runBlocking {
        val sak = sakMedAvslåttForlengelse().let {
            it.copy(sisteVedtak = it.sisteVedtak.copy(aktfaseKode = aktivitetsfase))
        }

        assertVurderingAvOppbruktRettighet(sak, VedtattAtIkkeForlenges, skalTilKelvin = true)
    }

    @ParameterizedTest(name = "{displayName}: {0}")
    @ValueSource(strings = [
        "UA",
        "AU"
    ])
    fun `søker i arbeidsavklaring eller arbeidsutprøving kan ha brukt opp innvilget forlengelse`(
        aktivitetsfase: String
    ) = runBlocking {
        val sak = sakMedInnvilgetForlengelse().let {
            it.copy(sisteVedtak = it.sisteVedtak.copy(aktfaseKode = aktivitetsfase))
        }

        assertVurderingAvOppbruktRettighet(sak, HarBruktOppForlengelsen, skalTilKelvin = true)
    }

    @ParameterizedTest(name = "{displayName}: {0}")
    @ValueSource(strings = [
        "UVUP",
        "FA",
        "SPE",
        "IKKE"
    ])
    fun `søker uten siste vedtak i aktivitetsfase arbeidsavklaring eller arbeidsutprøving har ikke en kant-i-kant-søknad`(
        aktivitetsfase: String
    ) = runBlocking {
        val sak = sakMedAvslåttForlengelse().let {
            it.copy(sisteVedtak = it.sisteVedtak.copy(aktfaseKode = aktivitetsfase))
        }

        assertVurderingAvOppbruktRettighet(sak, IkkeRelevantSøknad, skalTilKelvin = false)
    }

    @ParameterizedTest(name = "{displayName}: {0}")
    @ValueSource(strings = [
        "UVUP",
        "FA",
        "SPE",
        "IKKE"
    ])
    fun `oppbrukt forlengelse gir ikke en kant-i-kant-søknad uten siste vedtak i aktivitetsfase arbeidsavklaring eller arbeidsutprøving`(
        aktivitetsfase: String
    ) = runBlocking {
        val sak = sakMedInnvilgetForlengelse().let {
            it.copy(sisteVedtak = it.sisteVedtak.copy(aktfaseKode = aktivitetsfase))
        }

        assertVurderingAvOppbruktRettighet(sak, IkkeRelevantSøknad, skalTilKelvin = false)
    }

    @ParameterizedTest(name = "{displayName}: {0}")
    @ValueSource(longs = [
        1,
        30
    ])
    fun `ny søknad er ikke kant-i-kant når søker allerede har fått innvilget AAP i Arena for en fremtidig periode`(
        dagerTilVedtaketStarter: Long
    ) = runBlocking {
        val mottattDato = maksdato.minusDays(30)
        val sak = sakMedAvslåttForlengelse().let {
            it.copy(
                sisteVedtak = it.sisteVedtak.copy(
                    fra = mottattDato.plusDays(dagerTilVedtaketStarter)
                )
            )
        }

        assertVurderingAvOppbruktRettighet(sak, IkkeRelevantSøknad, skalTilKelvin = false, mottattDato = mottattDato)
    }

    @ParameterizedTest(name = "{displayName}: {0}")
    @NullSource
    @ValueSource(longs = [
        0,
        30
    ])
    fun `et fremtidig AAP-vedtak gjør søknaden uaktuell også når søker har fått innvilget forlengelse`(
        dagerTilMaksdatoForUnntak: Long?
    ) = runBlocking {
        val mottattDato = maksdato.minusDays(30)
        val sak = sakMedInnvilgetForlengelse().let {
            it.copy(
                sisteVedtak = it.sisteVedtak.copy(
                    fra = mottattDato.plusDays(1),
                    maxdatoUnntak = dagerTilMaksdatoForUnntak?.let(maksdato::plusDays)
                )
            )
        }

        assertVurderingAvOppbruktRettighet(sak, IkkeRelevantSøknad, skalTilKelvin = false, mottattDato = mottattDato)
    }

    @Test
    fun `et fremtidig AAP-vedtak gjør søknaden uaktuell selv om forlengelse ikke er vurdert`() = runBlocking {
        val mottattDato = maksdato.minusDays(30)
        val sak = testDataMedSisteSakDetaljer().let {
            it.copy(sisteVedtak = it.sisteVedtak.copy(fra = mottattDato.plusDays(1)))
        }

        assertVurderingAvOppbruktRettighet(sak, IkkeRelevantSøknad, skalTilKelvin = false, mottattDato = mottattDato)
    }

    @ParameterizedTest(name = "{displayName}: {0}")
    @ValueSource(longs = [
        -1,
        0
    ])
    fun `søker kan ha brukt opp forlengelsen når vedtaket starter senest den dagen søknaden mottas`(
        dagerTilVedtaketStarter: Long
    ) = runBlocking {
        val mottattDato = maksdato.minusDays(30)
        val sak = sakMedInnvilgetForlengelse().let {
            it.copy(
                sisteVedtak = it.sisteVedtak.copy(
                    fra = mottattDato.plusDays(dagerTilVedtaketStarter)
                )
            )
        }

        assertVurderingAvOppbruktRettighet(sak, HarBruktOppForlengelsen, skalTilKelvin = true, mottattDato = mottattDato)
    }

    @Test
    fun `manglende startdato hindrer ikke at innvilget forlengelse regnes som oppbrukt`() = runBlocking {
        val sak = sakMedInnvilgetForlengelse().let {
            it.copy(sisteVedtak = it.sisteVedtak.copy(fra = null))
        }

        assertVurderingAvOppbruktRettighet(sak, HarBruktOppForlengelsen, skalTilKelvin = true)
    }

    @ParameterizedTest(name = "{displayName}: {0}")
    @ValueSource(booleans = [
        true,
        false
    ])
    fun `oppbrukt kvote gir ikke en kant-i-kant-søknad når unntaksvilkårene for forlengelse ikke er vurdert`(
        harMaksdatoForUnntak: Boolean
    ) = runBlocking {
        val sak = testDataMedSisteSakDetaljer().let {
            it.copy(
                sisteVedtak = it.sisteVedtak.copy(
                    maxdatoUnntak = if (harMaksdatoForUnntak) maksdato else null
                )
            )
        }

        assertVurderingAvOppbruktRettighet(sak, IkkeRelevantSøknad, skalTilKelvin = false)
    }

    @Test
    fun `innvilget forlengelse regnes ikke som oppbrukt når maksdato for unntak mangler`() = runBlocking {
        val sak = sakMedInnvilgetForlengelse().let {
            it.copy(sisteVedtak = it.sisteVedtak.copy(maxdatoUnntak = null))
        }

        assertVurderingAvOppbruktRettighet(sak, VedtattForlenget, skalTilKelvin = false)
    }

    @ParameterizedTest(name = "{displayName}: {0}")
    @ValueSource(longs = [
        -TOLERANSE_I_DAGER,
        0,
        TOLERANSE_I_DAGER
    ])
    fun `søker har brukt opp forlengelsen når vedtaket løper til maksdato for unntak med fem dagers toleranse`(
        avvikFraMaksdatoForUnntak: Long
    ) = runBlocking {
        val sak = sakMedInnvilgetForlengelse().let {
            it.copy(
                sisteVedtak = it.sisteVedtak.copy(
                    maxdatoUnntak = maksdato.plusDays(avvikFraMaksdatoForUnntak)
                )
            )
        }

        assertVurderingAvOppbruktRettighet(sak, HarBruktOppForlengelsen, skalTilKelvin = true)
    }

    @ParameterizedTest(name = "{displayName}: {0}")
    @ValueSource(longs = [
        -(TOLERANSE_I_DAGER + 1),
        TOLERANSE_I_DAGER + 1
    ])
    fun `innvilget forlengelse regnes ikke som oppbrukt når sluttdato avviker mer enn fem dager fra maksdato for unntak`(
        avvikFraMaksdatoForUnntak: Long
    ) = runBlocking {
        val sak = sakMedInnvilgetForlengelse().let {
            it.copy(
                sisteVedtak = it.sisteVedtak.copy(
                    maxdatoUnntak = maksdato.plusDays(avvikFraMaksdatoForUnntak)
                )
            )
        }

        assertVurderingAvOppbruktRettighet(sak, VedtattForlenget, skalTilKelvin = false)
    }

    @ParameterizedTest(name = "{displayName}: {0}")
    @NullSource
    @ValueSource(longs = [
        -(TOLERANSE_I_DAGER + 1),
        -TOLERANSE_I_DAGER,
        0,
        TOLERANSE_I_DAGER,
        TOLERANSE_I_DAGER + 1
    ])
    fun `søker har brukt opp ordinær kvote og fått avslag på forlengelse uavhengig av maksdato for unntak`(
        avvikFraMaksdatoForUnntak: Long?
    ) = runBlocking {
        val sak = sakMedAvslåttForlengelse().let {
            it.copy(
                sisteVedtak = it.sisteVedtak.copy(
                    maxdatoUnntak = avvikFraMaksdatoForUnntak?.let(maksdato::plusDays)
                )
            )
        }

        assertVurderingAvOppbruktRettighet(sak, VedtattAtIkkeForlenges, skalTilKelvin = true)
    }

    @ParameterizedTest(name = "{displayName}: {0}")
    @ValueSource(longs = [
        -TOLERANSE_I_DAGER,
        0,
        TOLERANSE_I_DAGER
    ])
    fun `søker har brukt opp ordinær kvote og fått avslag på forlengelse når vedtaket løper til ordinær maksdato med fem dagers toleranse`(
        avvikFraOrdinærMaksdato: Long
    ) = runBlocking {
        val sak = sakMedAvslåttForlengelse().let {
            it.copy(
                sisteVedtak = it.sisteVedtak.copy(
                    maxdatoOrdinaer = maksdato.plusDays(avvikFraOrdinærMaksdato)
                )
            )
        }

        assertVurderingAvOppbruktRettighet(sak, VedtattAtIkkeForlenges, skalTilKelvin = true)
    }

    @ParameterizedTest(name = "{displayName}: {0}")
    @ValueSource(longs = [
        -(TOLERANSE_I_DAGER + 1),
        TOLERANSE_I_DAGER + 1
    ])
    fun `avslag på forlengelse betyr ikke oppbrukt ordinær kvote når vedtaket ikke løper til ordinær maksdato`(
        avvikFraOrdinærMaksdato: Long
    ) = runBlocking {
        val sak = sakMedAvslåttForlengelse().let {
            it.copy(
                sisteVedtak = it.sisteVedtak.copy(
                    maxdatoOrdinaer = maksdato.plusDays(avvikFraOrdinærMaksdato)
                )
            )
        }

        assertVurderingAvOppbruktRettighet(sak, IkkeRelevantSøknad, skalTilKelvin = false)
    }

    @ParameterizedTest(name = "{displayName}: {0}")
    @NullSource
    @ValueSource(longs = [
        -365,
        -(TOLERANSE_I_DAGER + 1),
        0,
        TOLERANSE_I_DAGER + 1
    ])
    fun `søker har brukt opp forlengelsen ved utløpet av unntaksperioden uavhengig av ordinær maksdato`(
        avvikFraOrdinærMaksdato: Long?
    ) = runBlocking {
        val sak = sakMedInnvilgetForlengelse().let {
            it.copy(
                sisteVedtak = it.sisteVedtak.copy(
                    maxdatoOrdinaer = avvikFraOrdinærMaksdato?.let(maksdato::plusDays)
                )
            )
        }

        assertVurderingAvOppbruktRettighet(sak, HarBruktOppForlengelsen, skalTilKelvin = true)
    }

    @ParameterizedTest(name = "{displayName}: {0}")
    @NullSource
    @ValueSource(longs = [
        -365,
        -(TOLERANSE_I_DAGER + 1),
        0,
        TOLERANSE_I_DAGER + 1
    ])
    fun `forlengelsen er ikke brukt opp før unntaksperioden utløper uavhengig av ordinær maksdato`(
        avvikFraOrdinærMaksdato: Long?
    ) = runBlocking {
        val maksdatoForUnntak = maksdato.plusDays(30)
        val sak = sakMedInnvilgetForlengelse().let {
            it.copy(
                sisteVedtak = it.sisteVedtak.copy(
                    maxdatoOrdinaer = avvikFraOrdinærMaksdato?.let(maksdato::plusDays),
                    maxdatoUnntak = maksdatoForUnntak,
                    maxdatoAap = maksdatoForUnntak
                )
            )
        }

        assertVurderingAvOppbruktRettighet(sak, VedtattForlenget, skalTilKelvin = false)
    }

    @ParameterizedTest(name = "{displayName}: {0}")
    @NullSource
    @ValueSource(booleans = [false])
    fun `ordinær kvote regnes ikke som oppbrukt når vedtakets sluttdato mangler`(
        unntaksvilkaarInnvilget: Boolean?
    ) = runBlocking {
        val sak = testDataMedSisteSakDetaljer().let {
            it.copy(
                unntaksvilkaarInnvilget = unntaksvilkaarInnvilget,
                sisteVedtak = it.sisteVedtak.copy(til = null, maxdatoUnntak = maksdato)
            )
        }

        assertVurderingAvOppbruktRettighet(sak, IkkeRelevantSøknad, skalTilKelvin = false)
    }

    @ParameterizedTest(name = "{displayName}: {0}")
    @NullSource
    @ValueSource(booleans = [false])
    fun `ordinær kvote regnes ikke som oppbrukt når ordinær maksdato mangler`(
        unntaksvilkaarInnvilget: Boolean?
    ) = runBlocking {
        val sak = testDataMedSisteSakDetaljer().let {
            it.copy(
                unntaksvilkaarInnvilget = unntaksvilkaarInnvilget,
                sisteVedtak = it.sisteVedtak.copy(maxdatoOrdinaer = null, maxdatoUnntak = maksdato)
            )
        }

        assertVurderingAvOppbruktRettighet(sak, IkkeRelevantSøknad, skalTilKelvin = false)
    }

    @ParameterizedTest(name = "{displayName}: {0}")
    @NullSource
    @ValueSource(booleans = [
        true,
        false
    ])
    fun `uten effektiv maksdato kan ikke søknaden vurderes som kant-i-kant`(
        unntaksvilkaarInnvilget: Boolean?
    ) = runBlocking {
        val sak = sakMedInnvilgetForlengelse().let {
            it.copy(
                unntaksvilkaarInnvilget = unntaksvilkaarInnvilget,
                sisteVedtak = it.sisteVedtak.copy(maxdatoAap = null)
            )
        }

        assertVurderingAvOppbruktRettighet(sak, IkkeRelevantSøknad, skalTilKelvin = false)
    }

    @ParameterizedTest(name = "{displayName}: {0}")
    @ValueSource(booleans = [
        true,
        false
    ])
    fun `innvilget forlengelse regnes ikke som oppbrukt uten sluttdato uavhengig av ordinær maksdato`(
        harOrdinærMaksdato: Boolean
    ) = runBlocking {
        val sak = sakMedInnvilgetForlengelse().let {
            it.copy(
                sisteVedtak = it.sisteVedtak.copy(
                    til = null,
                    maxdatoOrdinaer = if (harOrdinærMaksdato) maksdato else null
                )
            )
        }

        assertVurderingAvOppbruktRettighet(sak, VedtattForlenget, skalTilKelvin = false)
    }

    @Test
    fun `søker har brukt opp forlengelsen selv om ordinær kvote ble brukt opp i et tidligere vedtak`() = runBlocking {
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

        assertVurderingAvOppbruktRettighet(
            sak,
            HarBruktOppForlengelsen,
            skalTilKelvin = true,
            mottattDato = LocalDate.of(2026, 11, 4)
        )
    }

    @ParameterizedTest(name = "{displayName}: {0}")
    @ValueSource(longs = [
        -1,
        0,
        PERIODE_FØR_MAKSDATO_I_DAGER - 1
    ])
    fun `ny søknad nær eller etter effektiv maksdato er kant-i-kant når ordinær kvote er brukt opp og forlengelse er avslått`(
        dagerTilEffektivMaksdato: Long
    ) = runBlocking {
        val sak = sakMedAvslåttForlengelse().let {
            it.copy(
                sisteVedtak = it.sisteVedtak.copy(
                    maxdatoAap = maksdato.plusDays(dagerTilEffektivMaksdato)
                )
            )
        }

        assertVurderingAvOppbruktRettighet(sak, VedtattAtIkkeForlenges, skalTilKelvin = true)
    }

    @ParameterizedTest(name = "{displayName}: {0}")
    @ValueSource(longs = [
        -1,
        0,
        PERIODE_FØR_MAKSDATO_I_DAGER - 1
    ])
    fun `ny søknad nær eller etter effektiv maksdato er kant-i-kant når innvilget forlengelse er brukt opp`(
        dagerTilEffektivMaksdato: Long
    ) = runBlocking {
        val sak = sakMedInnvilgetForlengelse().let {
            it.copy(
                sisteVedtak = it.sisteVedtak.copy(
                    maxdatoAap = maksdato.plusDays(dagerTilEffektivMaksdato)
                )
            )
        }

        assertVurderingAvOppbruktRettighet(sak, HarBruktOppForlengelsen, skalTilKelvin = true)
    }

    @ParameterizedTest(name = "{displayName}: {0}")
    @ValueSource(longs = [
        PERIODE_FØR_MAKSDATO_I_DAGER,
        PERIODE_FØR_MAKSDATO_I_DAGER + 1
    ])
    fun `søker har søkt for tidlig med minst 26 uker til effektiv maksdato selv om forlengelse er avslått`(
        dagerTilEffektivMaksdato: Long
    ) = runBlocking {
        val sak = sakMedAvslåttForlengelse().let {
            it.copy(
                sisteVedtak = it.sisteVedtak.copy(
                    maxdatoAap = maksdato.plusDays(dagerTilEffektivMaksdato)
                )
            )
        }

        assertVurderingAvOppbruktRettighet(sak, IkkeRelevantSøknad, skalTilKelvin = false)
    }

    @ParameterizedTest(name = "{displayName}: {0}")
    @ValueSource(longs = [
        PERIODE_FØR_MAKSDATO_I_DAGER,
        PERIODE_FØR_MAKSDATO_I_DAGER + 1
    ])
    fun `søker har søkt for tidlig med minst 26 uker til effektiv maksdato selv om siste vedtak løper ut forlengelsen`(
        dagerTilEffektivMaksdato: Long
    ) = runBlocking {
        val sak = sakMedInnvilgetForlengelse().let {
            it.copy(
                sisteVedtak = it.sisteVedtak.copy(
                    maxdatoAap = maksdato.plusDays(dagerTilEffektivMaksdato)
                )
            )
        }

        assertVurderingAvOppbruktRettighet(sak, IkkeRelevantSøknad, skalTilKelvin = false)
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

    private suspend fun assertVurderingAvOppbruktRettighet(
        sisteSak: SisteSakDetaljer?,
        forventetResultat: OppbruktRettighetResultat,
        skalTilKelvin: Boolean,
        mottattDato: LocalDate = maksdato
    ) {
        val arena = gatewayProvider.provide(ArenaoppslagGateway::class)
        val person = mockk<Person>()
        coEvery { arena.historikkStatusDetalj(person, mottattDato) } returns
                HistorikkStatusDetaljResponse(null, sisteSak, null)

        val resultat = arenaService.harOppbruktRettighet(person, mottattDato, 1)
        coVerify(exactly = 1) { arena.historikkStatusDetalj(person, mottattDato) }
        assertEquals(forventetResultat, resultat)
        assertEquals(skalTilKelvin, resultat.skalTilKelvin())
    }

    private fun sakMedInnvilgetForlengelse() = testDataMedSisteSakDetaljer().let {
        it.copy(
            unntaksvilkaarInnvilget = true,
            sisteVedtak = it.sisteVedtak.copy(maxdatoUnntak = maksdato)
        )
    }

    private fun sakMedAvslåttForlengelse() =
        testDataMedSisteSakDetaljer().copy(unntaksvilkaarInnvilget = false)

    private fun testDataMedSisteSakDetaljer() = SisteSakDetaljer(
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
