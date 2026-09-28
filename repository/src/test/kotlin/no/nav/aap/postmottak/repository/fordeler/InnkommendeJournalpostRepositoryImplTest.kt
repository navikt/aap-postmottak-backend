package no.nav.aap.postmottak.repository.fordeler

import no.nav.aap.fordeler.InnkommendeJournalpost
import no.nav.aap.fordeler.InnkommendeJournalpostStatus
import no.nav.aap.fordeler.Regelresultat
import no.nav.aap.fordeler.ÅrsakTilStatus
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.random.Random

class InnkommendeJournalpostRepositoryImplTest {
    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun beforeEach() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() = dataSource.close()


    @Test
    fun `Kan lagre og hente innkommende journalpost uten regelresulat`() {
        val innkommendeJournalpost = InnkommendeJournalpost(
            journalpostId = JournalpostId(1),
            status = InnkommendeJournalpostStatus.IGNORERT,
            årsakTilStatus = ÅrsakTilStatus.ALLEREDE_JOURNALFØRT,
            behandlingstema = "behandlingstema",
            brevkode = "brevkode",
            brukerId = null,
        )

        dataSource.transaction { connection ->
            val innkommendeJournalpostRepository = InnkommendeJournalpostRepositoryImpl(connection)
            innkommendeJournalpostRepository.lagre(innkommendeJournalpost)
        }
        val hentetInnkommendeJournalpost = dataSource.transaction { connection ->
            val innkommendeJournalpostRepository = InnkommendeJournalpostRepositoryImpl(connection)
            innkommendeJournalpostRepository.hent(innkommendeJournalpost.journalpostId)
        }

        assertThat(hentetInnkommendeJournalpost).isEqualTo(innkommendeJournalpost)

    }


    @Test
    fun `Kan lagre og hente innkommende journalpost med regelresulat`() {
        val journalpostId = JournalpostId(100)
        val innkommendeJournalpost = InnkommendeJournalpost(
            journalpostId = journalpostId,
            status = InnkommendeJournalpostStatus.EVALUERT,
            behandlingstema = "behandlingstema",
            brevkode = "brevkode",
            regelresultat = Regelresultat(
                mapOf(
                    "KelvinSakRegel" to false,
                    "ArenaSakRegel" to false,
                    "ErIkkeReisestønadRegel" to true,
                    "ErIkkeAnkeRegel" to true,
                    "yolo" to true
                ),
                forJournalpost = journalpostId.referanse,
                systemNavn = "KELVIN"
            ),
            brukerId = Random.nextInt().toString(),
        )

        val id = dataSource.transaction { connection ->
            val innkommendeJournalpostRepository = InnkommendeJournalpostRepositoryImpl(connection)
            innkommendeJournalpostRepository.lagre(innkommendeJournalpost)
        }


        val hentetRegel = dataSource.transaction { connection ->
            RegelRepositoryImpl(connection).hentRegelresultat(journalpostId)
        }
        assertThat(hentetRegel).isEqualTo(innkommendeJournalpost.regelresultat)

        val hentetRegelPåId = dataSource.transaction { connection ->
            RegelRepositoryImpl(connection).hentRegelresultat(id, journalpostId)
        }
        assertThat(hentetRegelPåId).isEqualTo(innkommendeJournalpost.regelresultat)

        val hentetInnkommendeJournalpost =
            dataSource.transaction {
                val innkommendeJournalpostRepository = InnkommendeJournalpostRepositoryImpl(it)
                innkommendeJournalpostRepository.hent(innkommendeJournalpost.journalpostId)
            }

        assertThat(hentetInnkommendeJournalpost).isEqualTo(innkommendeJournalpost)
    }

    @Test
    fun `update oppdaterer eksisterende rad og legger til regelresultat uten å opprette ny rad`() {
        val journalpostId = JournalpostId(200)
        val brukerId = Random.nextInt().toString()
        val uten = InnkommendeJournalpost(
            journalpostId = journalpostId,
            status = InnkommendeJournalpostStatus.EVALUERT,
            behandlingstema = "behandlingstema",
            brevkode = "brevkode",
            enhet = "4491",
            brukerId = brukerId,
        )
        val regelresultat = Regelresultat(
            mapOf("KelvinSakRegel" to true, "ArenaSakRegel" to false, "ErIkkeReisestønadRegel" to true, "ErIkkeAnkeRegel" to true),
            forJournalpost = journalpostId.referanse,
            systemNavn = "KELVIN"
        )

        val id = dataSource.transaction { InnkommendeJournalpostRepositoryImpl(it).lagre(uten) }
        dataSource.transaction {
            InnkommendeJournalpostRepositoryImpl(it).update(uten.copy(regelresultat = regelresultat))
        }

        dataSource.transaction { connection ->
            val repo = InnkommendeJournalpostRepositoryImpl(connection)
            assertThat(repo.hentId(journalpostId)).isEqualTo(id)
            assertThat(repo.hent(journalpostId)).isEqualTo(uten.copy(regelresultat = regelresultat))
            assertThat(repo.finn(Ident(brukerId))).hasSize(1)
        }
    }

    @Test
    fun `update erstatter tidligere regelresultat`() {
        val journalpostId = JournalpostId(201)
        fun resultat(kelvin: Boolean) = Regelresultat(
            mapOf("KelvinSakRegel" to kelvin, "ArenaSakRegel" to !kelvin, "ErIkkeReisestønadRegel" to true, "ErIkkeAnkeRegel" to kelvin),
            forJournalpost = journalpostId.referanse,
            systemNavn = if (kelvin) "KELVIN" else "ARENA"
        )
        val opprinnelig = InnkommendeJournalpost(
            journalpostId = journalpostId,
            status = InnkommendeJournalpostStatus.EVALUERT,
            behandlingstema = null,
            brevkode = null,
            regelresultat = resultat(true),
        )
        dataSource.transaction { InnkommendeJournalpostRepositoryImpl(it).lagre(opprinnelig) }
        dataSource.transaction {
            InnkommendeJournalpostRepositoryImpl(it).update(opprinnelig.copy(regelresultat = resultat(false)))
        }

        val hentet = dataSource.transaction { InnkommendeJournalpostRepositoryImpl(it).hent(journalpostId) }
        assertThat(hentet.regelresultat).isEqualTo(resultat(false))
    }

    @Test
    fun `update feiler om raden ikke finnes`() {
        val ikkeLagret = InnkommendeJournalpost(
            journalpostId = JournalpostId(202),
            status = InnkommendeJournalpostStatus.EVALUERT,
            behandlingstema = null,
            brevkode = null,
        )
        assertThatThrownBy {
            dataSource.transaction { InnkommendeJournalpostRepositoryImpl(it).update(ikkeLagret) }
        }.isNotNull()
    }

    @Test
    fun `Kan lese rader med historiske statuser`() {
        listOf(
            InnkommendeJournalpostStatus.VIDERSENDT_TIL_KELVIN,
            InnkommendeJournalpostStatus.VIDERESENDT_TIL_ARENA,
            InnkommendeJournalpostStatus.GOSYS_JFR,
        ).forEachIndexed { i, status ->
            val jp = InnkommendeJournalpost(
                journalpostId = JournalpostId(300L + i),
                status = status,
                behandlingstema = null,
                brevkode = null,
            )
            dataSource.transaction { InnkommendeJournalpostRepositoryImpl(it).lagre(jp) }
            val hentet = dataSource.transaction { InnkommendeJournalpostRepositoryImpl(it).hent(jp.journalpostId) }
            assertThat(hentet.status).isEqualTo(status)
        }
    }

    @Test
    fun `Eksisterer`() {
        val journalpostId = JournalpostId(1)
        
        val brukerId = Random.nextInt().toString()
        val innkommendeJournalpost = InnkommendeJournalpost(
            journalpostId = journalpostId,
            status = InnkommendeJournalpostStatus.EVALUERT,
            behandlingstema = "behandlingstema",
            brevkode = "brevkode",
            regelresultat = Regelresultat(
                mapOf(
                    "KelvinSakRegel" to false,
                    "ArenaSakRegel" to false,
                    "ErIkkeReisestønadRegel" to true,
                    "ErIkkeAnkeRegel" to true,
                    "yolo" to true
                ),
                forJournalpost = journalpostId.referanse,
                systemNavn = "KELVIN"
            ),
            brukerId = brukerId,
        )
        assertFalse(dataSource.transaction { connection ->
            val innkommendeJournalpostRepository = InnkommendeJournalpostRepositoryImpl(connection)
            innkommendeJournalpostRepository.eksisterer(journalpostId)
        })

        assertTrue(dataSource.transaction { connection ->
            val innkommendeJournalpostRepository = InnkommendeJournalpostRepositoryImpl(connection)
            innkommendeJournalpostRepository.lagre(innkommendeJournalpost)
            innkommendeJournalpostRepository.eksisterer(journalpostId)
        })
        
        assertThat(dataSource.transaction { connection ->
            val innkommendeJournalpostRepository = InnkommendeJournalpostRepositoryImpl(connection)
            innkommendeJournalpostRepository.finn(Ident(brukerId))
        }.single()).isEqualTo(innkommendeJournalpost)
    }
}