package no.nav.aap.fordeler.arena.jobber

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.aap.fordeler.Enhetsutreder
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentMedTittel
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Filtype
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.JournalpostMedDokumentTitler
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Variant
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Variantformat
import no.nav.aap.postmottak.klient.joark.JoarkClient
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.test.fakes.WithFakes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class AutomatiskJournalføringJobbTest : WithFakes {
    val flytJobbRepositoryMock = mockk<FlytJobbRepository>(relaxed = true)
    val joarkClientMock = mockk<JoarkClient>(relaxed = true)
    val journalpostServiceMock = mockk<JournalpostService>(relaxed = true)
    val enhetsutrederMock = mockk<Enhetsutreder>(relaxed = true)
    val automatiskJournalføringJobb = AutomatiskJournalføringJobbUtfører(
        joarkClientMock,
        flytJobbRepositoryMock,
        journalpostServiceMock,
        enhetsutrederMock
    )
    
    @Test
    fun `Skal opprette manuell journalføirngsjobb dersom automatisk journalføring har feilet 2 ganger`() {
        val journalpostId = JournalpostId(1)
        val journalpost = JournalpostMedDokumentTitler(
            journalpostId = journalpostId,
            dokumenter = listOf(
                DokumentMedTittel(
                    DokumentInfoId("123"),
                    "NAV 11.13-05",
                    "Hoveddokument",
                    listOf(Variant(Filtype.JSON, Variantformat.ORIGINAL))
                )
            ),
            person = Person(1, UUID.randomUUID(), listOf(Ident("123"))),
            journalførendeEnhet = null,
            tema = "AAP",
            status = Journalstatus.MOTTATT,
            kanal = KanalFraKodeverk.NAV_NO,
            mottattDato = LocalDate.now(),
            fagsystem = null,
            saksnummer = null,
            behandlingstema = null
        )


        val jobbKontekst = AutomatiskJournalføringKontekst(
            journalpostId = journalpostId,
            ident = Ident("123"),
            saksnummer = "123",
        )

        val jobbInput = spyk(JobbInput(AutomatiskJournalføringJobbUtfører).medAutomatiskJournalføringKontekst(
            jobbKontekst
        ))

        every { journalpostServiceMock.hentjournalpost(any()) } returns journalpost
        every { enhetsutrederMock.finnJournalføringsenhet(any()) } returns "4491"
        every { jobbInput.antallRetriesForsøkt() } returns 3

        automatiskJournalføringJobb.utfør(jobbInput)

        verify(exactly = 1) {
            flytJobbRepositoryMock.leggTil(withArg {
                assertThat(it.type()).isEqualTo(ManuellJournalføringJobbUtfører.type())
                assertThat(it.getArenaVideresenderKontekst()).isEqualTo(journalpost.opprettArenaVideresenderKontekst("4491"))
            })
        }
    }
}