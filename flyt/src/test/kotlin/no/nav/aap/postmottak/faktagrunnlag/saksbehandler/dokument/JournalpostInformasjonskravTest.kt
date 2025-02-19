package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksvurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.Tema
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.TemaVurdering
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Dokument
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentMedTittel
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Filtype
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Variant
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Variantformat
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class JournalpostInformasjonskravTest {
    val journalpostService = mockk<JournalpostService>()
    val journalpostRepository = mockk<JournalpostRepository>()
    val saksnummerRepository = mockk<SaksnummerRepository>()
    val avklarTemaRepository = mockk<AvklarTemaRepository>()

    val journalpostInformasjonskrav = JournalpostInformasjonskrav(
        journalpostRepository,
        journalpostService,
        saksnummerRepository,
        avklarTemaRepository
    )


    @Test
    fun `Skal returnere ENDRET hvis saksnummer har blitt endret utenfor systemet`() {
        val journalpostId = JournalpostId(123456)

        val journalpost = genererJournalpost(journalpostId)
        val hentet = genererJournalpost(journalpostId, saksnummer = "1234")

        val behandlingId = BehandlingId(1)
        val flytKontekst = FlytKontekst(journalpostId, behandlingId, TypeBehandling.Journalføring)

        every { journalpostService.hentJournalpost(journalpostId) } returns hentet
        every { journalpostRepository.hentHvisEksisterer(behandlingId) } returns journalpost
        every { journalpostRepository.lagre(hentet) } returns Unit
        every { saksnummerRepository.hentSakVurdering(behandlingId) } returns Saksvurdering("avklart saksnummer")
        every { avklarTemaRepository.hentTemaAvklaring(behandlingId) } returns null

        val actual = journalpostInformasjonskrav.oppdater(flytKontekst)
        assertThat(actual).isEqualTo(Informasjonskrav.Endret.ENDRET)
    }

    @Test
    fun `Skal returner ENDRET hvis tema har blitt endret utenfor systemet`() {
        val journalpostId = JournalpostId(123456)

        val journalpost = genererJournalpost(journalpostId)
        val hentet = genererJournalpost(journalpostId, tema = "nyttTema")

        val behandlingId = BehandlingId(1)
        val flytKontekst = FlytKontekst(journalpostId, behandlingId, TypeBehandling.Journalføring)

        every { journalpostService.hentJournalpost(journalpostId) } returns hentet
        every { journalpostRepository.hentHvisEksisterer(behandlingId) } returns journalpost
        every { journalpostRepository.lagre(hentet) } returns Unit
        every { saksnummerRepository.hentSakVurdering(behandlingId) } returns null
        every { avklarTemaRepository.hentTemaAvklaring(behandlingId) } returns TemaVurdering(true, Tema.AAP)
        val actual = journalpostInformasjonskrav.oppdater(flytKontekst)
        assertThat(actual).isEqualTo(Informasjonskrav.Endret.ENDRET)
    }

    @Test
    fun `Endringer som samsvarer med avklaringer skal returnere IKKE_ENDRET`() {
        val journalpostId = JournalpostId(123456)
        val journalpost = genererJournalpost(journalpostId)
        val hentet = genererJournalpost(journalpostId, saksnummer = "1234", tema = "AAP")

        val behandlingId = BehandlingId(1)
        val flytKontekst = FlytKontekst(journalpostId, behandlingId, TypeBehandling.Journalføring)

        every { journalpostService.hentJournalpost(journalpostId) } returns hentet
        every { journalpostRepository.hentHvisEksisterer(behandlingId) } returns journalpost
        every { journalpostRepository.lagre(hentet) } returns Unit
        every { saksnummerRepository.hentSakVurdering(behandlingId) } returns Saksvurdering("1234")
        every { avklarTemaRepository.hentTemaAvklaring(behandlingId) } returns TemaVurdering(true, Tema.AAP)
        val actual = journalpostInformasjonskrav.oppdater(flytKontekst)
        assertThat(actual).isEqualTo(Informasjonskrav.Endret.IKKE_ENDRET)
    }

    @Test
    fun `Skal ikke returnere IKKE_ENDRET hvis journalposten er journalført, selv om det er andre relevante endringer`() {
        val journalpostId = JournalpostId(123456)

        val journalpost = genererJournalpost(journalpostId)
        val hentet = genererJournalpost(
            journalpostId,
            saksnummer = "1234",
            tema = "Annet tema",
            fagsystem = "Annet system",
            status = Journalstatus.JOURNALFOERT
        )

        val behandlingId = BehandlingId(1)
        val flytKontekst = FlytKontekst(journalpostId, behandlingId, TypeBehandling.Journalføring)

        every { journalpostService.hentJournalpost(journalpostId) } returns hentet
        every { journalpostRepository.hentHvisEksisterer(behandlingId) } returns journalpost
        every { journalpostRepository.lagre(hentet) } returns Unit
        every { saksnummerRepository.hentSakVurdering(behandlingId) } returns Saksvurdering("avklart saksnummer")
        every { avklarTemaRepository.hentTemaAvklaring(behandlingId) } returns TemaVurdering(true, Tema.AAP)

        val actual = journalpostInformasjonskrav.oppdater(flytKontekst)
        assertThat(actual).isEqualTo(Informasjonskrav.Endret.IKKE_ENDRET)
    }


    private fun genererJournalpost(
        journalpostId: JournalpostId,
        person: Person = Person(1, UUID.randomUUID(), listOf(Ident("12345678901"))),
        dokumenter: List<Dokument> = listOf(
            DokumentMedTittel(
                DokumentInfoId("45426854351"),
                "NAV 11.13-05",
                "Dokumenttittel",
                listOf(Variant(Filtype.PDF, Variantformat.ARKIV))
            ),
        ),
        journalførendeEnhet: String? = null,
        tema: String = "AAP",
        status: Journalstatus = Journalstatus.MOTTATT,
        kanal: KanalFraKodeverk = KanalFraKodeverk.UKJENT,
        mottattDato: LocalDate = LocalDate.of(2020, 12, 1),
        fagsystem: String? = null,
        saksnummer: String? = null,
        behandlingstema: String? = null
    ): Journalpost {
        return Journalpost(
            journalpostId = journalpostId,
            person = person,
            dokumenter = dokumenter,
            journalførendeEnhet = journalførendeEnhet,
            tema = tema,
            status = status,
            kanal = kanal,
            mottattDato = mottattDato,
            fagsystem = fagsystem,
            saksnummer = saksnummer,
            behandlingstema = behandlingstema
        )
    }
}