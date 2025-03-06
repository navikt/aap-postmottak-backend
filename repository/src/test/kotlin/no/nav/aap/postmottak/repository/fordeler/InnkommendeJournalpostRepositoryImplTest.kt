package no.nav.aap.postmottak.repository.fordeler

import no.nav.aap.fordeler.InnkommendeJournalpost
import no.nav.aap.fordeler.InnkommendeJournalpostRepository
import no.nav.aap.fordeler.InnkommendeJournalpostStatus
import no.nav.aap.fordeler.Regelresultat
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InnkommendeJournalpostRepositoryImplTest {

    @AfterEach
    fun afterEach() {
        InitTestDatabase.dataSource.transaction {
            it.execute("""TRUNCATE regel_evaluering CASCADE""")
            it.execute("TRUNCATE innkommende_journalpost CASCADE")
        }
    }

    @Test
    fun `Kan lagre og hente innkommende journalpost uten regelresulat`() {
        val innkommendeJournalpost = InnkommendeJournalpost(
            journalpostId = JournalpostId(1),
            status = InnkommendeJournalpostStatus.IGNORERT,
            behandlingstema = "behandlingstema",
            brevkode = "brevkode"
        )

        inContext {
            innkommendeJournalpostRepository.lagre(innkommendeJournalpost)

            val hentetInnkommendeJournalpost =
                innkommendeJournalpostRepository.hent(innkommendeJournalpost.journalpostId)

            assertThat(hentetInnkommendeJournalpost).isEqualTo(innkommendeJournalpost)
        }
    }


    @Test
    fun `Kan lagre og hente innkommende journalpost med regelresulat`() {
        val journalpostId = JournalpostId(1)
        val innkommendeJournalpost = InnkommendeJournalpost(
            journalpostId = journalpostId,
            status = InnkommendeJournalpostStatus.EVALUERT,
            behandlingstema = "behandlingstema",
            brevkode = "brevkode",
            regelresultat = Regelresultat(mapOf("yolo" to true))
        )

        InitTestDatabase.dataSource.transaction { connection ->
            val id = InnkommendeJournalpostRepositoryImpl(connection).lagre(innkommendeJournalpost)

            val hentetInnkommendeJournalpost =
                InnkommendeJournalpostRepositoryImpl(connection).hent(innkommendeJournalpost.journalpostId)

            val hentetRegel = RegelRepositoryImpl(connection).hentRegelresultat(journalpostId)
            assertThat(hentetRegel).isEqualTo(innkommendeJournalpost.regelresultat)
            
            val hentetRegelPåId = RegelRepositoryImpl(connection).hentRegelresultat(id)
            assertThat(hentetRegelPåId).isEqualTo(innkommendeJournalpost.regelresultat)

            assertThat(hentetInnkommendeJournalpost).isEqualTo(innkommendeJournalpost)
        }
    }
    
    @Test
    fun `Eksisterer`() {
        val journalpostId = JournalpostId(1)
        val innkommendeJournalpost = InnkommendeJournalpost(
            journalpostId = journalpostId,
            status = InnkommendeJournalpostStatus.EVALUERT,
            behandlingstema = "behandlingstema",
            brevkode = "brevkode",
            regelresultat = Regelresultat(mapOf("yolo" to true))
        )
        InitTestDatabase.dataSource.transaction { connection ->
            val innkommendeJournalpostRepository = InnkommendeJournalpostRepositoryImpl(connection)
            
            assertFalse(innkommendeJournalpostRepository.eksisterer(journalpostId))
            innkommendeJournalpostRepository.lagre(innkommendeJournalpost)
            assertTrue(innkommendeJournalpostRepository.eksisterer(journalpostId))
        }
    }

    private class Context(
        val innkommendeJournalpostRepository: InnkommendeJournalpostRepository,
    )

    private fun <T> inContext(block: Context.() -> T): T {
        return InitTestDatabase.dataSource.transaction {
            val context =
                Context(InnkommendeJournalpostRepositoryImpl(it))
            context.let(block)
        }
    }
}