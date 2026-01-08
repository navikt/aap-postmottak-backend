package no.nav.aap.postmottak.repository.faktagrunnlag

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.json.DefaultJsonMapper.fromJson
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.postmottak.avklaringsbehov.løsning.ForenkletDokument
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksinfo
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksvurdering
import no.nav.aap.postmottak.gateway.AvsenderMottakerDto
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId

class SaksnummerRepositoryImpl(private val connection: DBConnection) : SaksnummerRepository {

    companion object : Factory<SaksnummerRepositoryImpl> {
        override fun konstruer(connection: DBConnection): SaksnummerRepositoryImpl {
            return SaksnummerRepositoryImpl(connection)
        }
    }

    override fun hentKelvinSaker(behandlingId: BehandlingId): List<Saksinfo> =
        connection.queryList(
            """
            SELECT * FROM (
                SELECT * FROM INNHENTEDE_SAKER_FOR_BEHANDLING WHERE BEHANDLING_ID = ?
                ORDER BY OPPRETTET DESC LIMIT 1) sakinfo
            JOIN SAKER_PAA_BEHANDLING ON INNHENTEDE_SAKER_FOR_BEHANDLING_ID = sakinfo.ID

        """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper { row ->
                Saksinfo(
                    row.getString("saksnummer"),
                    row.getPeriode("periode"),
                    row.getBoolean("avslag"),
                    row.getEnumOrNull("resultatkode")
                )
            }
        }


    override fun lagreKelvinSak(
        behandlingId: BehandlingId,
        saksinfo: List<Saksinfo>
    ) {
        val id = connection.executeReturnKey(
            """
            INSERT INTO INNHENTEDE_SAKER_FOR_BEHANDLING (BEHANDLING_ID) VALUES (?)
        """.trimIndent()
        ) { setParams { setLong(1, behandlingId.id) } }

        connection.executeBatch(
            """
            INSERT INTO SAKER_PAA_BEHANDLING (
                INNHENTEDE_SAKER_FOR_BEHANDLING_ID,
                SAKSNUMMER, 
                PERIODE,
                AVSLAG,
                RESULTATKODE) 
                VALUES (?, ?, ?::daterange, ?, ?) 
        """.trimIndent(), saksinfo
        ) {
            setParams {
                setLong(1, id)
                setString(2, it.saksnummer)
                setPeriode(3, it.periode)
                setBoolean(4, it.avslag)
                setEnumName(5, it.resultat)
            }
        }
    }

    override fun lagreSakVurdering(behandlingId: BehandlingId, saksvurdering: Saksvurdering) {
        val avklaringId = connection.executeReturnKey(
            """
            INSERT INTO SAKSNUMMER_AVKLARING (SAKSNUMMER, GENERELL_SAK, OPPRETTET_NY, JOURNALPOSTTITTEL, AVSENDERMOTTAKER, DOKUMENTER) 
            VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent()
        ) {
            setParams {
                setString(1, saksvurdering.saksnummer)
                setBoolean(2, saksvurdering.generellSak)
                setBoolean(3, saksvurdering.opprettetNy)
                setString(4, saksvurdering.journalposttittel)
                setString(5, saksvurdering.avsenderMottaker?.let(DefaultJsonMapper::toJson))
                setString(6, saksvurdering.dokumenter?.let(DefaultJsonMapper::toJson))
            }
        }
        setVurderingInaktiv(behandlingId)

        connection.execute("""INSERT INTO SAKSVURDERING_GRUNNLAG (BEHANDLING_ID, SAKSNUMMER_AVKLARING_ID) VALUES (?, ?)""")
        { setParams { setLong(1, behandlingId.id); setLong(2, avklaringId) } }
    }

    override fun hentSakVurdering(behandlingId: BehandlingId): Saksvurdering? {
        return connection.queryFirstOrNull(
            """SELECT * FROM SAKSVURDERING_GRUNNLAG
            JOIN saksnummer_avklaring ON saksnummer_avklaring.id = SAKSVURDERING_GRUNNLAG.saksnummer_avklaring_id
            WHERE BEHANDLING_ID = ?
            ORDER BY TIDSSTEMPEL DESC LIMIT 1
        """
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { row ->
                Saksvurdering(
                    row.getStringOrNull("SAKSNUMMER"),
                    row.getBoolean("GENERELL_SAK"),
                    row.getBoolean("OPPRETTET_NY"),
                    row.getStringOrNull("JOURNALPOSTTITTEL"),
                    row.getStringOrNull("AVSENDERMOTTAKER")?.let { fromJson<AvsenderMottakerDto>(it) },
                    row.getStringOrNull("DOKUMENTER")?.let { fromJson<List<ForenkletDokument>>(it) }
                )
            }
        }
    }

    private fun setVurderingInaktiv(behandlingId: BehandlingId) {
        connection.execute("""UPDATE SAKSVURDERING_GRUNNLAG SET AKTIV = FALSE WHERE BEHANDLING_ID = ? AND  AKTIV""") {
            setParams {
                setLong(
                    1,
                    behandlingId.id
                )
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        connection.execute(
            """
            INSERT INTO SAKSVURDERING_GRUNNLAG (SAKSNUMMER_AVKLARING_ID, BEHANDLING_ID)
            SELECT SAKSNUMMER_AVKLARING_ID, ? FROM SAKSVURDERING_GRUNNLAG WHERE BEHANDLING_ID = ? AND AKTIV
        """.trimIndent()
        ) {
            setParams {
                setLong(1, tilBehandling.id)
                setLong(2, fraBehandling.id)
            }
        }
    }

    override fun eksistererAvslagPåTidligereBehandling(behandlingId: BehandlingId): Boolean {
        val kelvinSaker = hentKelvinSaker(behandlingId)
        return kelvinSaker.any { it.avslag }
    }
}