package no.nav.aap.postmottak.repository.faktagrunnlag

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravInput
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import java.time.Instant

class InformasjonskravRepositoryImpl(private val connection: DBConnection) : InformasjonskravRepository {
    companion object : Factory<InformasjonskravRepositoryImpl> {
        override fun konstruer(connection: DBConnection): InformasjonskravRepositoryImpl {
            return InformasjonskravRepositoryImpl(connection)
        }
    }

    override fun hentOppdateringer(
        journalpostId: JournalpostId,
        informasjonskrav: List<InformasjonskravNavn>,
    ): List<InformasjonskravOppdatert> {
        return connection.queryList(
            """
            SELECT DISTINCT ON (INFORMASJONSKRAV) BEHANDLING_ID, INFORMASJONSKRAV, OPPDATERT, INFORMASJONSKRAV_INPUT
            FROM INFORMASJONSKRAV_OPPDATERT
            WHERE JOURNALPOST_ID = ? AND INFORMASJONSKRAV = ANY(?::text[])
            ORDER BY INFORMASJONSKRAV, OPPDATERT DESC, ID DESC
            """.trimIndent()
        ) {
            setParams {
                setLong(1, journalpostId.referanse)
                setArray(2, informasjonskrav.map { it.name })
            }
            setRowMapper { row ->
                InformasjonskravOppdatert(
                    behandlingId = BehandlingId(row.getLong("BEHANDLING_ID")),
                    navn = row.getEnum("INFORMASJONSKRAV"),
                    oppdatert = row.getInstant("OPPDATERT"),
                    forrigeInput = row.getStringOrNull("INFORMASJONSKRAV_INPUT"),
                )
            }
        }
    }

    override fun registrerOppdateringer(
        journalpostId: JournalpostId,
        behandlingId: BehandlingId,
        informasjonskrav: Map<InformasjonskravNavn, InformasjonskravInput>,
        oppdatert: Instant,
    ) {
        connection.executeBatch(
            """
            INSERT INTO INFORMASJONSKRAV_OPPDATERT
                (JOURNALPOST_ID, BEHANDLING_ID, INFORMASJONSKRAV, OPPDATERT, INFORMASJONSKRAV_INPUT)
            VALUES (?, ?, ?, ?, ?::jsonb)
            """.trimIndent(), informasjonskrav.entries
        ) {
            setParams { (informasjonskravNavn, input) ->
                setLong(1, journalpostId.referanse)
                setLong(2, behandlingId.id)
                setEnumName(3, informasjonskravNavn)
                setInstant(4, oppdatert)
                setString(5, DefaultJsonMapper.toJson(input))
            }
        }
    }
}
