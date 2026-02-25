package no.nav.aap.postmottak.repository.behandling

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Params
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandling
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandlingsreferanse
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingsreferansePathParam
import no.nav.aap.postmottak.journalpostogbehandling.behandling.StegTilstand
import no.nav.aap.postmottak.journalpostogbehandling.flyt.StegStatus
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.kontrakt.behandling.Status
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.kontrakt.steg.StegType
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.*

class BehandlingRepositoryImpl(private val connection: DBConnection) : BehandlingRepository {
    companion object : Factory<BehandlingRepositoryImpl> {
        override fun konstruer(connection: DBConnection): BehandlingRepositoryImpl {
            return BehandlingRepositoryImpl(connection)
        }
    }

    override fun opprettBehandling(journalpostId: JournalpostId, typeBehandling: TypeBehandling): BehandlingId {
        val query = """
            INSERT INTO BEHANDLING (status, type, referanse, journalpost_id)
                 VALUES (?, ?, ?, ?)
            """.trimIndent()
        val behandlingId = connection.executeReturnKey(query) {
            setParams {
                setEnumName(1, Status.OPPRETTET)
                setEnumName(2, typeBehandling)
                setUUID(3, UUID.randomUUID())
                setLong(4, journalpostId.referanse)
            }
        }

        return BehandlingId(behandlingId)
    }

    private data class StegTilstandIntenal(
        val tidspunkt: LocalDateTime,
        val steg: StegType,
        val status: StegStatus,
        val aktiv: Boolean
    )

    private fun mapBehandling(row: Row): Behandling {
        val behandlingId = BehandlingId(row.getLong("id"))
        return Behandling(
            id = behandlingId,
            journalpostId = JournalpostId(row.getLong("journalpost_id")),
            referanse = row.getUUID("referanse").let(::BehandlingsreferansePathParam),
            status = row.getEnum("status"),
            stegHistorikk = row.getStringOrNull("historikk")?.let { s ->
                DefaultJsonMapper.fromJson<List<StegTilstandIntenal>>(s).map {
                    StegTilstand(
                        tidspunkt = it.tidspunkt,
                        stegStatus = it.status,
                        stegType = it.steg,
                        aktiv = it.aktiv
                    )
                }
            }.orEmpty(),
            opprettetTidspunkt = row.getLocalDateTime("OPPRETTET_TID"),
            versjon = row.getLong("versjon"),
            typeBehandling = row.getEnum("type")
        )
    }

    override fun oppdaterBehandlingStatus(
        behandlingId: BehandlingId,
        status: Status
    ) {
        val query = """UPDATE behandling SET status = ? WHERE ID = ?"""

        return connection.execute(query) {
            setParams {
                setEnumName(1, status)
                setLong(2, behandlingId.toLong())
            }
            setResultValidator {
                require(it == 1)
            }
        }
    }

    override fun loggBesøktSteg(behandlingId: BehandlingId, tilstand: StegTilstand) {
        val updateQuery = """
            UPDATE STEG_HISTORIKK set aktiv = false WHERE behandling_id = ? and aktiv = true
        """.trimIndent()

        connection.execute(updateQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }

        val query = """
                INSERT INTO STEG_HISTORIKK (behandling_id, steg, status, aktiv, opprettet_tid) 
                VALUES (?, ?, ?, ?, ?)
                """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setEnumName(2, tilstand.steg())
                setEnumName(3, tilstand.status())
                setBoolean(4, true)
                setLocalDateTime(5, LocalDateTime.now())
            }
        }
    }

    override fun hent(behandlingId: BehandlingId): Behandling {
        val query = """
            $behandlingerQuery
            WHERE b.id = ?
            """.trimIndent()

        return utførHentQuery(query) { setLong(1, behandlingId.toLong()) }
    }

    override fun hent(journalpostId: JournalpostId): Behandling {
        val query = """
            $behandlingerQuery
            WHERE b.journalpost_id = ?
            """.trimIndent()

        return utførHentQuery(query) { setLong(1, journalpostId.referanse) }
    }

    override fun hent(referanse: Behandlingsreferanse): Behandling {
        val query = """
            $behandlingerQuery
            WHERE referanse = ?
            """.trimIndent()

        return utførHentQuery(query) { setUUID(1, referanse.referanse) }
    }

    @Language("PostgreSQL")
    private val behandlingerQuery = """
select b.*, sh.historikk
from behandling b
         left join lateral (
    select jsonb_agg(
                   jsonb_build_object(
                           'steg', steg,
                           'status', status,
                           'tidspunkt', opprettet_tid,
                           'aktiv', aktiv
                   )
                   order by opprettet_tid
           ) as historikk
    from steg_historikk
    where behandling_id = b.id
    ) sh on true
    """.trimIndent()

    override fun hentAlleBehandlingerForSak(saksnummer: JournalpostId): List<Behandling> {
        val query = """$behandlingerQuery WHERE journalpost_id = ?
        """.trimMargin()

        return connection.queryList(query) {
            setParams { setLong(1, saksnummer.referanse) }
            setRowMapper(::mapBehandling)
        }
    }

    override fun hentÅpenJournalføringsbehandling(journalpostId: JournalpostId): Behandling? {
        return connection.queryFirstOrNull(
            """
            $behandlingerQuery WHERE journalpost_id = ? AND
            type = 'Journalføring' AND
            (status = 'OPPRETTET' OR status = 'UTREDES')
        """.trimIndent()
        ) {
            setParams { setLong(1, journalpostId.referanse) }
            setRowMapper(::mapBehandling)
        }
    }

    override fun hentBehandlingerForPerson(person: Person): List<Behandling> {
        return connection.queryList(
            """
                $behandlingerQuery WHERE journalpost_id IN (SELECT journalpost_id FROM journalpost WHERE person_id = ?)
                 and exists(select 1
             from jsonb_array_elements(sh.historikk) elem
             where elem ->> 'status' = 'AVKLARINGSPUNKT')
        """.trimIndent()
        ) {
            setParams { setLong(1, person.id) }
            setRowMapper(::mapBehandling)
        }
    }


    override fun markerSavepoint() {
        connection.markerSavepoint()
    }


    private fun utførHentQuery(query: String, params: Params.() -> Unit): Behandling {
        return connection.queryFirst(query) {
            setParams(params)
            setRowMapper {
                mapBehandling(it)
            }
        }
    }

}
