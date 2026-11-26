package no.nav.aap.postmottak.repository.under18

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.postmottak.faktagrunnlag.register.PersonRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.repository.postgresRepositoryRegistry
import no.nav.aap.postmottak.test.modell.TestPerson
import no.nav.aap.postmottak.under18.Under18Aldersgruppe
import no.nav.aap.postmottak.under18.Under18Aldersgrunnlag
import no.nav.aap.postmottak.under18.Under18Repository
import no.nav.aap.postmottak.under18.Under18SamtykkeVurdering
import no.nav.aap.postmottak.under18.Under18Søknadsgrunnlag
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Test
import java.sql.SQLException
import java.time.LocalDate
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class Under18RepositoryImplTest {
    private val db = TestDataSource()
    private val person = db.transaction {
        postgresRepositoryRegistry.provider(it).provide<PersonRepository>().finnEllerOpprett(TestPerson().identer.toList()).id
    }

    private fun nyBehandling(journalpost: JournalpostId) = db.transaction {
        postgresRepositoryRegistry.provider(it).provide<BehandlingRepository>().opprettBehandling(journalpost, TypeBehandling.Under18)
    }

    private fun <T> medRepository(block: Under18Repository.() -> T) = db.transaction {
        postgresRepositoryRegistry.provider(it).provide<Under18Repository>().block()
    }

    @Test
    fun `en samtidig journalføring må gjenbruke samlingen som vant`() {
        val søknader = listOf(JournalpostId(90), JournalpostId(20))
        val behandlinger = søknader.map(::nyBehandling)
        val klar = CyclicBarrier(2)
        val vinnere = Executors.newFixedThreadPool(2).use { pool ->
            søknader.indices.map { i ->
                pool.submit<Boolean> {
                    try {
                        medRepository {
                            assertThat(finnBehandlingForNyeSøknader(person)).isNull()
                            klar.await(10, TimeUnit.SECONDS)
                            lagreSøknadsgrunnlag(behandlinger[i], Under18Søknadsgrunnlag(person, listOf(søknader[i])))
                        }
                        true
                    } catch (e: Exception) {
                        check(generateSequence<Throwable>(e) { it.cause }.filterIsInstance<SQLException>().any { it.sqlState == "23505" }) { e }
                        false
                    }
                }
            }.map { it.get(15, TimeUnit.SECONDS) }
        }
        assertThat(vinnere).containsExactlyInAnyOrder(true, false)
        val vinner = vinnere.indexOf(true)
        val id = behandlinger[vinner]
        val søknad = søknader[1 - vinner]
        val tidligere = medRepository { hentSøknadsgrunnlag(id) }
        val ny = tidligere.leggTilSøknad(søknad)
        medRepository { lagreSøknadsgrunnlag(id, ny) }
        assertThatIllegalStateException().isThrownBy {
            medRepository { lagreSøknadsgrunnlag(id, tidligere.stengForNyeSøknader()) }
        }
        medRepository {
            assertThat(hentBehandlingForSøknad(søknad)).isEqualTo(id)
            assertThat(hentSøknadsgrunnlag(id)).isEqualTo(ny)
            lagreSøknadsgrunnlag(id, ny.stengForNyeSøknader())
            assertThat(finnBehandlingForNyeSøknader(person)).isNull()
        }
        assertThatIllegalStateException().isThrownBy { medRepository { lagreSøknadsgrunnlag(id, ny) } }
    }

    @Test
    fun `rollback til savepoint bevarer opprinnelig grunnlag og sletting fjerner personopplysningene`() {
        val journalpost = JournalpostId(31)
        val id = nyBehandling(journalpost)
        val samling = Under18Søknadsgrunnlag(person, listOf(journalpost))
        val alder = Under18Aldersgrunnlag(LocalDate.of(2008, 1, 1), Under18Aldersgruppe.SAMTYKKE)
        val samtykke = Under18SamtykkeVurdering(false, listOf(JournalpostId(32)))
        db.transaction { connection ->
            val repo = postgresRepositoryRegistry.provider(connection).provide<Under18Repository>()
            repo.lagreSøknadsgrunnlag(id, samling)
            repo.lagreAldersgrunnlag(id, alder)
            repo.lagreSamtykkevurdering(id, samtykke)
            connection.execute("SAVEPOINT under18")
            repo.lagreSamtykkevurdering(id, Under18SamtykkeVurdering(true, emptyList()))
            repo.lagreSøknadsgrunnlag(id, samling.stengForNyeSøknader())
            connection.execute("ROLLBACK TO SAVEPOINT under18")
        }
        medRepository {
            assertThat(hentSøknadsgrunnlag(id)).isEqualTo(samling)
            assertThat(hentAldersgrunnlag(id)).isEqualTo(alder)
            assertThat(hentAktivSamtykkevurdering(id)).isEqualTo(samtykke)
            repeat(2) { slett(id) }
            assertThat(hentAldersgrunnlag(id)).isNull()
            assertThat(hentAktivSamtykkevurdering(id)).isNull()
            assertThat(hentBehandlingForSøknad(journalpost)).isNull()
            assertThat(finnBehandlingForNyeSøknader(person)).isNull()
        }
    }
}
