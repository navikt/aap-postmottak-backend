package no.nav.aap.postmottak.repository.lås

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandlingsreferanse
import no.nav.aap.postmottak.journalpostogbehandling.lås.BehandlingSkrivelås
import no.nav.aap.postmottak.journalpostogbehandling.lås.TaSkriveLåsRepository

class TaSkriveLåsRepositoryImpl(private val connection: DBConnection): TaSkriveLåsRepository {

    companion object : Factory<TaSkriveLåsRepositoryImpl> {
        override fun konstruer(connection: DBConnection): TaSkriveLåsRepositoryImpl {
            return TaSkriveLåsRepositoryImpl(connection)
        }
    }
    
    override fun lås(behandlingId: BehandlingId): BehandlingSkrivelås {
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

    override fun lås(referanse: Behandlingsreferanse): BehandlingSkrivelås {
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

    override fun verifiserSkrivelås(skrivelås: BehandlingSkrivelås) {
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
