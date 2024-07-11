package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.Row
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.HelseinstitusjonVurdering
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class HelseinstitusjonRepository(private val connection: DBConnection) {

    fun lagre(behandlingId: BehandlingId, helseinstitusjonVurdering: HelseinstitusjonVurdering) {
        setHelseinstitusjonVurderingInaktiv(behandlingId)
        connection.execute("""INSERT INTO HELSEINSTITUSJON_GRUNNLAG 
            (BEHANDLING_ID,
            BEGRUNNELSE,
            FAAR_KOST_OG_LOSJI,
            HAR_FASTE_UTGIFTER,
            FORSOERGER_EKTEFELLE)
            VALUES(?, ?, ?, ?, ?)
        """.trimMargin()) {
            setParams {
                setLong(1, behandlingId.toLong())
                setString(2, helseinstitusjonVurdering.begrunnelse)
                setBoolean(3, helseinstitusjonVurdering.faarFriKostOgLosji)
                setBoolean(4, helseinstitusjonVurdering.harFasteUtgifter)
                setBoolean(5, helseinstitusjonVurdering.forsoergerEktefelle)
            }
        }
    }

    fun setHelseinstitusjonVurderingInaktiv(behandlingId: BehandlingId) {
        hentAktivHelseinstitusjonVurderingHvisEksisterer(behandlingId)
        connection.execute("""
            UPDATE HELSEINSTITUSJON_GRUNNLAG SET AKTIV = FALSE
            WHERE BEHANDLING_ID = ?
        """.trimIndent()) {
            setParams { setLong(1, behandlingId.toLong()) }
        }
    }

    fun hentAktivHelseinstitusjonVurdering(behandlingId: BehandlingId): HelseinstitusjonVurdering {
        return connection.queryFirst(
            """
                SELECT * FROM HELSEINSTITUSJON_GRUNNLAG
                WHERE BEHANDLING_ID = ?
                AND AKTIV = TRUE
            """.trimIndent(), {
                setParams {
                    setLong(1, behandlingId.toLong())
                }
                setRowMapper {
                    mapHelseinstitusjonVurdering(it)
                }
            }
        )
    }
    fun hentAktivHelseinstitusjonVurderingHvisEksisterer(behandlingId: BehandlingId): HelseinstitusjonVurdering? {
        return try {
            hentAktivHelseinstitusjonVurdering(behandlingId)
        } catch (e: NoSuchElementException) { null }
    }

    private fun mapHelseinstitusjonVurdering(row: Row): HelseinstitusjonVurdering {
        return HelseinstitusjonVurdering(
                dokumenterBruktIVurdering = emptyList(),
                begrunnelse = row.getString("BEGRUNNELSE"),
                faarFriKostOgLosji = row.getBoolean("FAAR_KOST_OG_LOSJI"),
                forsoergerEktefelle = row.getBooleanOrNull("FORSOERGER_EKTEFELLE"),
                harFasteUtgifter = row.getBooleanOrNull("HAR_FASTE_UTGIFTER")
                )
    }
}