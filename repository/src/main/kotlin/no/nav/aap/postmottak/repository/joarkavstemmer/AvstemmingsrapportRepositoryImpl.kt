package no.nav.aap.postmottak.repository.joarkavstemmer

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.postmottak.joarkavstemmer.Avstemmingsrapport
import no.nav.aap.postmottak.joarkavstemmer.AvstemmingsrapportRepository
import no.nav.aap.postmottak.joarkavstemmer.JournalpostAvstemmingsresultat
import java.time.Instant

class AvstemmingsrapportRepositoryImpl(private val connection: DBConnection) : AvstemmingsrapportRepository {
    companion object : Factory<AvstemmingsrapportRepositoryImpl> {
        override fun konstruer(connection: DBConnection) = AvstemmingsrapportRepositoryImpl(connection)
    }

    override fun lagre(rapport: Avstemmingsrapport) {
        val kjøringId = connection.executeReturnKey(
            """
            INSERT INTO JOARK_AVSTEMMING_KJORING (STARTET_TIDSPUNKT, FULLFORT_TIDSPUNKT)
            VALUES (?, ?)
            """.trimIndent()
        ) {
            setParams {
                setInstant(1, rapport.startetTidspunkt)
                setInstant(2, rapport.fullførtTidspunkt)
            }
        }
        connection.executeBatch(
            """
            INSERT INTO JOARK_AVSTEMMING_RESULTAT
                (KJORING_ID, REKKEFOLGE, JOURNALPOST_ID, UTFALL, KANAL, DATO_OPPRETTET)
            VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            rapport.resultater.withIndex().toList()
        ) {
            setParams { (index, resultat) ->
                setLong(1, kjøringId)
                setInt(2, index)
                setLong(3, resultat.journalpostId)
                setEnumName(4, resultat.utfall)
                setString(5, resultat.kanal)
                setLocalDate(6, resultat.datoOpprettet)
            }
        }
    }

    override fun hentSiste(): Avstemmingsrapport? {
        val rader = connection.queryList(
            """
            SELECT k.STARTET_TIDSPUNKT, k.FULLFORT_TIDSPUNKT,
                   r.JOURNALPOST_ID, r.UTFALL, r.KANAL, r.DATO_OPPRETTET
            FROM (SELECT * FROM JOARK_AVSTEMMING_KJORING ORDER BY ID DESC LIMIT 1) k
            LEFT JOIN JOARK_AVSTEMMING_RESULTAT r ON r.KJORING_ID = k.ID
            ORDER BY r.REKKEFOLGE
            """.trimIndent()
        ) {
            setRowMapper { row ->
                RapportRad(
                    startetTidspunkt = row.getInstant("STARTET_TIDSPUNKT"),
                    fullførtTidspunkt = row.getInstant("FULLFORT_TIDSPUNKT"),
                    resultat = row.getLongOrNull("JOURNALPOST_ID")?.let { journalpostId ->
                        JournalpostAvstemmingsresultat(
                            journalpostId = journalpostId,
                            utfall = row.getEnum("UTFALL"),
                            kanal = row.getStringOrNull("KANAL"),
                            datoOpprettet = row.getLocalDateOrNull("DATO_OPPRETTET"),
                        )
                    },
                )
            }
        }
        val første = rader.firstOrNull() ?: return null
        return Avstemmingsrapport(
            startetTidspunkt = første.startetTidspunkt,
            fullførtTidspunkt = første.fullførtTidspunkt,
            resultater = rader.mapNotNull { it.resultat },
        )
    }

    private data class RapportRad(
        val startetTidspunkt: Instant,
        val fullførtTidspunkt: Instant,
        val resultat: JournalpostAvstemmingsresultat?,
    )
}
