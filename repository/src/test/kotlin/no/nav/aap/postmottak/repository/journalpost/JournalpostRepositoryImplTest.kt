package no.nav.aap.postmottak.repository.journalpost

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.JournalpostPåSak
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksvurdering
import no.nav.aap.postmottak.gateway.Fagsystem
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.AvsenderMottaker
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Dokument
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Filtype
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Variant
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Variantformat
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.SaksnummerRepositoryImpl
import no.nav.aap.postmottak.repository.person.PersonRepositoryImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class JournalpostRepositoryImplTest {
    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun beforeEach() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() = dataSource.close()

    @Test
    fun `Kan lagre og hente journalpost`() {
        dataSource.transaction { connection ->
            // Setup
            val behandlingRepository = BehandlingRepositoryImpl(connection)

            val person = PersonRepositoryImpl(connection).finnEllerOpprett(listOf(Ident("12345678")))
            val journalpost = genererJournalpost(person)
            val behandlingid =
                behandlingRepository.opprettBehandling(journalpost.journalpostId, TypeBehandling.Journalføring)
            val journalpostRepository = JournalpostRepositoryImpl(connection)

            // Act
            journalpostRepository.lagre(journalpost)

            val hentetJournalpost = journalpostRepository.hentHvisEksisterer(behandlingid)

            assertThat(hentetJournalpost).usingRecursiveComparison().isEqualTo(journalpost)
            assertThat(hentetJournalpost?.dokumenter?.size).isGreaterThan(0)
            assertThat(hentetJournalpost?.erSøknad()).isTrue()
            assertThat(hentetJournalpost?.erDigital()).isTrue()
        }
    }

    @Test
    fun `Henter nyeste journalpost når det er flere på en behandling`() {
        val behandlingid = dataSource.transaction { connection ->
            // Setup
            val behandlingRepository = BehandlingRepositoryImpl(connection)

            val person = PersonRepositoryImpl(connection).finnEllerOpprett(listOf(Ident("12345678")))
            val journalpost = genererJournalpost(person)
            val behandlingid =
                behandlingRepository.opprettBehandling(journalpost.journalpostId, TypeBehandling.Journalføring)
            val journalpostRepository = JournalpostRepositoryImpl(connection)

            // Act
            journalpostRepository.lagre(journalpost)

            behandlingid
        }
        dataSource.transaction { connection ->
            val journalpostRepository = JournalpostRepositoryImpl(connection)
            val person = PersonRepositoryImpl(connection).finnEllerOpprett(listOf(Ident("12345678")))
            journalpostRepository.lagre(genererJournalpost(person, tema = "YOLO"))

            val hentetJournalpost = journalpostRepository.hentHvisEksisterer(behandlingid)

            assertThat(hentetJournalpost?.tema).isEqualTo("YOLO")
        }
    }


    @Test
    fun `henter unike journalposter for sak fra nyeste journalpost eller saksvurdering`() {
        dataSource.transaction { connection ->
            val person = PersonRepositoryImpl(connection).finnEllerOpprett(listOf(Ident("12345678")))
            val journalpostRepository = JournalpostRepositoryImpl(connection)
            val saksnummerRepository = SaksnummerRepositoryImpl(connection)
            val behandlingRepository = BehandlingRepositoryImpl(connection)
            val saksnummer = "saksnummer"
            val annetSaksnummer = "annen-sak"
            val gammeltSaksnummer = "gammel-sak"
            val journalpost = genererJournalpost(person).copy(saksnummer = saksnummer)
            val journalpostUtenSaksnummer = journalpost.copy(journalpostId = JournalpostId(20), saksnummer = null)
            val journalpostPåAnnenSak =
                journalpost.copy(journalpostId = JournalpostId(30), saksnummer = annetSaksnummer)
            val journalpostIdUtenGrunnlag = JournalpostId(40)

            journalpostRepository.lagre(journalpost)
            journalpostRepository.lagre(journalpost)
            journalpostRepository.lagre(journalpostUtenSaksnummer)
            journalpostRepository.lagre(journalpostPåAnnenSak)

            val behandling = behandlingRepository.opprettBehandling(
                journalpostUtenSaksnummer.journalpostId, TypeBehandling.Journalføring
            )
            saksnummerRepository.lagreSakVurdering(behandling, Saksvurdering(gammeltSaksnummer))
            saksnummerRepository.lagreSakVurdering(behandling, Saksvurdering(saksnummer))
            val nyBehandling = behandlingRepository.opprettBehandling(
                journalpostUtenSaksnummer.journalpostId, TypeBehandling.DokumentHåndtering
            )
            saksnummerRepository.kopier(behandling, nyBehandling)

            val utenJournalpost = behandlingRepository.opprettBehandling(
                journalpostIdUtenGrunnlag, TypeBehandling.Journalføring
            )
            saksnummerRepository.lagreSakVurdering(utenJournalpost, Saksvurdering(saksnummer))

            assertThat(saksnummerRepository.hentJournalposterForSak(saksnummer))
                .extracting<JournalpostId> { it.journalpostId }
                .containsExactly(journalpost.journalpostId, journalpostUtenSaksnummer.journalpostId, journalpostIdUtenGrunnlag)
            assertThat(saksnummerRepository.hentJournalposterForSak(gammeltSaksnummer)).isEmpty()
            assertThat(saksnummerRepository.hentJournalposterForSak("ukjent-sak")).isEmpty()
            val journalpostUtenLagredeData = saksnummerRepository.hentJournalposterForSak(saksnummer)
                .single { it.journalpostId == journalpostIdUtenGrunnlag }
            assertThat(journalpostUtenLagredeData)
                .isEqualTo(JournalpostPåSak(journalpostIdUtenGrunnlag, null, null, null, null))

            saksnummerRepository.lagreSakVurdering(nyBehandling, Saksvurdering(annetSaksnummer))
            assertThat(saksnummerRepository.hentJournalposterForSak(saksnummer))
                .extracting<JournalpostId> { it.journalpostId }
                .containsExactly(journalpost.journalpostId, journalpostIdUtenGrunnlag)
            assertThat(saksnummerRepository.hentJournalposterForSak(annetSaksnummer))
                .extracting<JournalpostId> { it.journalpostId }
                .containsExactly(journalpostUtenSaksnummer.journalpostId, journalpostPåAnnenSak.journalpostId)
        }
    }

    @Test
    fun `historiske sakstilknytninger ignoreres og journalpostens saksnummer har forrang`() {
        dataSource.transaction { connection ->
            val person = PersonRepositoryImpl(connection).finnEllerOpprett(listOf(Ident("12345678")))
            val journalpostRepository = JournalpostRepositoryImpl(connection)
            val saksnummerRepository = SaksnummerRepositoryImpl(connection)
            val behandlingRepository = BehandlingRepositoryImpl(connection)
            val opprinneligSaksnummer = "saksnummer"
            val nyttSaksnummer = "ny-sak"
            val journalpost = genererJournalpost(person).copy(saksnummer = opprinneligSaksnummer)
            val oppdatertJournalpost = journalpost.copy(saksnummer = nyttSaksnummer)

            journalpostRepository.lagre(journalpost)
            journalpostRepository.lagre(oppdatertJournalpost)
            val behandling = behandlingRepository.opprettBehandling(journalpost.journalpostId, TypeBehandling.Journalføring)
            saksnummerRepository.lagreSakVurdering(behandling, Saksvurdering(opprinneligSaksnummer))

            assertThat(saksnummerRepository.hentJournalposterForSak(opprinneligSaksnummer)).isEmpty()
            assertThat(saksnummerRepository.hentJournalposterForSak(nyttSaksnummer))
                .extracting<JournalpostId> { it.journalpostId }
                .containsExactly(journalpost.journalpostId)
        }
    }

    @Test
    fun `henter metadata fra siste journalpostversjon`() {
        dataSource.transaction { connection ->
            val person = PersonRepositoryImpl(connection).finnEllerOpprett(listOf(Ident("12345678")))
            val journalpostRepository = JournalpostRepositoryImpl(connection)
            val saksnummerRepository = SaksnummerRepositoryImpl(connection)
            val journalpost = genererJournalpost(person)
            val oppdatertJournalpost = journalpost.copy(
                status = Journalstatus.JOURNALFOERT,
                mottattDato = LocalDate.of(2026, 11, 12),
                kanal = KanalFraKodeverk.NAV_NO,
                tema = "OPP",
            )
            journalpostRepository.lagre(journalpost)
            journalpostRepository.lagre(oppdatertJournalpost)

            assertThat(saksnummerRepository.hentJournalposterForSak(requireNotNull(journalpost.saksnummer)))
                .containsExactly(
                    JournalpostPåSak(
                        journalpostId = oppdatertJournalpost.journalpostId,
                        journalstatus = oppdatertJournalpost.status,
                        mottattDato = oppdatertJournalpost.mottattDato,
                        kanal = oppdatertJournalpost.kanal,
                        tema = oppdatertJournalpost.tema,
                    )
                )
        }
    }

    private fun genererJournalpost(
        person: Person,
        dokumenter: List<Dokument>? = null,
        tema: String = "AAP"
    ) = Journalpost(
        journalpostId = JournalpostId(10),
        person = person,
        journalførendeEnhet = "YOLO",
        tema = tema,
        behandlingstema = null,
        tittel = "Tittel på journalposten",
        status = Journalstatus.MOTTATT,
        mottattDato = LocalDate.of(2021, 1, 1),
        mottattTid = LocalDate.of(2021, 1, 1).atStartOfDay(),
        kanal = KanalFraKodeverk.UKJENT,
        saksnummer = "saksnummer",
        fagsystem = Fagsystem.KELVIN.name,
        avsenderMottaker = AvsenderMottaker("id", "FNR", "Navn"),
        dokumenter = dokumenter ?: listOf(
            Dokument(
                brevkode = "NAV 11-13.05",
                dokumentInfoId = DokumentInfoId("1"),
                tittel = "Søknad om AAP",
                varianter = listOf(
                    Variant(
                        filtype = Filtype.JSON,
                        variantformat = Variantformat.ORIGINAL
                    )
                ),
            ),
            Dokument(
                dokumentInfoId = DokumentInfoId("2"),
                brevkode = "NAV 11-13.05",
                tittel = "Vedlegg til søknad",
                varianter = listOf(
                    Variant(
                        filtype = Filtype.JSON,
                        variantformat = Variantformat.ORIGINAL
                    ),
                    Variant(
                        filtype = Filtype.PDF,
                        variantformat = Variantformat.SLADDET
                    )
                ),
            )
        )
    )
}