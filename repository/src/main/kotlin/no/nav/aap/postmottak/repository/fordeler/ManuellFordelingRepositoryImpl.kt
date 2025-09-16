package no.nav.aap.postmottak.repository.fordeler

import no.nav.aap.fordeler.ManuellFordelingRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.repository.RepositoryFactory
import no.nav.aap.postmottak.Fagsystem
import no.nav.aap.postmottak.journalpostogbehandling.Ident

class ManuellFordelingRepositoryImpl(
    private val connection: DBConnection,
): ManuellFordelingRepository {
    override fun fordelTilFagsystem(ident: Ident) =
        connection.queryFirstOrNull("select fagsystem from manuell_fordeling where ident = ?") {
            setParams {
                setString(1, ident.identifikator)
            }
            setRowMapper {
                it.getEnum<Fagsystem>("fagsystem")
            }
        }

    companion object: RepositoryFactory<ManuellFordelingRepository> {
        override fun konstruer(connection: DBConnection) = ManuellFordelingRepositoryImpl(connection)
    }
}