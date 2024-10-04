package no.nav.aap.postmottak.sakogbehandling.behandling.vurdering


import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.postmottak.sakogbehandling.sak.Saksnummer
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class AvklaringRepositoryImpl(private val connection: DBConnection) : AvklaringRepository {
    override fun lagreTeamAvklaring(behandlingId: BehandlingId, vurdering: Boolean) {
        connection.execute(
            """
            INSERT INTO SKAL_TIL_AAP_AVKLARING (BEHANDLING_ID, SKAL_TIL_AAP) VALUES (
            ?, ?)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setBoolean(2, vurdering)
            }
        }
    }

    override fun lagreKategoriseringVurdering(behandlingId: BehandlingId, brevkode: Brevkode) {
        connection.execute(
            """
            INSERT INTO KATEGORIAVKLARING (BEHANDLING_ID, KATEGORI) VALUES (
            ?, ?)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setEnumName(2, brevkode)
            }
        }
    }

    override fun lagreStrukturertDokument(behandlingId: BehandlingId, strukturertDokument: String) {
        connection.execute(
            """
            INSERT INTO DIGITALISERINGSAVKLARING (BEHANDLING_ID, STRUKTURERT_DOKUMENT) VALUES (
            ?, ?)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setString(2, strukturertDokument)
            }
        }
    }

    override fun lagreSakVurdering(behandlingId: BehandlingId, saksvurdering: Saksvurdering) {
        connection.execute(
            """
            INSERT INTO SAKSNUMMER_AVKLARING (BEHANDLING_ID, SAKSNUMMER, OPPRETT_NY, GENERELL_SAK) VALUES (
            ?, ?, ?, ?)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setString(2, saksvurdering.saksnummer)
                setBoolean(3, saksvurdering.opprettNySak)
                setBoolean(4, saksvurdering.generellSak)
            }
        }
    }

    override fun hentTemaAvklaring(behandlingId: BehandlingId): TemaVurdeirng? {
        return connection.queryFirstOrNull(vurderingQuery("SKAL_TIL_AAP_AVKLARING")) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { row -> TemaVurdeirng (
                    row.getBoolean(
                        "skal_til_aap"
                    )
                )
            }
        }
    }

    override fun hentKategoriAvklaring(behandlingId: BehandlingId): KategoriVurdering? {
        return connection.queryFirstOrNull(vurderingQuery("KATEGORIAVKLARING")) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { row ->
                KategoriVurdering(
                    row.getEnum("kategori")
                )
            }
        }
    }

    override fun hentSakAvklaring(behandlingId: BehandlingId): Saksvurdering? {
        return connection.queryFirstOrNull(vurderingQuery("SAKSNUMMER_AVKLARING")) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { row ->
                Saksvurdering(
                    row.getStringOrNull("SAKSNUMMER"),
                    row.getBoolean("OPPRETT_NY"),
                    row.getBoolean("GENERELL_SAK"),
                )
            }
        }
    }

    override fun hentStruktureringsavklaring(behandlingId: BehandlingId): Struktureringsvurdering? {
        return connection.queryFirstOrNull(vurderingQuery("DIGITALISERINGSAVKLARING")) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { row ->
                Struktureringsvurdering(
                    row.getString("strukturert_dokument")
                )
            }
        }
    }

    private fun vurderingQuery(tableName: String) =
        """SELECT * FROM $tableName 
            WHERE BEHANDLING_ID = ?
            ORDER BY TIDSSTEMPEL DESC LIMIT 1
        """.trimMargin()

}