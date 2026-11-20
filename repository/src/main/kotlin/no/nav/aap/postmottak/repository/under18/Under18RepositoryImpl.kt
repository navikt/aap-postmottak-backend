package no.nav.aap.postmottak.repository.under18

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.under18.Under18Repository
import no.nav.aap.postmottak.under18.Under18SamtykkeVurdering
import java.time.LocalDateTime

class Under18RepositoryImpl(private val connection: DBConnection) : Under18Repository {
    companion object : Factory<Under18RepositoryImpl> {
        override fun konstruer(connection: DBConnection): Under18RepositoryImpl {
            return Under18RepositoryImpl(connection)
        }
    }

    override fun lagreSøknad(behandlingId: BehandlingId, journalpostId: JournalpostId) {
        connection.execute(
            """
            INSERT INTO UNDER18_SOKNAD (BEHANDLING_ID, KILDE_JOURNALPOST_ID)
            VALUES (?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLong(2, journalpostId.referanse)
            }
        }
    }

    override fun hentSøknader(behandlingId: BehandlingId): List<JournalpostId> =
        connection.queryList(
            """
            SELECT KILDE_JOURNALPOST_ID
            FROM UNDER18_SOKNAD
            WHERE BEHANDLING_ID = ?
            ORDER BY ID
            """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper { row -> JournalpostId(row.getLong("kilde_journalpost_id")) }
        }

    override fun lagreSamtykkevurdering(behandlingId: BehandlingId, vurdering: Under18SamtykkeVurdering) {
        val vurderingId = connection.executeReturnKey(
            """
            INSERT INTO UNDER18_SAMTYKKEVURDERING (
                BEGGE_FORELDRE_SAMTYKKET,
                SAMTYKKE_JOURNALPOST_IDER,
                VENT_OPPRETTET_TID
            )
            VALUES (
                ?,
                ARRAY(SELECT verdi::BIGINT FROM UNNEST(?::TEXT[]) AS verdier(verdi)),
                ?
            )
            """.trimIndent()
        ) {
            setParams {
                setBoolean(1, vurdering.beggeForeldreSamtykket)
                setArray(2, vurdering.samtykkeJournalpostIder.map { it.referanse.toString() })
                setLocalDateTime(3, vurdering.ventOpprettetTid)
            }
        }

        connection.execute(
            """
            UPDATE UNDER18_SAMTYKKE_GRUNNLAG
            SET AKTIV = FALSE
            WHERE BEHANDLING_ID = ? AND AKTIV = TRUE
            """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.id) }
        }

        connection.execute(
            """
            INSERT INTO UNDER18_SAMTYKKE_GRUNNLAG (BEHANDLING_ID, SAMTYKKEVURDERING_ID)
            VALUES (?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLong(2, vurderingId)
            }
        }
    }

    override fun hentSamtykkevurderinger(behandlingId: BehandlingId): List<Under18SamtykkeVurdering> =
        connection.queryList(
            """
            SELECT V.BEGGE_FORELDRE_SAMTYKKET,
                   COALESCE(ARRAY_TO_STRING(V.SAMTYKKE_JOURNALPOST_IDER, ','), '') AS SAMTYKKE_JOURNALPOST_IDER,
                   V.VENT_OPPRETTET_TID
            FROM UNDER18_SAMTYKKE_GRUNNLAG G
            JOIN UNDER18_SAMTYKKEVURDERING V ON V.ID = G.SAMTYKKEVURDERING_ID
            WHERE G.BEHANDLING_ID = ?
            ORDER BY G.ID
            """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper(::mapSamtykkevurdering)
        }

    override fun hentAktivSamtykkevurdering(behandlingId: BehandlingId): Under18SamtykkeVurdering? =
        connection.queryFirstOrNull(
            """
            SELECT V.BEGGE_FORELDRE_SAMTYKKET,
                   COALESCE(ARRAY_TO_STRING(V.SAMTYKKE_JOURNALPOST_IDER, ','), '') AS SAMTYKKE_JOURNALPOST_IDER,
                   V.VENT_OPPRETTET_TID
            FROM UNDER18_SAMTYKKE_GRUNNLAG G
            JOIN UNDER18_SAMTYKKEVURDERING V ON V.ID = G.SAMTYKKEVURDERING_ID
            WHERE G.BEHANDLING_ID = ? AND G.AKTIV = TRUE
            """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper(::mapSamtykkevurdering)
        }

    override fun lagreVentOpprettetTid(behandlingId: BehandlingId, tidspunkt: LocalDateTime) {
        val vurderingId = connection.queryList(
            """
            SELECT SAMTYKKEVURDERING_ID
            FROM UNDER18_SAMTYKKE_GRUNNLAG
            WHERE BEHANDLING_ID = ? AND AKTIV = TRUE
            """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper { row -> row.getLong("samtykkevurdering_id") }
        }.singleOrNull()
            ?: error("Fant ingen aktiv samtykkevurdering for behandling ${behandlingId.id}")

        connection.execute(
            """
            UPDATE UNDER18_SAMTYKKEVURDERING
            SET VENT_OPPRETTET_TID = COALESCE(VENT_OPPRETTET_TID, ?)
            WHERE ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLocalDateTime(1, tidspunkt)
                setLong(2, vurderingId)
            }
        }
    }

    private fun mapSamtykkevurdering(row: Row): Under18SamtykkeVurdering {
        val samtykkeJournalpostIder = row.getString("samtykke_journalpost_ider")
            .takeIf(String::isNotEmpty)
            ?.split(",")
            ?.map { JournalpostId(it.toLong()) }
            .orEmpty()

        return Under18SamtykkeVurdering(
            beggeForeldreSamtykket = row.getBoolean("begge_foreldre_samtykket"),
            samtykkeJournalpostIder = samtykkeJournalpostIder,
            ventOpprettetTid = row.getLocalDateTimeOrNull("vent_opprettet_tid")
        )
    }
}
