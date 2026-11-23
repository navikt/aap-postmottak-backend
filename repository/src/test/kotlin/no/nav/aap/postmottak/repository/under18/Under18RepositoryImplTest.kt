package no.nav.aap.postmottak.repository.under18

import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.repository.postgresRepositoryRegistry
import no.nav.aap.postmottak.test.MockConnection
import no.nav.aap.postmottak.under18.Under18Repository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test

class Under18RepositoryImplTest {
    @Test
    fun `dummy er registrert og påvirker ikke kopiering eller sletting i andre flyter`() {
        val repository = postgresRepositoryRegistry.provider(MockConnection().toDBConnection())
            .provide<Under18Repository>()

        assertThat(repository).isInstanceOf(Under18RepositoryImpl::class.java)
        assertThatCode {
            repository.kopier(BehandlingId(1), BehandlingId(2))
            repository.slett(BehandlingId(1))
        }.doesNotThrowAnyException()
    }
}
