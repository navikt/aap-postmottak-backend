package no.nav.aap.postmottak.under18

import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.fordeler.InnkommendeJournalpost
import no.nav.aap.fordeler.InnkommendeJournalpostRepository
import no.nav.aap.fordeler.InnkommendeJournalpostStatus
import no.nav.aap.fordeler.Mottakshendelse
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav.Endret
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravGrunnlagImpl
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravInput
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravRepository
import no.nav.aap.postmottak.faktagrunnlag.register.PersonRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandling
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandlingsreferanse
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingsreferansePathParam
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.klient.defaultGatewayProvider
import no.nav.aap.postmottak.test.FakeUnleash
import no.nav.aap.postmottak.test.Fakes
import no.nav.aap.postmottak.test.MockConnection
import no.nav.aap.postmottak.test.fakes.InMemoryAvklaringsbehovRepository
import no.nav.aap.postmottak.test.fakes.TestJournalPost
import no.nav.aap.postmottak.test.fakes.TestJournalposter
import no.nav.aap.postmottak.test.modell.TestPerson
import no.nav.aap.postmottak.test.modell.TestPersoner
import no.nav.aap.postmottak.test.modell.genererIdent
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.behandling.Status
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.postmottak.journalpostogbehandling.behandling.StegTilstand
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

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

internal object InMemoryIdSequence {
    private val behandlingIds = AtomicLong(1_000_000)
    private val personIds = AtomicLong(1_000_000)

    fun nesteBehandlingId() = behandlingIds.incrementAndGet()
    fun nestePersonId() = personIds.incrementAndGet()
}

internal object InMemoryBehandlingRepository : BehandlingRepository {
    private val behandlinger = ConcurrentHashMap<BehandlingId, Behandling>()

    fun lagre(behandling: Behandling) {
        behandlinger[behandling.id] = behandling
    }

    override fun opprettBehandling(
        journalpostId: JournalpostId,
        typeBehandling: TypeBehandling,
    ): BehandlingId = error("Ikke implementert i test-repositoriet")

    override fun hent(behandlingId: BehandlingId) =
        checkNotNull(behandlinger[behandlingId]) { "Fant ikke behandling $behandlingId" }

    override fun hent(referanse: Behandlingsreferanse) =
        behandlinger.values.single { it.referanse.referanse == referanse.referanse }

    override fun hentSisteBehandling(journalpostId: JournalpostId) =
        behandlinger.values.filter { it.journalpostId == journalpostId }.maxOrNull()
            ?: error("Fant ikke behandling for journalpost $journalpostId")

    override fun hentAlleBehandlingerForJournalpost(journalpostId: JournalpostId) =
        behandlinger.values.filter { it.journalpostId == journalpostId }

    override fun hentÅpenJournalføringsbehandling(journalpostId: JournalpostId): Behandling? = null

    override fun hentÅpenUnder18Behandling(personId: Long, forOppdatering: Boolean): BehandlingId? =
        error("Ikke implementert i test-repositoriet")

    override fun hentBehandlingerForPerson(person: Person): List<Behandling> = emptyList()

    override fun markerSavepoint() = Unit

    override fun oppdaterBehandlingStatus(behandlingId: BehandlingId, status: Status) = Unit

    override fun loggBesøktSteg(
        behandlingId: BehandlingId,
        tilstand: StegTilstand,
    ) = Unit
}

internal object InMemoryInformasjonskravRepository : InformasjonskravRepository {
    private val oppdateringer = ConcurrentHashMap<JournalpostId, CopyOnWriteArrayList<InformasjonskravOppdatert>>()

    override fun hentOppdateringer(
        journalpostId: JournalpostId,
        informasjonskrav: List<InformasjonskravNavn>,
    ): List<InformasjonskravOppdatert> =
        oppdateringer[journalpostId].orEmpty()
            .filter { it.navn in informasjonskrav }
            .groupBy { it.navn }
            .values
            .mapNotNull { it.maxByOrNull(InformasjonskravOppdatert::oppdatert) }

    override fun registrerOppdateringer(
        journalpostId: JournalpostId,
        behandlingId: BehandlingId,
        informasjonskrav: Map<InformasjonskravNavn, InformasjonskravInput>,
        oppdatert: Instant,
    ) {
        val lagrede = oppdateringer.computeIfAbsent(journalpostId) { CopyOnWriteArrayList() }
        informasjonskrav.forEach { (navn, input) ->
            lagrede += InformasjonskravOppdatert(
                behandlingId = behandlingId,
                navn = navn,
                oppdatert = oppdatert,
                forrigeInput = DefaultJsonMapper.toJson(input),
            )
        }
    }
}

internal object InMemoryJournalpostRepository : JournalpostRepository {
    private val journalposter = ConcurrentHashMap<JournalpostId, Journalpost>()

    override fun lagre(journalpost: Journalpost) {
        journalposter[journalpost.journalpostId] = journalpost
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId) =
        journalposter[InMemoryBehandlingRepository.hent(behandlingId).journalpostId]

    override fun hentHvisEksisterer(behandlingsreferanse: Behandlingsreferanse) =
        journalposter[InMemoryBehandlingRepository.hent(behandlingsreferanse).journalpostId]

    override fun hentHvisEksisterer(journalpostId: JournalpostId) = journalposter[journalpostId]
}

internal object InMemoryPersonRepository : PersonRepository {
    private val persons = ConcurrentHashMap<Long, Person>()

    fun lagre(person: Person): Person {
        persons[person.id] = person
        return person
    }

    override fun finnEllerOpprett(identer: List<Ident>): Person {
        val eksisterende = persons.values.filter { person -> identer.any(person::er) }.distinctBy { it.identifikator }
        check(eksisterende.size <= 1) { "Identene tilhører flere personer" }
        val person = eksisterende.singleOrNull()
            ?: Person(InMemoryIdSequence.nestePersonId(), UUID.randomUUID(), identer)
        val oppdatert = Person(
            id = person.id,
            identifikator = person.identifikator,
            identer = (person.identer() + identer).distinct(),
        )
        return lagre(oppdatert)
    }

    override fun oppdater(person: Person, identer: List<Ident>) {
        lagre(
            Person(
                id = person.id,
                identifikator = person.identifikator,
                identer = (person.identer() + identer).distinct(),
            )
        )
    }

    override fun hent(identifikator: UUID) =
        persons.values.single { it.identifikator == identifikator }

    override fun hent(personId: Long) =
        checkNotNull(persons[personId]) { "Fant ikke person $personId" }

    override fun finn(ident: Ident) = persons.values.singleOrNull { it.er(ident) }
}

internal object InMemoryInnkommendeJournalpostRepository : InnkommendeJournalpostRepository {
    private data class Mottak(
        val journalpost: InnkommendeJournalpost,
        val id: Long,
        val mottattTid: LocalDateTime,
    )

    private val mottak = ConcurrentHashMap<JournalpostId, Mottak>()
    private val ids = AtomicLong()

    override fun eksisterer(journalpostId: JournalpostId) = mottak.containsKey(journalpostId)

    override fun hent(journalpostId: JournalpostId) =
        checkNotNull(mottak[journalpostId]) { "Fant ikke innkommende journalpost $journalpostId" }.journalpost

    override fun hentId(journalpostId: JournalpostId) =
        checkNotNull(mottak[journalpostId]) { "Fant ikke innkommende journalpost $journalpostId" }.id

    override fun hentHvisEksisterer(journalpostId: JournalpostId) = mottak[journalpostId]?.journalpost

    override fun finn(ident: Ident) =
        mottak.values.filter { it.journalpost.brukerId == ident.identifikator }.map { it.journalpost }

    override fun finnMottakshistorikk(identer: List<Ident>): List<Mottakshendelse> {
        val identifikatorer = identer.mapTo(HashSet()) { it.identifikator }
        return mottak.values
            .filter { it.journalpost.brukerId in identifikatorer }
            .map { Mottakshendelse(it.journalpost.journalpostId, it.mottattTid) }
            .sortedBy { it.mottattTid }
    }

    override fun lagre(innkommendeJournalpost: InnkommendeJournalpost): Long {
        val id = ids.incrementAndGet()
        mottak[innkommendeJournalpost.journalpostId] = Mottak(
            journalpost = innkommendeJournalpost,
            id = id,
            mottattTid = LocalDateTime.now().plusSeconds(1),
        )
        return id
    }

    override fun update(innkommendeJournalpost: InnkommendeJournalpost) {
        val forrige = mottak[innkommendeJournalpost.journalpostId]
        mottak[innkommendeJournalpost.journalpostId] = Mottak(
            journalpost = innkommendeJournalpost,
            id = forrige?.id ?: ids.incrementAndGet(),
            mottattTid = forrige?.mottattTid ?: LocalDateTime.now().plusSeconds(1),
        )
    }
}
