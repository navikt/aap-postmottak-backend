package no.nav.aap.fordeler.arena

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaVedtak
import no.nav.aap.arenaoppslag.kontrakt.apiv1.HistorikkStatusDetaljResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SignifikantHistorikkResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SisteSakDetaljer
import no.nav.aap.arenaoppslag.kontrakt.apiv1.VedtakMedMaksdato
import no.nav.aap.fordeler.arena.ArenaService.Companion.hentMaksAntallUkerFørUtløpMaksimalt
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.postmottak.gateway.ArenaoppslagGateway
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.klient.createGatewayProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ArenaServiceTest {

    private val gatewayProvider = createGatewayProvider {
        register<ArenaoppslagGatewayMock>()
    }

    private val arenaService = ArenaService(gatewayProvider)

    @Test
    fun `maximum date must be less than 26 weeks away`() = runBlocking {
        val maksdato = LocalDate.of(2026, 12, 12)
        val vedtak = requireNotNull(sakMedId(1234).sisteVedtak).copy(
            til = maksdato,
            maxdatoOrdinaer = maksdato,
            maxdatoAap = maksdato
        )
        val sisteSak = mockk<SisteSakDetaljer>(relaxed = true)
        every { sisteSak.sisteVedtak } returns vedtak
        every { sisteSak.unntaksvilkaarInnvilget } returns null
        val historikk = mockk<HistorikkStatusDetaljResponse>()
        every { historikk.sisteSakOgVedtak } returns sisteSak
        val arena = gatewayProvider.provide(ArenaoppslagGateway::class)
        coEvery { arena.historikkStatusDetalj(any(), any()) } returns historikk
        val person = mockk<Person>()

        for (dagerFørMaksdato in listOf(181L, 182L, 183L)) {
            assertEquals(
                dagerFørMaksdato < 182,
                arenaService.harOppbruktRettighet(person, maksdato.minusDays(dagerFørMaksdato), 1),
                "$dagerFørMaksdato days before maximum date"
            )
        }
    }


    @Test
    fun `returnerer false naar listen er tom`() {
        assertFalse(arenaService.harSignifikanteVedtakUtoverTypeAap(emptyList()))
    }

    @Test
    fun `harSignifikanteVedtakUtoverTypeAap returnerer true naar det er klage`() {
        val vedtak = listOf(
            arenaVedtak(rettighetskode = "AAP"),
            arenaVedtak(rettighetskode = "AA115"),
            arenaVedtak(rettighetskode = "KLAG1")
        )

        assertTrue(arenaService.harSignifikanteVedtakUtoverTypeAap(vedtak))
    }

    @Test
    fun `harSignifikanteVedtakUtoverTypeAap returnerer true naar rettighetskoder ikke inneholder AAP`() {
        val vedtak = listOf(arenaVedtak(rettighetskode = "KLAG1"))

        assertTrue(arenaService.harSignifikanteVedtakUtoverTypeAap(vedtak))
    }

    @Test
    fun `harSignifikanteVedtakUtoverTypeAap returnerer true naar det finnes flere rettighetskoder inkludert AAP`() {
        val vedtak = listOf(
            arenaVedtak(rettighetskode = "AAP"),
            arenaVedtak(rettighetskode = "KLAG1")
        )

        assertTrue(arenaService.harSignifikanteVedtakUtoverTypeAap(vedtak))
    }

    @Test
    fun `harFlereSignifikanteSaker returnerer false naar det kun finnes en sak`() {
        val sisteSakId = 1234
        val historikk = SignifikantHistorikkResponse(
            true,
            listOf(
                arenaVedtak(sakId = sisteSakId, rettighetskode = "AAP"),
                arenaVedtak(sakId = sisteSakId, rettighetskode = "AA115")
            )
        )
        val sisteSak = sakMedId(sisteSakId)

        assertFalse(arenaService.harFlereSignifikanteSaker(historikk.saker(), sisteSak))
    }

    @Test
    fun `harFlereSignifikanteSaker returnerer true naar det finnes fler saker enn sisteSak`() {
        val historikk = SignifikantHistorikkResponse(
            true,
            listOf(
                arenaVedtak(sakId = 1234, rettighetskode = "AAP"),
                arenaVedtak(sakId = 5678, rettighetskode = "AAP")
            )
        )
        val sisteSak = sakMedId(1234)

        assertTrue(arenaService.harFlereSignifikanteSaker(historikk.saker(), sisteSak))
    }

    @Test
    fun `harFlereSignifikanteSaker returnerer false naar historikk er tomt`() {
        val historikk = SignifikantHistorikkResponse.ingen
        val sisteSak = sakMedId(1234)

        assertFalse(arenaService.harFlereSignifikanteSaker(historikk.saker(), sisteSak))
    }

    @Test
    fun `harFlereSignifikanteSaker returnerer true naar det finnes tre saker`() {
        val historikk = SignifikantHistorikkResponse(
            true,
            listOf(
                arenaVedtak(sakId = 1111, rettighetskode = "AAP"),
                arenaVedtak(sakId = 2222, rettighetskode = "AA115"),
                arenaVedtak(sakId = 3333, rettighetskode = "AAP")
            )
        )
        val sisteSak = sakMedId(1111)

        assertTrue(arenaService.harFlereSignifikanteSaker(historikk.saker(), sisteSak))
    }

    @Test
    fun `sakenHarBegyntPåAndreÅretMedUnntak false naar unntak er null`() {
        val mottattDato = LocalDate.of(2026, 6, 10)
        val sisteSak = sakMedId(1234).copy(unntaksvilkaarGjelderFra = null)

        assertFalse(arenaService.sakenHarBegyntPåAndreÅretMedUnntak(mottattDato, sisteSak))
    }

    @Test
    fun `sakenHarBegyntPåAndreÅretMedUnntak true naar unntak er mer enn 18 mnd siden`() {
        val mottattDato = LocalDate.of(2026, 6, 10)
        val unntaksDato = mottattDato.minusYears(2).plusWeeks(hentMaksAntallUkerFørUtløpMaksimalt())
            .minusDays(1) // 19 months + 1 day ago
        val sisteSak = sakMedId(1234).copy(unntaksvilkaarGjelderFra = unntaksDato)

        assertTrue(arenaService.sakenHarBegyntPåAndreÅretMedUnntak(mottattDato, sisteSak))
    }

    @Test
    fun `sakenHarBegyntPåAndreÅretMedUnntak false naar unntak er eksakt 18 mnd siden`() {
        val mottattDato = LocalDate.of(2026, 6, 10)
        val unntaksDato = mottattDato.minusYears(2).plusWeeks(hentMaksAntallUkerFørUtløpMaksimalt()) // exactly at limit
        val sisteSak = sakMedId(1234).copy(unntaksvilkaarGjelderFra = unntaksDato)

        assertFalse(arenaService.sakenHarBegyntPåAndreÅretMedUnntak(mottattDato, sisteSak))
    }

    @Test
    fun `sakenHarBegyntPåAndreÅretMedUnntak false naar unntak er mindre enn 18 mnd siden`() {
        val mottattDato = LocalDate.of(2026, 6, 10)
        val unntaksDato = mottattDato.minusMonths(18).plusDays(1) // 17 months + 29 days ago
        val sisteSak = sakMedId(1234).copy(unntaksvilkaarGjelderFra = unntaksDato)

        assertFalse(arenaService.sakenHarBegyntPåAndreÅretMedUnntak(mottattDato, sisteSak))
    }

    @Test
    fun `sakenHarBegyntPåAndreÅretMedUnntak false naar unntak er i dag`() {
        val mottattDato = LocalDate.of(2026, 6, 10)
        val sisteSak = sakMedId(1234).copy(unntaksvilkaarGjelderFra = mottattDato)

        assertFalse(arenaService.sakenHarBegyntPåAndreÅretMedUnntak(mottattDato, sisteSak))
    }

    @Test
    fun `sakenHarBegyntPåAndreÅretMedUnntak false naar unntak er i fremtiden`() {
        val mottattDato = LocalDate.of(2026, 6, 10)
        val unntaksDato = mottattDato.plusMonths(1)
        val sisteSak = sakMedId(1234).copy(unntaksvilkaarGjelderFra = unntaksDato)

        assertFalse(arenaService.sakenHarBegyntPåAndreÅretMedUnntak(mottattDato, sisteSak))
    }

    @Test
    fun `sakenHarBegyntPåAndreÅretMedUnntak true naar unntak er langt tilbake`() {
        val mottattDato = LocalDate.of(2026, 6, 10)
        val unntaksDato = LocalDate.of(2020, 1, 1)
        val sisteSak = sakMedId(1234).copy(unntaksvilkaarGjelderFra = unntaksDato)

        assertTrue(arenaService.sakenHarBegyntPåAndreÅretMedUnntak(mottattDato, sisteSak))
    }

    private fun arenaVedtak(sakId: Int = 1, rettighetskode: String) = ArenaVedtak(
        sakId,
        "AKTIV",
        null,
        null,
        null,
        rettighetskode,
        null,
        1,
        "AU"
    )

    private fun sakMedId(sakId: Int): SisteSakDetaljer {
        return SisteSakDetaljer(
            sakId = sakId,
            saknummer = "$sakId-25",
            sakStatus = "AKTIV",
            sakRegistrert = LocalDate.of(2025, 1, 1),
            sakAvsluttet = null,
            sisteVedtak = VedtakMedMaksdato(
                vedtakId = sakId,
                aktfaseKode = "INNV",
                vedtaktypeKode = "O",
                vedtakstatuskode = "IVERK",
                fra = LocalDate.of(2025, 1, 1),
                til = LocalDate.of(2026, 1, 1),
                maxdatoOrdinaer = null,
                maxdatoUnntak = null,
                maxdatoAap = LocalDate.of(2026, 12, 12),
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
