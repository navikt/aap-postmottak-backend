package no.nav.aap.motor

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.Row
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.SakId

class FlytJobbRepository(private val connection: DBConnection) {
    private val jobbRepository = JobbRepository(connection)

    fun leggTil(jobbInput: JobbInput) {
        jobbRepository.leggTil(jobbInput)
    }


    private fun mapOppgave(row: Row): JobbInput {
        return JobbInput(JobbType.parse(row.getString("type")))
            .medId(row.getLong("id"))
            .medStatus(row.getEnum("status"))
            .forBehandling(
                row.getLongOrNull("sak_id")?.let(::SakId),
                row.getLongOrNull("behandling_id")?.let(::BehandlingId)
            )
            .medAntallFeil(row.getLong("antall_feil"))
    }

    fun hentOppgaveForBehandling(id: BehandlingId): List<JobbInput> {
        val query = """
            SELECT *, (SELECT count(1) FROM JOBB_HISTORIKK h WHERE h.jobb_id = op.id AND h.status = '${JobbStatus.FEILET.name}') as antall_feil
                 FROM JOBB op
                 WHERE op.status IN ('${JobbStatus.KLAR.name}','${JobbStatus.FEILET.name}')
                   AND op.behandling_id = ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, id.toLong())
            }
            setRowMapper { row ->
                mapOppgave(row)
            }
        }
    }
}
