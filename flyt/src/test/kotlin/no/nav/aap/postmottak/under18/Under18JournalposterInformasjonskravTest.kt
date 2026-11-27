package no.nav.aap.postmottak.under18

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.aap.fordeler.InnkommendeJournalpost
import no.nav.aap.fordeler.InnkommendeJournalpostStatus
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav.Endret
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravGrunnlagImpl
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandling
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingsreferansePathParam
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.klient.defaultGatewayProvider
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.postmottak.test.FakeUnleash
import no.nav.aap.postmottak.test.Fakes
import no.nav.aap.postmottak.test.MockConnection
import no.nav.aap.postmottak.test.fakes.InMemoryAvklaringsbehovRepository
import no.nav.aap.postmottak.test.fakes.TestJournalPost
import no.nav.aap.postmottak.test.fakes.TestJournalposter
import no.nav.aap.postmottak.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.postmottak.test.inmemoryrepo.InMemoryIdSequence
import no.nav.aap.postmottak.test.inmemoryrepo.InMemoryInformasjonskravRepository
import no.nav.aap.postmottak.test.inmemoryrepo.InMemoryInnkommendeJournalpostRepository
import no.nav.aap.postmottak.test.inmemoryrepo.InMemoryJournalpostRepository
import no.nav.aap.postmottak.test.inmemoryrepo.InMemoryPersonRepository
import no.nav.aap.postmottak.test.modell.TestPerson
import no.nav.aap.postmottak.test.modell.TestPersoner
import no.nav.aap.postmottak.test.modell.genererIdent
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

@Fakes
class Under18JournalposterInformasjonskravTest {
    private val gatewayProvider = defaultGatewayProvider { register<FakeUnleash>() }

    private val repositoryRegistry = RepositoryRegistry()
        .register<InMemoryBehandlingRepository>()
        .register<InMemoryInformasjonskravRepository>()
        .register<InMemoryJournalpostRepository>()
        .register<InMemoryPersonRepository>()
        .register<InMemoryInnkommendeJournalpostRepository>()
        .register<InMemoryAvklaringsbehovRepository>()

    @Test
    fun `ny kandidat siden forrige kjøring gir endring selv om en annen behandling har lagret journalposten`() {
        val person = TestPersoner.leggTil {}
        val hovedjournalpost = TestJournalposter.leggTil { this.person = person }
        val kandidat = TestJournalposter.leggTil { this.person = person; papirsøknad() }
        val oppsett = opprettOppsett(hovedjournalpost, person)

        assertThat(oppdater(oppsett)).isEqualTo(Endret.IKKE_ENDRET)

        oppsett.registrerMottak(kandidat)
        val journalpost = JournalpostService.konstruer(oppsett.repositoryProvider, gatewayProvider)
            .hentJournalpost(kandidat.journalpostId())
        oppsett.repositoryProvider.provide<JournalpostRepository>().lagre(journalpost)

        assertThat(oppdater(oppsett)).isEqualTo(Endret.ENDRET)
        assertThat(oppdater(oppsett)).isEqualTo(Endret.IKKE_ENDRET)
    }

    @Test
    fun `nye kandidater gir endring og kjente kandidater gir uendret resultat`() {
        val person = TestPersoner.leggTil {}
        val hovedjournalpost = TestJournalposter.leggTil { this.person = person }
        val førsteKandidat = TestJournalposter.leggTil { this.person = person }
        val andreKandidat = TestJournalposter.leggTil { this.person = person }
        val oppsett = opprettOppsett(hovedjournalpost, person)

        oppsett.registrerMottak(førsteKandidat)
        assertThat(oppdater(oppsett)).isEqualTo(Endret.ENDRET)
        assertThat(oppdater(oppsett)).isEqualTo(Endret.IKKE_ENDRET)

        oppsett.registrerMottak(andreKandidat)
        assertThat(oppdater(oppsett)).isEqualTo(Endret.ENDRET)
        assertThat(oppdater(oppsett)).isEqualTo(Endret.IKKE_ENDRET)

        TestJournalposter.oppdater(førsteKandidat.copy(tema = "DAG"))
        assertThat(oppdater(oppsett)).isEqualTo(Endret.IKKE_ENDRET)
        assertThat(InMemoryJournalpostRepository.hentHvisEksisterer(førsteKandidat.journalpostId())).isNull()
        assertThat(InMemoryJournalpostRepository.hentHvisEksisterer(andreKandidat.journalpostId())).isNull()
    }

    @Test
    fun `kandidat fra SAF med endret person gir intern feil`() {
        val person = TestPersoner.leggTil {}
        val hovedjournalpost = TestJournalposter.leggTil { this.person = person }
        val oppsett = opprettOppsett(hovedjournalpost, person)
        val kandidat = TestJournalposter.leggTil { this.person = person }
        oppsett.registrerMottak(kandidat)

        val annenPerson = TestPersoner.leggTil {}
        TestJournalposter.oppdater(kandidat.copy(brukerId = annenPerson.aktivIdent().identifikator))

        assertThatThrownBy { hentData(oppsett, kandidat) }
            .isInstanceOf(IllegalStateException::class.java)
        assertThat(InMemoryJournalpostRepository.hentHvisEksisterer(kandidat.journalpostId())).isNull()
    }

    @Test
    fun `kandidat med ORGNR godtas ikke selv når id er lik søkerens`() {
        val person = TestPersoner.leggTil {}
        val hovedjournalpost = TestJournalposter.leggTil { this.person = person }
        val oppsett = opprettOppsett(hovedjournalpost, person)
        val kandidat = TestJournalposter.leggTil {
            medUtenlandskOrgnr(person.aktivIdent().identifikator)
        }
        oppsett.registrerMottak(kandidat)

        assertThatThrownBy { hentData(oppsett, kandidat) }
            .isInstanceOf(IllegalStateException::class.java)
        assertThat(InMemoryJournalpostRepository.hentHvisEksisterer(kandidat.journalpostId())).isNull()
    }

    @Test
    fun `kandidat på søkerens historiske ident godtas`() {
        val person = TestPerson()
        val historiskIdent = Ident(
            genererIdent(LocalDate.of(1985, 6, 15)).identifikator,
            aktivIdent = false,
        )
        val personMedHistoriskIdent = person.copy(identer = person.identer + historiskIdent)
        TestPersoner.leggTil(personMedHistoriskIdent)
        val hovedjournalpost = TestJournalposter.leggTil { this.person = personMedHistoriskIdent }
        val oppsett = opprettOppsett(hovedjournalpost, personMedHistoriskIdent)
        val kandidat = TestJournalposter.leggTil { fnr = historiskIdent.identifikator }
        oppsett.registrerMottak(kandidat)

        assertThatCode { hentData(oppsett, kandidat) }.doesNotThrowAnyException()
        assertThat(InMemoryJournalpostRepository.hentHvisEksisterer(kandidat.journalpostId())).isNull()
    }

    @Test
    fun `kandidat på søkerens aktive ident godtas`() {
        val person = TestPersoner.leggTil {}
        val hovedjournalpost = TestJournalposter.leggTil { this.person = person }
        val oppsett = opprettOppsett(hovedjournalpost, person)
        val kandidat = TestJournalposter.leggTil { this.person = person }
        oppsett.registrerMottak(kandidat)

        assertThatCode { hentData(oppsett, kandidat) }.doesNotThrowAnyException()
        assertThat(InMemoryJournalpostRepository.hentHvisEksisterer(kandidat.journalpostId())).isNull()
    }

    private data class Oppsett(
        val repositoryProvider: RepositoryProvider,
        val kontekst: FlytKontekst,
        val hovedjournalpostId: JournalpostId,
    ) {
        fun registrerMottak(journalpost: TestJournalPost) {
            InMemoryInnkommendeJournalpostRepository.lagre(
                InnkommendeJournalpost(
                    journalpostId = journalpost.journalpostId(),
                    brevkode = journalpost.brevkode.kode,
                    behandlingstema = null,
                    status = InnkommendeJournalpostStatus.EVALUERT,
                    brukerId = journalpost.brukerId,
                )
            )
        }
    }

    private fun opprettOppsett(hovedjournalpost: TestJournalPost, testPerson: TestPerson): Oppsett {
        val hovedjournalpostId = hovedjournalpost.journalpostId()
        val behandlingId = BehandlingId(InMemoryIdSequence.nesteBehandlingId())
        val person = InMemoryPersonRepository.lagre(
            Person(
                id = InMemoryIdSequence.nestePersonId(),
                identifikator = UUID.randomUUID(),
                identer = testPerson.identer.toList(),
            )
        )
        val journalpost = hovedjournalpost.tilJournalpost().copy(person = person)
        InMemoryJournalpostRepository.lagre(journalpost)

        val behandling = Behandling(
            id = behandlingId,
            journalpostId = hovedjournalpostId,
            opprettetTidspunkt = LocalDateTime.now(),
            referanse = BehandlingsreferansePathParam(UUID.randomUUID()),
            typeBehandling = TypeBehandling.Under18,
        )
        InMemoryBehandlingRepository.lagre(behandling)
        InMemoryAvklaringsbehovRepository.opprett(
            behandlingId = behandlingId,
            definisjon = Definisjon.AVKLAR_UNDER18_SAMTYKKE,
            funnetISteg = StegType.AVKLAR_UNDER18_SAMTYKKE,
        )

        return Oppsett(
            repositoryProvider = repositoryRegistry.provider(MockConnection().toDBConnection()),
            kontekst = FlytKontekst(hovedjournalpostId, behandlingId, TypeBehandling.Under18),
            hovedjournalpostId = hovedjournalpostId,
        )
    }

    private fun oppdater(oppsett: Oppsett): Endret {
        val endrede = InformasjonskravGrunnlagImpl(oppsett.repositoryProvider, gatewayProvider)
            .oppdaterFaktagrunnlagForKravliste(
                listOf(StegType.AVKLAR_UNDER18 to Under18JournalposterInformasjonskrav),
                oppsett.kontekst,
            )
        return if (Under18JournalposterInformasjonskrav in endrede) Endret.ENDRET else Endret.IKKE_ENDRET
    }

    private fun hentData(oppsett: Oppsett, kandidat: TestJournalPost) =
        Under18JournalposterInformasjonskrav.konstruer(oppsett.repositoryProvider, gatewayProvider).let { krav ->
            val input = krav.klargjør(oppsett.kontekst)
            val hovedperson = InMemoryJournalpostRepository.hentHvisEksisterer(oppsett.hovedjournalpostId)!!.person
            assertThat(input.kandidater).contains(kandidat.journalpostId())
            assertThat(input.identer).containsExactlyInAnyOrderElementsOf(hovedperson.identer())
            krav.hentData(input)
        }
}
