package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument

import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav
import no.nav.aap.postmottak.faktagrunnlag.register.PersonService
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksvurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.Tema
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.TemaVurdering
import no.nav.aap.postmottak.gateway.Bruker
import no.nav.aap.postmottak.gateway.BrukerIdType
import no.nav.aap.postmottak.gateway.JournalpostGateway
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.gateway.SafDatoType
import no.nav.aap.postmottak.gateway.SafJournalpost
import no.nav.aap.postmottak.gateway.SafRelevantDato
import no.nav.aap.postmottak.gateway.Sak
import no.nav.aap.postmottak.gateway.Sakstype
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class JournalpostInformasjonskravTest {
    private val journalpostRepository = mockk<JournalpostRepository>()
    private val saksnummerRepository = mockk<SaksnummerRepository>()
    private val avklarTemaRepository = mockk<AvklarTemaRepository>()
    private val journalpostGateway = mockk<JournalpostGateway>()
    private val personService = mockk<PersonService>()

    private val person = Person(1, UUID.randomUUID(), listOf(Ident("12345678910")))

    private val journalpostService = JournalpostService(journalpostGateway, personService)

    private val journalpostInformasjonskrav = JournalpostInformasjonskrav(
        journalpostRepository,
        journalpostService,
        saksnummerRepository,
        avklarTemaRepository
    )

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
    }

    @AfterEach
    fun afterEach() {
        checkUnnecessaryStub()
    }

    @Test
    fun `Skal returnere ENDRET hvis saksnummer har blitt endret utenfor systemet`() {
        val journalpostId = JournalpostId(123456)

        val persistert = genererJournalpost(journalpostId)
        val hentet = genererSafJournalpost(journalpostId, saksnummer = "1234")

        val behandlingId = BehandlingId(1)
        val flytKontekst = FlytKontekst(journalpostId, behandlingId, TypeBehandling.Journalføring)

        every { journalpostGateway.hentJournalpost(journalpostId) } returns hentet
        every { personService.finnOgOppdaterPerson(any()) } returns person
        every { journalpostRepository.hentHvisEksisterer(behandlingId) } returns persistert
        every { journalpostRepository.lagre(any()) } just Runs
        every { saksnummerRepository.hentSakVurdering(behandlingId) } returns Saksvurdering("avklart saksnummer")
        every { avklarTemaRepository.hentTemaAvklaring(behandlingId) } returns null

        val actual = journalpostInformasjonskrav.oppdater(flytKontekst)
        assertThat(actual).isEqualTo(Informasjonskrav.Endret.ENDRET)
    }

    @Test
    fun `Skal returner ENDRET hvis tema har blitt endret utenfor systemet`() {
        val journalpostId = JournalpostId(123456)

        val persistert = genererJournalpost(journalpostId)
        val hentet = genererSafJournalpost(journalpostId, tema = "nyttTema")

        val behandlingId = BehandlingId(1)
        val flytKontekst = FlytKontekst(journalpostId, behandlingId, TypeBehandling.Journalføring)

        every { journalpostGateway.hentJournalpost(journalpostId) } returns hentet
        every { personService.finnOgOppdaterPerson(any()) } returns person
        every { journalpostRepository.hentHvisEksisterer(behandlingId) } returns persistert
        every { journalpostRepository.lagre(any()) } just Runs
        every { saksnummerRepository.hentSakVurdering(behandlingId) } returns null
        every { avklarTemaRepository.hentTemaAvklaring(behandlingId) } returns TemaVurdering(true, Tema.AAP)

        val actual = journalpostInformasjonskrav.oppdater(flytKontekst)
        assertThat(actual).isEqualTo(Informasjonskrav.Endret.ENDRET)
    }

    @Test
    fun `Endringer som samsvarer med avklaringer skal returnere IKKE_ENDRET`() {
        val journalpostId = JournalpostId(123456)
        val saksvurdering = Saksvurdering("1234")

        val persistert = genererJournalpost(journalpostId)
        val hentet = genererSafJournalpost(journalpostId, saksnummer = saksvurdering.saksnummer, tema = "AAP")

        val behandlingId = BehandlingId(1)
        val flytKontekst = FlytKontekst(journalpostId, behandlingId, TypeBehandling.Journalføring)

        every { journalpostGateway.hentJournalpost(journalpostId) } returns hentet
        every { personService.finnOgOppdaterPerson(any()) } returns person
        every { journalpostRepository.hentHvisEksisterer(behandlingId) } returns persistert
        every { journalpostRepository.lagre(any()) } just Runs
        every { saksnummerRepository.hentSakVurdering(behandlingId) } returns saksvurdering
        every { avklarTemaRepository.hentTemaAvklaring(behandlingId) } returns TemaVurdering(true, Tema.AAP)

        val actual = journalpostInformasjonskrav.oppdater(flytKontekst)
        assertThat(actual).isEqualTo(Informasjonskrav.Endret.IKKE_ENDRET)
    }

    @Test
    fun `Skal ikke returnere IKKE_ENDRET hvis journalposten er journalført, selv om det er andre relevante endringer`() {
        val journalpostId = JournalpostId(123456)

        val persistert = genererJournalpost(journalpostId)
        val hentet = genererSafJournalpost(
            journalpostId,
            saksnummer = "1234",
            tema = "Annet tema",
            fagsystem = "Annet system",
            journalstatus = Journalstatus.JOURNALFOERT
        )

        val behandlingId = BehandlingId(1)
        val flytKontekst = FlytKontekst(journalpostId, behandlingId, TypeBehandling.Journalføring)

        every { journalpostGateway.hentJournalpost(journalpostId) } returns hentet
        every { personService.finnOgOppdaterPerson(any()) } returns person
        every { journalpostRepository.hentHvisEksisterer(behandlingId) } returns persistert
        every { journalpostRepository.lagre(any()) } just Runs

        val actual = journalpostInformasjonskrav.oppdater(flytKontekst)
        assertThat(actual).isEqualTo(Informasjonskrav.Endret.IKKE_ENDRET)
    }

    @Test
    fun `Skal kaste feil hvis bruker er orgnr og status ikke er journalfoert`() {
        val journalpostId = JournalpostId(123456)

        val hentet = genererSafJournalpost(
            journalpostId,
            Bruker(id = "987654321", BrukerIdType.ORGNR),
            journalstatus = Journalstatus.MOTTATT
        )

        val behandlingId = BehandlingId(1)
        val flytKontekst = FlytKontekst(journalpostId, behandlingId, TypeBehandling.Journalføring)

        every { journalpostGateway.hentJournalpost(journalpostId) } returns hentet
        every { journalpostRepository.hentHvisEksisterer(behandlingId) } returns genererJournalpost(journalpostId)

        assertThrows<IllegalArgumentException> {
            journalpostInformasjonskrav.oppdater(flytKontekst)
        }
    }

    private fun genererJournalpost(
        journalpostId: JournalpostId,
        person: Person = Person(1, UUID.randomUUID(), listOf(Ident("12345678901"))),
        tema: String = "AAP",
        status: Journalstatus = Journalstatus.MOTTATT,
        saksnummer: String? = null,
    ): Journalpost {
        return Journalpost(
            journalpostId = journalpostId,
            person = person,
            avsenderMottaker = null,
            dokumenter = emptyList(),
            journalførendeEnhet = null,
            tema = tema,
            status = status,
            kanal = KanalFraKodeverk.NAV_NO,
            mottattDato = LocalDate.now(),
            mottattTid = null,
            fagsystem = null,
            saksnummer = saksnummer,
            behandlingstema = null,
            tittel = null,
        )
    }

    private fun genererSafJournalpost(
        journalpostId: JournalpostId,
        bruker: Bruker = Bruker(id = "12345678910", BrukerIdType.FNR),
        tema: String = "AAP",
        journalstatus: Journalstatus = Journalstatus.MOTTATT,
        kanal: KanalFraKodeverk = KanalFraKodeverk.UKJENT,
        fagsystem: String? = null,
        saksnummer: String? = null,
    ): SafJournalpost {
        return SafJournalpost(
            journalpostId = journalpostId.referanse,
            bruker = bruker,
            dokumenter = emptyList(),
            tema = tema,
            journalstatus = journalstatus,
            kanal = kanal,
            relevanteDatoer = listOf(SafRelevantDato(LocalDateTime.now(), SafDatoType.DATO_REGISTRERT)),
            sak = if (saksnummer != null || fagsystem != null) Sak(
                fagsakId = saksnummer,
                fagsaksystem = fagsystem,
                sakstype = Sakstype.FAGSAK
            ) else null,
        )
    }
}
