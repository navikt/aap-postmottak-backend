package no.nav.aap.postmottak.avklaringsbehov.løser

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.gateway.Factory as GatewayFactory
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.httpklient.exception.VerdiIkkeFunnetException
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.lookup.repository.Factory as RepositoryFactory
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.SYSTEMBRUKER
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.postmottak.avklaringsbehov.løsning.AvklarUnder18SamtykkeLøsning
import no.nav.aap.postmottak.gateway.BrukerIdType
import no.nav.aap.postmottak.gateway.JournalpostGateway
import no.nav.aap.postmottak.gateway.SafJournalpost
import no.nav.aap.postmottak.gateway.SafSak
import no.nav.aap.postmottak.gateway.SafTilknyttetJournalpost
import no.nav.aap.postmottak.faktagrunnlag.register.PersonRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandlingsreferanse
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.klient.defaultGatewayProvider
import no.nav.aap.postmottak.klient.saf.graphql.SafGraphqlClientCredentialsClient
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.test.FakeUnleash
import no.nav.aap.postmottak.test.Fakes
import no.nav.aap.postmottak.test.MockConnection
import no.nav.aap.postmottak.test.fakes.InMemoryAvklaringsbehovRepository
import no.nav.aap.postmottak.test.fakes.IsolertUnder18Repository
import no.nav.aap.postmottak.test.fakes.TestJournalPost
import no.nav.aap.postmottak.test.fakes.TestJournalposter
import no.nav.aap.postmottak.test.modell.TestPerson
import no.nav.aap.postmottak.test.modell.TestPersoner
import no.nav.aap.postmottak.test.modell.genererIdent
import no.nav.aap.postmottak.under18.Under18Repository
import no.nav.aap.postmottak.under18.Under18Aldersgrunnlag
import no.nav.aap.postmottak.under18.Under18Aldersgruppe
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import java.util.UUID

private const val SAF_IKKE_FUNNET_JOURNALPOST_ID = 9_000_000_000L
private const val MANGLER_BRUKER_JOURNALPOST_ID = 9_000_000_001L

@Fakes
class AvklarUnder18SamtykkeLøserTest {
    private val gatewayProvider = defaultGatewayProvider { register<FakeUnleash>() }

    @ParameterizedTest
    @ValueSource(ints = [7, 40])
    fun `manglende samtykke setter på vent i løseren med avkortet frist`(dagerTil18: Long) {
        medOppsett(fødselsdato = LocalDate.now().minusYears(18).plusDays(dagerTil18)) {
            repositoryProvider, _, avklaringsbehovRepository, kontekst, _, _ ->
            løs(repositoryProvider, kontekst, beggeForeldreSamtykket = false)

            val vent = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
                .hentVentepunkter()
                .single()
            val fødselsdato = LocalDate.now().minusYears(18).plusDays(dagerTil18)
            assertThat(vent.definisjon).isEqualTo(Definisjon.VENT_PA_UNDER18_SAMTYKKE)
            assertThat(vent.frist()).isEqualTo(minOf(LocalDate.now().plusWeeks(3), fødselsdato.plusYears(18)))
        }
    }

    @Test
    fun `godkjent samtykke oppretter ikke vent`() {
        medOppsett { repositoryProvider, _, avklaringsbehovRepository, kontekst, _, _ ->
            løs(repositoryProvider, kontekst, beggeForeldreSamtykket = true)

            assertThat(avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId).hentVentepunkter())
                .isEmpty()
        }
    }

    @Test
    fun `godkjent samtykke lagres uten å ta behandlingen av vent direkte`() {
        medOppsett { repositoryProvider, under18Repository, avklaringsbehovRepository, kontekst, _, _ ->
            løs(repositoryProvider, kontekst, beggeForeldreSamtykket = false)
            løs(repositoryProvider, kontekst, beggeForeldreSamtykket = true)

            assertThat(avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId).erSattPåVent()).isTrue()
            assertThat(under18Repository.hentAktivSamtykkevurdering(kontekst.behandlingId)?.beggeForeldreSamtykket)
                .isTrue()
        }
    }

    @Test
    fun `flere gyldige referanser utenfor kandidatlisten og søknadsjournalposten lagres`() {
        medOppsett { repositoryProvider, under18Repository, _, kontekst, person, hovedjournalpost ->
            val annenJournalpost = TestJournalposter.leggTil { this.person = person }

            løs(
                repositoryProvider,
                kontekst,
                beggeForeldreSamtykket = false,
                journalpostIder = listOf(hovedjournalpost.journalpostId, annenJournalpost.journalpostId),
            )

            assertThat(under18Repository.hentAktivSamtykkevurdering(kontekst.behandlingId)?.samtykkeJournalpostIder)
                .containsExactly(
                    JournalpostId(hovedjournalpost.journalpostId),
                    JournalpostId(annenJournalpost.journalpostId),
                )
        }
    }

    @Test
    fun `tom samtykkeliste er gyldig`() {
        medOppsett { repositoryProvider, under18Repository, _, kontekst, _, _ ->
            løs(repositoryProvider, kontekst, beggeForeldreSamtykket = true)

            assertThat(under18Repository.hentAktivSamtykkevurdering(kontekst.behandlingId)?.samtykkeJournalpostIder)
                .isEmpty()
        }
    }

    @Test
    fun `ukjent journalpost blant flere avviser hele løsningen uten å endre vurdering eller vent`() {
        medOppsett { repositoryProvider, under18Repository, avklaringsbehovRepository, kontekst, person, _ ->
            val gyldigJournalpost = TestJournalposter.leggTil { this.person = person }
            løs(repositoryProvider, kontekst, beggeForeldreSamtykket = true)
            val vurderingFørAvvisning = under18Repository.hentAktivSamtykkevurdering(kontekst.behandlingId)
            val antallVurderingerFørAvvisning =
                under18Repository.hentSamtykkevurderinger(kontekst.behandlingId).size
            val gatewayProviderMedIkkeFunnet = defaultGatewayProvider {
                register<FakeUnleash>()
                register<SamtykkeTestJournalpostGateway>()
            }

            assertThatThrownBy {
                løs(
                    repositoryProvider,
                    kontekst,
                    beggeForeldreSamtykket = false,
                    journalpostIder = listOf(gyldigJournalpost.journalpostId, SAF_IKKE_FUNNET_JOURNALPOST_ID),
                    gatewayProvider = gatewayProviderMedIkkeFunnet,
                )
            }.isInstanceOf(UgyldigForespørselException::class.java)

            assertThat(under18Repository.hentAktivSamtykkevurdering(kontekst.behandlingId))
                .isEqualTo(vurderingFørAvvisning)
            assertThat(under18Repository.hentSamtykkevurderinger(kontekst.behandlingId))
                .hasSize(antallVurderingerFørAvvisning)
            assertThat(avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId).hentVentepunkter())
                .isEmpty()
        }
    }

    @Test
    fun `journalpost for en annen person avvises`() {
        medOppsett { repositoryProvider, under18Repository, _, kontekst, _, _ ->
            val annenPerson = TestPersoner.leggTil {}
            val annenPersonsJournalpost = TestJournalposter.leggTil { person = annenPerson }

            assertThatThrownBy {
                løs(
                    repositoryProvider,
                    kontekst,
                    beggeForeldreSamtykket = true,
                    journalpostIder = listOf(annenPersonsJournalpost.journalpostId),
                )
            }.isInstanceOf(UgyldigForespørselException::class.java)
            assertThat(under18Repository.hentAktivSamtykkevurdering(kontekst.behandlingId)).isNull()
        }
    }

    @Test
    fun `orgnr avvises selv når identifikatoren er lik søkerens`() {
        medOppsett { repositoryProvider, under18Repository, _, kontekst, person, _ ->
            val orgnrJournalpost = TestJournalposter.leggTil {
                fnr = person.aktivIdent().identifikator
                brukerType = BrukerIdType.ORGNR
            }

            assertThatThrownBy {
                løs(
                    repositoryProvider,
                    kontekst,
                    beggeForeldreSamtykket = true,
                    journalpostIder = listOf(orgnrJournalpost.journalpostId),
                )
            }.isInstanceOf(UgyldigForespørselException::class.java)
            assertThat(under18Repository.hentAktivSamtykkevurdering(kontekst.behandlingId)).isNull()
        }
    }

    @Test
    fun `journalpost uten bruker avvises`() {
        medOppsett { repositoryProvider, under18Repository, _, kontekst, person, _ ->
            val journalpostUtenBruker = TestJournalposter.leggTil {
                journalpostId = MANGLER_BRUKER_JOURNALPOST_ID
                this.person = person
            }
            val gatewayProviderUtenBruker = defaultGatewayProvider {
                register<FakeUnleash>()
                register<SamtykkeTestJournalpostGateway>()
            }

            assertThatThrownBy {
                løs(
                    repositoryProvider,
                    kontekst,
                    beggeForeldreSamtykket = true,
                    journalpostIder = listOf(journalpostUtenBruker.journalpostId),
                    gatewayProvider = gatewayProviderUtenBruker,
                )
            }.isInstanceOf(UgyldigForespørselException::class.java)
            assertThat(under18Repository.hentAktivSamtykkevurdering(kontekst.behandlingId)).isNull()
        }
    }

    @Test
    fun `historisk ident for samme søker godtas`() {
        val opprinneligPerson = TestPerson()
        val historiskIdent = Ident(
            genererIdent(opprinneligPerson.fødselsdato.toLocalDate().minusYears(1)).identifikator,
            aktivIdent = false,
        )
        val person = opprinneligPerson.copy(identer = opprinneligPerson.identer + historiskIdent)
        TestPersoner.leggTil(person)

        medOppsett(person) { repositoryProvider, under18Repository, _, kontekst, _, _ ->
            val journalpostMedHistoriskIdent = TestJournalposter.leggTil { fnr = historiskIdent.identifikator }

            løs(
                repositoryProvider,
                kontekst,
                beggeForeldreSamtykket = true,
                journalpostIder = listOf(journalpostMedHistoriskIdent.journalpostId),
            )

            assertThat(under18Repository.hentAktivSamtykkevurdering(kontekst.behandlingId)?.samtykkeJournalpostIder)
                .containsExactly(JournalpostId(journalpostMedHistoriskIdent.journalpostId))
        }
    }

    private fun medOppsett(
        person: TestPerson = TestPersoner.leggTil {},
        fødselsdato: LocalDate = LocalDate.now().minusYears(18).plusDays(7),
        blokk: (
            RepositoryProvider,
            IsolertUnder18Repository,
            AvklaringsbehovRepository,
            FlytKontekst,
            TestPerson,
            TestJournalPost,
        ) -> Unit,
    ) {
        val hovedjournalpost = TestJournalposter.leggTil { this.person = person }
        val behandlingId = BehandlingId(UUID.randomUUID().leastSignificantBits)
        val kontekst = FlytKontekst(
            hovedjournalpost.journalpostId(),
            behandlingId,
            TypeBehandling.Under18,
        )
        val repositoryProvider = RepositoryRegistry()
            .register<IsolertUnder18Repository>()
            .register<InMemoryAvklaringsbehovRepository>()
            .register<SamtykkeTestJournalpostRepository>()
            .register<SamtykkeTestPersonRepository>()
            .provider(MockConnection().toDBConnection())
        val under18Repository = repositoryProvider.provide<Under18Repository>()
        check(under18Repository is IsolertUnder18Repository)
        under18Repository.lagreAldersgrunnlag(
            behandlingId, Under18Aldersgrunnlag(fødselsdato, Under18Aldersgruppe.vurder(fødselsdato, LocalDate.now()))
        )
        repositoryProvider.provide<JournalpostRepository>().lagre(
            hovedjournalpost.tilJournalpost().copy(
                person = Person(1, UUID.randomUUID(), person.identer.toList()),
            ),
        )

        blokk(
            repositoryProvider,
            under18Repository,
            repositoryProvider.provide(),
            kontekst,
            person,
            hovedjournalpost,
        )
    }

    private fun løs(
        repositoryProvider: RepositoryProvider,
        kontekst: FlytKontekst,
        beggeForeldreSamtykket: Boolean,
        journalpostIder: List<Long> = emptyList(),
        gatewayProvider: GatewayProvider = this.gatewayProvider,
    ) {
        AvklarUnder18SamtykkeLøser.konstruer(repositoryProvider, gatewayProvider).løs(
            AvklaringsbehovKontekst(SYSTEMBRUKER, kontekst),
            AvklarUnder18SamtykkeLøsning(beggeForeldreSamtykket, journalpostIder),
        )
    }
}

internal class SamtykkeTestJournalpostRepository : JournalpostRepository {
    companion object : RepositoryFactory<SamtykkeTestJournalpostRepository> {
        override fun konstruer(connection: DBConnection) = SamtykkeTestJournalpostRepository()
    }

    private val journalposter = mutableMapOf<JournalpostId, Journalpost>()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): Journalpost? = null

    override fun hentHvisEksisterer(behandlingsreferanse: Behandlingsreferanse): Journalpost? = null

    override fun hentHvisEksisterer(journalpostId: JournalpostId): Journalpost? = journalposter[journalpostId]

    override fun lagre(journalpost: Journalpost) {
        journalposter[journalpost.journalpostId] = journalpost
    }
}

internal class SamtykkeTestPersonRepository : PersonRepository {
    companion object : RepositoryFactory<SamtykkeTestPersonRepository> {
        override fun konstruer(connection: DBConnection) = SamtykkeTestPersonRepository()
    }

    override fun finnEllerOpprett(identer: List<Ident>): Person = error("Ikke brukt i denne testen")

    override fun oppdater(person: Person, identer: List<Ident>) = error("Ikke brukt i denne testen")

    override fun hent(identifikator: UUID): Person = error("Ikke brukt i denne testen")

    override fun hent(personId: Long): Person = error("Ikke brukt i denne testen")

    override fun finn(ident: Ident): Person? = error("Ikke brukt i denne testen")
}

internal class SamtykkeTestJournalpostGateway(
    private val safGateway: JournalpostGateway,
) : JournalpostGateway {
    companion object : GatewayFactory<SamtykkeTestJournalpostGateway> {
        override fun konstruer() =
            SamtykkeTestJournalpostGateway(SafGraphqlClientCredentialsClient.konstruer())
    }

    override fun hentJournalpost(journalpostId: JournalpostId): SafJournalpost =
        when (journalpostId.referanse) {
            SAF_IKKE_FUNNET_JOURNALPOST_ID -> throw VerdiIkkeFunnetException("Fant ingen journalpost.")
            MANGLER_BRUKER_JOURNALPOST_ID -> safGateway.hentJournalpost(journalpostId).copy(bruker = null)
            else -> safGateway.hentJournalpost(journalpostId)
        }

    override fun hentSaker(fnr: String): List<SafSak> = safGateway.hentSaker(fnr)

    override fun hentTilknyttedeJournalposter(
        dokumentInfoId: DokumentInfoId,
    ): List<SafTilknyttetJournalpost> = safGateway.hentTilknyttedeJournalposter(dokumentInfoId)
}
