package no.nav.aap.postmottak.repository.under18

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.postmottak.faktagrunnlag.register.PersonRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.kontrakt.behandling.Status
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.repository.postgresRepositoryRegistry
import no.nav.aap.postmottak.test.modell.TestPerson
import no.nav.aap.postmottak.test.fakes.TestJournalposter
import no.nav.aap.postmottak.under18.Under18Aldersgruppe
import no.nav.aap.postmottak.under18.Under18Aldersgrunnlag
import no.nav.aap.postmottak.under18.Under18Repository
import no.nav.aap.postmottak.under18.Under18Søknadsgrunnlag
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Test
import java.time.LocalDate

class Under18RepositoryImplTest {
    private val db = TestDataSource()
    private fun nyBehandling(journalpost: JournalpostId) = db.transaction {
        postgresRepositoryRegistry.provider(it).provide<BehandlingRepository>().opprettBehandling(journalpost, TypeBehandling.Under18)
    }

    private fun <T> medRepository(block: Under18Repository.() -> T) = db.transaction {
        postgresRepositoryRegistry.provider(it).provide<Under18Repository>().block()
    }

    @Test
    fun `beholder tidligere aldersgrunnlag når saksbehandler overstyrer alderen`() {
        val id = nyBehandling(JournalpostId(91))
        val pdl = Under18Aldersgrunnlag(LocalDate.of(2008, 12, 1), Under18Aldersgruppe.SAMTYKKE)
        val manuelt = pdl.copy(aldersgruppe = Under18Aldersgruppe.FOR_UNG)
        medRepository {
            lagreAldersgrunnlag(id, pdl)
            lagreAldersgrunnlag(id, manuelt)
            lagreAldersgrunnlag(id, manuelt)
            assertThat(hentAldersgrunnlag(id)).isEqualTo(manuelt)
        }
        val historikk = db.transaction { connection ->
            connection.queryList("""
                SELECT v.aldersgruppe, g.aktiv FROM under18_aldersgrunnlag g
                JOIN under18_aldersvurdering v ON v.id = g.aldersvurdering_id WHERE g.behandling_id = ? ORDER BY g.id
            """.trimIndent()) {
                setParams { setLong(1, id.id) }
                setRowMapper { it.getEnum<Under18Aldersgruppe>("aldersgruppe") to it.getBoolean("aktiv") }
            }
        }
        assertThat(historikk).containsExactly(pdl.aldersgruppe to false, manuelt.aldersgruppe to true)
    }

    @Test
    fun `en ny versjon beholder søknader og tilknytninger fra forrige grunnlag`() {
        val første = JournalpostId(90)
        val id = nyBehandling(første)
        val person = db.transaction {
            postgresRepositoryRegistry.provider(it).provide<PersonRepository>().finnEllerOpprett(TestPerson().identer.toList())
        }
        val tidligere = Under18Søknadsgrunnlag(person.id, setOf(første))
        val ny = tidligere.leggTilSøknad(JournalpostId(20))
        medRepository {
            lagreSøknadsgrunnlag(id, tidligere)
            lagreSøknadsgrunnlag(id, ny)
            assertThat(hentSøknadsgrunnlag(id)).isEqualTo(ny)
            ny.søknader.forEach { assertThat(hentBehandlingForSøknad(it)).isEqualTo(id) }
        }
        assertThatIllegalStateException().isThrownBy { medRepository { lagreSøknadsgrunnlag(id, tidligere) } }
    }

    @Test
    fun `status avgjør hvilken Under18-behandling som kan ta imot neste søknad`() {
        db.transaction {
            val provider = postgresRepositoryRegistry.provider(it)
            val behandlinger = provider.provide<BehandlingRepository>()
            val person = provider.provide<PersonRepository>().finnEllerOpprett(TestPerson().identer.toList())
            fun ny() = TestJournalposter.leggTil {}.tilJournalpost().copy(person = person).let { journalpost ->
                provider.provide<JournalpostRepository>().lagre(journalpost)
                behandlinger.opprettBehandling(journalpost.journalpostId, TypeBehandling.Under18)
            }
            val id = ny()
            behandlinger.oppdaterBehandlingStatus(id, Status.UTREDES)
            assertThat(behandlinger.hentÅpenUnder18Behandling(person.id)).isEqualTo(id)
            behandlinger.oppdaterBehandlingStatus(id, Status.IVERKSETTES)
            assertThat(behandlinger.hentÅpenUnder18Behandling(person.id)).isNull()
            behandlinger.oppdaterBehandlingStatus(id, Status.AVSLUTTET)
            val neste = ny()
            assertThat(behandlinger.hentÅpenUnder18Behandling(person.id, forOppdatering = true)).isEqualTo(neste)
            ny()
            assertThatIllegalStateException().isThrownBy { behandlinger.hentÅpenUnder18Behandling(person.id) }
                .withMessageContaining("flere åpne Under18-behandlinger")
        }
    }
}
