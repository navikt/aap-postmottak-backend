package no.nav.aap.fordeler.arena

import io.mockk.mockk
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaVedtak
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakMedSisteVedtakOgMaksdato
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SignifikantHistorikkResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.VedtakMedMaksdato
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.postmottak.gateway.ArenaoppslagGateway
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.klient.createGatewayProvider
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
    fun `returnerer false naar listen er tom`() {
        assertFalse(arenaService.harSignifikanteVedtakUtoverTypeAap(emptyList()))
    }

    @Test
    fun `returnerer true naar det er flere rettighetskoder`() {
        val vedtak = listOf(
            arenaVedtak(rettighetskode = "AAP"),
            arenaVedtak(rettighetskode = "AA115")
        )

        assertTrue(arenaService.harSignifikanteVedtakUtoverTypeAap(vedtak))
    }

    @Test
    fun `returnerer true naar rettighetskoder ikke inneholder AAP`() {
        val vedtak = listOf(arenaVedtak(rettighetskode = "KLAGE"))

        assertTrue(arenaService.harSignifikanteVedtakUtoverTypeAap(vedtak))
    }

    @Test
    fun `returnerer true naar det finnes flere rettighetskoder inkludert AAP`() {
        val vedtak = listOf(
            arenaVedtak(rettighetskode = "AAP"),
            arenaVedtak(rettighetskode = "KLAGE")
        )

        assertTrue(arenaService.harSignifikanteVedtakUtoverTypeAap(vedtak))
    }

    @Test
    fun `harFlereSignifikanteSaker returnerer false naar det kun finnes en sak`() {
        val sisteSakId = 1234
        val historikk = SignifikantHistorikkResponse(
            true,
            listOf(arenaVedtak(sakId = sisteSakId, rettighetskode = "AAP"))
        )
        val sisteSak = sakMedId(sisteSakId)

        assertFalse(arenaService.harFlereSignifikanteSaker(historikk, sisteSak))
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

        assertTrue(arenaService.harFlereSignifikanteSaker(historikk, sisteSak))
    }

    @Test
    fun `harFlereSignifikanteSaker returnerer false naar historikk er tomt`() {
        val historikk = SignifikantHistorikkResponse.ingen
        val sisteSak = sakMedId(1234)

        assertFalse(arenaService.harFlereSignifikanteSaker(historikk, sisteSak))
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

        assertTrue(arenaService.harFlereSignifikanteSaker(historikk, sisteSak))
    }

    private fun arenaVedtak(sakId: Int = 1, rettighetskode: String) = ArenaVedtak(
        sakId,
        "AKTIV",
        null,
        null,
        null,
        rettighetskode,
        null
    )

    private fun sakMedId(sakId: Int): SakMedSisteVedtakOgMaksdato {
        return SakMedSisteVedtakOgMaksdato(
            sakId = sakId,
            saknummer = "$sakId-25",
            sakStatus = "AKTIV",
            sakRegistrert = LocalDate.of(2025, 1, 1),
            sakAvsluttet = null,
            unntaksvilkaarGjelderFra = null,
            har_11_12_forlengelse = false,
            utredesForUfor = false,
            ferdigAvklart = false,
            lopendeVedtak = true,
            sisteVedtak = VedtakMedMaksdato(
                vedtakId = sakId,
                aktfaseKode = "INNV",
                vedtaktypeKode = "O",
                fra = LocalDate.of(2025, 1, 1),
                til = LocalDate.of(2026, 1, 1),
                maxdatoOrdinaer = null,
                maxdatoUnntak = null,
                maxdatoAap = LocalDate.of(2026, 12, 12),
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

    override suspend fun maksdatoForSaker(ident: Ident): List<SakMedSisteVedtakOgMaksdato> = emptyList()

    override suspend fun sisteUtbetalingsdatoForPerson(ident: Ident): LocalDate? = null
}
