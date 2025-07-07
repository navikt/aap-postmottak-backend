package no.nav.aap.fordeler.regler

import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Ident
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ManueltOppgittBarn
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.OppgitteBarn
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.gateway.DokumentGateway
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.gateway.SafDocumentResponse
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Dokument
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Filtype
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Variant
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Variantformat
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random.Default.nextLong

internal class AlleOppgitteBarnHarIdentRegelTest {

    private val regel = spyk(AlleOppgitteBarnHarIdentRegel())

    private val journalpostService = mockk<JournalpostService>()
    private val dokumentGateway = mockk<DokumentGateway>()

    private val generator = AlleOppgitteBarnHarIdentRegelInputGenerator(journalpostService, dokumentGateway)

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @ParameterizedTest
    @EnumSource(Brevkoder::class, names = ["SØKNAD"], mode = EnumSource.Mode.EXCLUDE)
    fun `Alt som ikke er digital søknad skal ignoreres`(brevkode: Brevkoder) {
        val journalpostId = JournalpostId(nextLong())
        val dokumentId = DokumentInfoId("1")
        val journalpost = opprettJournalpost(
            journalpostId,
            Dokument(dokumentId, brevkode.kode, listOf(Variant(Filtype.JSON, Variantformat.ORIGINAL)))
        )

        every { journalpostService.hentJournalpost(journalpostId) } returns journalpost

        val input = generator.generer(RegelInput(journalpostId.referanse, mockk(), ""))

        val resultat = regel.vurder(input)

        assertTrue(resultat)

        verify {
            regel.vurder(OppgitteBarnRegelInput(null))
            journalpostService.hentJournalpost(journalpostId)
            dokumentGateway wasNot Called
        }
    }

    @Test
    fun `Ingen barn oppgitt`() {
        val journalpostId = JournalpostId(nextLong())
        val dokumentId = DokumentInfoId("1")

        every { journalpostService.hentJournalpost(journalpostId) } returns opprettJournalpost(
            journalpostId,
            Dokument(dokumentId, Brevkoder.SØKNAD.kode, listOf(Variant(Filtype.JSON, Variantformat.ORIGINAL)))
        )
        every { dokumentGateway.hentDokument(journalpostId, dokumentId) } returns opprettSafResponse(null)

        val input = generator.generer(RegelInput(journalpostId.referanse, mockk(), ""))

        val resultat = regel.vurder(input)

        assertTrue(resultat)

        verify {
            journalpostService.hentJournalpost(journalpostId)
            dokumentGateway.hentDokument(journalpostId, dokumentId)
            regel.vurder(OppgitteBarnRegelInput(null))
        }
    }

    @Test
    fun `Alle oppgitte barn har ident`() {
        val journalpostId = JournalpostId(nextLong())
        val dokumentId = DokumentInfoId("1")
        val oppgitteBarn = OppgitteBarn(
            emptySet(), listOf(
                ManueltOppgittBarn(ident = Ident("33221101234")),
                ManueltOppgittBarn(navn = "navn", ident = Ident("33221101234")),
                ManueltOppgittBarn(ident = Ident("33221101234"), fødselsdato = LocalDate.now()),
            )
        )

        every { journalpostService.hentJournalpost(journalpostId) } returns opprettJournalpost(
            journalpostId,
            Dokument(dokumentId, Brevkoder.SØKNAD.kode, listOf(Variant(Filtype.JSON, Variantformat.ORIGINAL)))
        )
        every { dokumentGateway.hentDokument(journalpostId, dokumentId) } returns opprettSafResponse(oppgitteBarn)

        val input = generator.generer(RegelInput(journalpostId.referanse, mockk(), ""))

        val resultat = regel.vurder(input)

        assertTrue(resultat)

        verify {
            journalpostService.hentJournalpost(journalpostId)
            dokumentGateway.hentDokument(journalpostId, dokumentId)
            regel.vurder(OppgitteBarnRegelInput(oppgitteBarn))
        }
    }

    @Test
    fun `Finnes barn uten ident`() {
        val journalpostId = JournalpostId(nextLong())
        val dokumentId = DokumentInfoId("1")

        val oppgitteBarn = OppgitteBarn(
            emptySet(), listOf(
                ManueltOppgittBarn(ident = Ident("33221101234")),
                ManueltOppgittBarn(navn = "navn", ident = Ident("33221101234")),
                ManueltOppgittBarn(fødselsdato = LocalDate.now()), // Mangler ident
            )
        )

        every { journalpostService.hentJournalpost(journalpostId) } returns opprettJournalpost(
            journalpostId,
            Dokument(dokumentId, Brevkoder.SØKNAD.kode, listOf(Variant(Filtype.JSON, Variantformat.ORIGINAL)))
        )
        every { dokumentGateway.hentDokument(journalpostId, dokumentId) } returns opprettSafResponse(oppgitteBarn)

        val input = generator.generer(RegelInput(journalpostId.referanse, mockk(), ""))
        val resultat = regel.vurder(input)

        assertFalse(resultat)

        verify {
            journalpostService.hentJournalpost(journalpostId)
            dokumentGateway.hentDokument(journalpostId, dokumentId)
            regel.vurder(OppgitteBarnRegelInput(oppgitteBarn))
        }
    }

    private fun opprettJournalpost(
        journalpostId: JournalpostId,
        dokument: Dokument? = null
    ) =
        Journalpost(
            journalpostId = journalpostId,
            person = Person(nextLong(), UUID.randomUUID(), emptyList()),
            journalførendeEnhet = null,
            tema = "AAP",
            behandlingstema = null,
            status = Journalstatus.JOURNALFOERT,
            mottattDato = LocalDate.now(),
            mottattTid = LocalDateTime.now(),
            dokumenter = listOfNotNull(
                dokument,
                Dokument(
                    dokumentInfoId = DokumentInfoId("2"),
                    brevkode = Brevkoder.ANNEN.kode,
                    varianter = listOf(Variant(Filtype.PDF, Variantformat.ARKIV))
                ),
            ),
            kanal = KanalFraKodeverk.NAV_NO,
            saksnummer = null,
            fagsystem = null
        )

    private fun opprettSafResponse(oppgitteBarn: OppgitteBarn? = null): SafDocumentResponse {
        val melding = SøknadV0(
            student = null,
            yrkesskade = "",
            oppgitteBarn = oppgitteBarn,
            medlemskap = null
        )

        return SafDocumentResponse(
            DefaultJsonMapper.toJson(melding).toByteArray().inputStream(),
            "contentType",
            "filnavn"
        )
    }
}
