package no.nav.aap.postmottak.repository.faktagrunnlag


import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering.Digitaliseringsvurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering.DigitaliseringsvurderingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId

class DigitaliseringsvurderingRepositoryImpl(private val connection: DBConnection) :
    DigitaliseringsvurderingRepository {

    companion object : Factory<DigitaliseringsvurderingRepositoryImpl> {
        override fun konstruer(connection: DBConnection): DigitaliseringsvurderingRepositoryImpl {
            return DigitaliseringsvurderingRepositoryImpl(connection)
        }
    }

    override fun lagre(behandlingId: BehandlingId, strukturertDokument: Digitaliseringsvurdering) {
        require(strukturertDokument.kategori == InnsendingType.SØKNAD || strukturertDokument.kategori == InnsendingType.MELDEKORT || strukturertDokument.søknadsdato == null) {
            "Søknadsdato skal ikke være satt for andre innsendingstyper enn SØKNAD eller MELDEKORT"
        }

        val vurderingsId = connection.executeReturnKey(
            """
            INSERT INTO DIGITALISERINGSAVKLARING (KATEGORI, STRUKTURERT_DOKUMENT, SOKNADSDATO) VALUES (?, ?, ?)
        """.trimIndent()
        ) {
            setParams {
                setEnumName(1, strukturertDokument.kategori)
                setString(2, strukturertDokument.strukturertDokument)
                setLocalDate(3, strukturertDokument.søknadsdato)
            }
        }

        connection.execute("""UPDATE DIGITALISERINGSVURDERING_GRUNNLAG SET AKTIV = FALSE WHERE BEHANDLING_ID = ?""") {
            setParams { setLong(1, behandlingId.id) }
        }

        connection.execute(
            """
            INSERT INTO DIGITALISERINGSVURDERING_GRUNNLAG (BEHANDLING_ID, DIGITALISERINGSAVKLARING_ID) VALUES (?, ?)
        """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.id); setLong(2, vurderingsId) }
        }
    }


    override fun hentHvisEksisterer(behandlingId: BehandlingId): Digitaliseringsvurdering? {
        return connection.queryFirstOrNull(
            """
            SELECT * FROM DIGITALISERINGSVURDERING_GRUNNLAG 
            JOIN DIGITALISERINGSAVKLARING ON DIGITALISERINGSAVKLARING.id = DIGITALISERINGSAVKLARING_ID
            WHERE BEHANDLING_ID = ? AND AKTIV
        """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { row ->
                Digitaliseringsvurdering(
                    row.getEnum("Kategori"),
                    row.getStringOrNull("strukturert_dokument"),
                    row.getLocalDateOrNull("soknadsdato")
                )
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        connection.execute(
            """
            INSERT INTO DIGITALISERINGSVURDERING_GRUNNLAG (DIGITALISERINGSAVKLARING_ID, BEHANDLING_ID)
            SELECT DIGITALISERINGSAVKLARING_ID, ? FROM DIGITALISERINGSVURDERING_GRUNNLAG WHERE BEHANDLING_ID = ? AND AKTIV
        """.trimIndent()
        ) {
            setParams {
                setLong(1, tilBehandling.id)
                setLong(2, fraBehandling.id)
            }
        }
    }

}
