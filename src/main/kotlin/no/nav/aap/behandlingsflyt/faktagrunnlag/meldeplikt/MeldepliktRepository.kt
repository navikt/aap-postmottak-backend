package no.nav.aap.behandlingsflyt.faktagrunnlag.meldeplikt

import no.nav.aap.behandlingsflyt.avklaringsbehov.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection

class MeldepliktRepository(private val connection: DBConnection) {

    fun hentHvisEksisterer(behandlingId: BehandlingId): MeldepliktGrunnlag? {
        return connection.queryFirstOrNull("SELECT ID FROM MELDEPLIKT_FRITAK_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                val id = row.getLong("ID")
                MeldepliktGrunnlag(
                    id = id,
                    behandlingId = behandlingId,
                    vurderinger = hentFritaksvurderinger(id)
                )
            }
        }
    }

    private fun hentFritaksvurderinger(meldepliktId: Long): List<Fritaksvurdering> {
        return connection.queryList("SELECT PERIODE, BEGRUNNELSE, HAR_FRITAK FROM MELDEPLIKT_FRITAK_VURDERING INNER JOIN MELDEPLIKT_FRITAK ON VURDERING_ID = ID WHERE GRUNNLAG_ID = ?") {
            setParams {
                setLong(1, meldepliktId)
            }
            setRowMapper { row ->
                Fritaksvurdering(
                    periode = row.getPeriode("PERIODE"),
                    begrunnelse = row.getString("BEGRUNNELSE"),
                    harFritak = row.getBoolean("HAR_FRITAK")
                )
            }
        }
    }

    fun lagre(behandlingId: BehandlingId, vurderinger: List<Fritaksvurdering>) {
        val meldepliktGrunnlag = hentHvisEksisterer(behandlingId)

        if (meldepliktGrunnlag?.vurderinger == vurderinger) return

        if (meldepliktGrunnlag != null) {
            deaktiverEksisterende(behandlingId)
        }

        val grunnlagId =
            connection.executeReturnKey("INSERT INTO MELDEPLIKT_FRITAK_GRUNNLAG (BEHANDLING_ID) VALUES (?)") {
                setParams {
                    setLong(1, behandlingId.toLong())
                }
            }

        vurderinger.forEach { vurdering ->
            val vurderingId =
                connection.executeReturnKey("INSERT INTO MELDEPLIKT_FRITAK_VURDERING (PERIODE, BEGRUNNELSE, HAR_FRITAK) VALUES (?::daterange, ?, ?)") {
                    setParams {
                        setPeriode(1, vurdering.periode)
                        setString(2, vurdering.begrunnelse)
                        setBoolean(3, vurdering.harFritak)
                    }
                }
            connection.execute("INSERT INTO MELDEPLIKT_FRITAK (GRUNNLAG_ID, VURDERING_ID) VALUES (?, ?)") {
                setParams {
                    setLong(1, grunnlagId)
                    setLong(2, vurderingId)
                }
            }
        }
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE MELDEPLIKT_FRITAK_GRUNNLAG SET AKTIV = 'FALSE' WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }

    fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val fraId =
            connection.queryList("SELECT VURDERING_ID FROM MELDEPLIKT_FRITAK_GRUNNLAG INNER JOIN MELDEPLIKT_FRITAK ON GRUNNLAG_ID = ID WHERE AKTIV AND BEHANDLING_ID = ?") {
                setParams {
                    setLong(1, fraBehandling.toLong())
                }
                setRowMapper { row ->
                    row.getLong("VURDERING_ID")
                }
            }

        if (fraId.isEmpty()) {
            return
        }

        val grunnlagId =
            connection.executeReturnKey("INSERT INTO MELDEPLIKT_FRITAK_GRUNNLAG (BEHANDLING_ID) VALUES (?)") {
                setParams {
                    setLong(1, tilBehandling.toLong())
                }
            }

        fraId.forEach { vurderingId ->
            connection.execute("INSERT INTO MELDEPLIKT_FRITAK (GRUNNLAG_ID, VURDERING_ID) VALUES (?, ?)") {
                setParams {
                    setLong(1, grunnlagId)
                    setLong(2, vurderingId)
                }
            }
        }
    }
}
