package no.nav.aap.postmottak.sakogbehandling.lås

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.sakogbehandling.behandling.Behandlingsreferanse
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class TaSkriveLåsRepository(private val connection: DBConnection) {

    fun lås(behandlingId: BehandlingId): BehandlingSkrivelås {
        val query = """SELECT versjon FROM BEHANDLING WHERE ID = ? FOR UPDATE"""

        return connection.queryFirst(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                BehandlingSkrivelås(behandlingId, it.getLong("versjon"))
            }
        }
    }

    fun lås(referanse: Behandlingsreferanse): BehandlingSkrivelås {
        val query = """SELECT id, versjon FROM BEHANDLING WHERE referanse = ? FOR UPDATE"""

        return connection.queryFirst(query) {
            setParams {
                setUUID(1, referanse.referanse)
            }
            setRowMapper {
                BehandlingSkrivelås(
                    BehandlingId(it.getLong("id")),
                    it.getLong("versjon")
                )
            }
        }
    }

    fun verifiserSkrivelås(skrivelås: BehandlingSkrivelås) {
        val query = """UPDATE behandling SET versjon = ? WHERE ID = ? and versjon = ?"""

        return connection.execute(query) {
            setParams {
                setLong(1, skrivelås.versjon + 1)
                setLong(2, skrivelås.id.toLong())
                setLong(3, skrivelås.versjon)
            }
            setResultValidator {
                require(it == 1)
            }
        }
    }
}
