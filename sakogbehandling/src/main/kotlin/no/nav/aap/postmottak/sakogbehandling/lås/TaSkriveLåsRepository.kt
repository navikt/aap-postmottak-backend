package no.nav.aap.postmottak.sakogbehandling.lås

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.sakogbehandling.sak.Saksnummer
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.SakId

class TaSkriveLåsRepository(private val connection: DBConnection) {

    fun lås(behandlingId: BehandlingId): Skrivelås {
        val behandling = låsBehandling(behandlingId)
        return Skrivelås(behandling)
    }

    fun låsBehandling(behandlingId: BehandlingId): BehandlingSkrivelås {
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

    fun lås(behandlingId: Long): Skrivelås {
        val query = """SELECT id, versjon FROM BEHANDLING WHERE id = ? FOR UPDATE"""

        return connection.queryFirst(query) {
            setParams {
                setLong(1, behandlingId)
            }
            setRowMapper {
                Skrivelås(
                    BehandlingSkrivelås(
                        BehandlingId(it.getLong("id")),
                        it.getLong("versjon")
                    )
                )
            }
        }
    }

    fun verifiserSkrivelås(skrivelås: Skrivelås) {
        verifiserSkrivelås(skrivelås.behandlingSkrivelås)
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
