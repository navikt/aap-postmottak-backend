package no.nav.aap.postmottak.sakogbehandling.behandling

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Params
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.behandling.vurdering.AvklaringRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.behandling.vurdering.Vurderinger
import no.nav.aap.verdityper.sakogbehandling.BehandlingId


class DokumentbehandlingRepository(
    private val connection: DBConnection,
    private val journalpostRepository: JournalpostRepository = JournalpostRepositoryImpl(connection)
    ) {

    private val vurderingRepository = AvklaringRepositoryImpl(connection)

    fun hentMedLås(behandlingId: BehandlingId, versjon: Long? = null): Dokumentbehandling {
        val query = """
            WITH params (pId, pVersjon) as (values(?, ?))
            SELECT * FROM BEHANDLING b, params
            WHERE b.id = pId
            AND (b.versjon = pVersjon OR pVersjon is null)
            FOR UPDATE OF b
            """.trimIndent()

        return utførHentQuery(query) { setLong(1, behandlingId.toLong()); setLong(2, versjon) }
    }

    fun hentMedLås(journalpostId: JournalpostId, versjon: Long? = null): Dokumentbehandling {
        val query = """
            WITH params (pId, pVersjon) as (values(?, ?))
            SELECT * FROM BEHANDLING b, params
            WHERE journalpost_id = pId
            AND (b.versjon = pVersjon OR pVersjon is null)
            FOR UPDATE OF b
            """.trimIndent()

        return utførHentQuery(query) { setLong(1, journalpostId.referanse); setLong(2, versjon) }

    }

    fun hent(journalpostId: JournalpostId, versjon: Long? = null): Dokumentbehandling {
        val query = """
            WITH params (pId, pVersjon) as (values(?, ?))
            SELECT * FROM BEHANDLING b, params
            WHERE journalpost_id = pId
            AND (b.versjon = pVersjon OR pVersjon is null)
            """.trimIndent()

        return utførHentQuery(query) { setLong(1, journalpostId.referanse); setLong(2, versjon) }
    }

    private fun utførHentQuery(query: String, params: Params.() -> Unit): Dokumentbehandling {
        return connection.queryFirst(query) {
            setParams(params)
            setRowMapper {
                mapBehandling(it)
            }
        }
    }

    private fun mapBehandling(row: Row): Dokumentbehandling {
        val behandlingId = BehandlingId(row.getLong("id"))
        return Dokumentbehandling(
            id = behandlingId,
            journalpost = journalpostRepository.hentHvisEksisterer(behandlingId) ?: throw IllegalStateException("Behandling mangler journalpost"),
            versjon = row.getLong("versjon"),
            vurderinger = hentVurderingerForBehandling(behandlingId)
        )
    }

    private fun hentVurderingerForBehandling(behandlingId: BehandlingId) = Vurderinger(
        saksvurdering = vurderingRepository.hentSakAvklaring(behandlingId),
        avklarTemaVurdering = vurderingRepository.hentTemaAvklaring(behandlingId),
        kategorivurdering = vurderingRepository.hentKategoriAvklaring(behandlingId),
        struktureringsvurdering = vurderingRepository.hentStruktureringsavklaring(behandlingId)
    )
}
