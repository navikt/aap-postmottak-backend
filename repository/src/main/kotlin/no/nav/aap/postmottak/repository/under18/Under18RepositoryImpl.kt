package no.nav.aap.postmottak.repository.under18

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.under18.Under18Aldersgrunnlag
import no.nav.aap.postmottak.under18.Under18Repository
import no.nav.aap.postmottak.under18.Under18SamtykkeVurdering
import no.nav.aap.postmottak.under18.Under18Søknadsavklaring
import no.nav.aap.postmottak.under18.Under18Søknadsgrunnlag

class Under18RepositoryImpl(private val connection: DBConnection) : Under18Repository {
    companion object : Factory<Under18RepositoryImpl> {
        override fun konstruer(connection: DBConnection) = Under18RepositoryImpl(connection)
    }

    override fun finnBehandlingForNyeSøknader(personId: Long): BehandlingId? =
        connection.queryFirstOrNull("SELECT behandling_id FROM under18_samling WHERE person_id = ? AND tar_imot") {
            setParams { setLong(1, personId) }
            setRowMapper { BehandlingId(it.getLong("behandling_id")) }
        }

    override fun hentBehandlingForSøknad(journalpostId: JournalpostId): BehandlingId? =
        connection.queryFirstOrNull("SELECT behandling_id FROM under18_soknad WHERE journalpost_id = ?") {
            setParams { setLong(1, journalpostId.referanse) }
            setRowMapper { BehandlingId(it.getLong("behandling_id")) }
        }

    override fun lagreSøknadsgrunnlag(behandlingId: BehandlingId, grunnlag: Under18Søknadsgrunnlag) {
        // Lås samlingen før vi leser søknadene. En samtidig utvidelse eller stenging må ikke overskrives.
        val finnes = connection.queryFirstOrNull("SELECT behandling_id FROM under18_samling WHERE behandling_id = ? FOR UPDATE") {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper { it.getLong("behandling_id") }
        } != null
        val antallLagret = if (finnes) {
            val tidligere = hentSøknadsgrunnlag(behandlingId)
            if (tidligere == grunnlag) return
            check(tidligere.tarImotNyeSøknader) { "$behandlingId er stengt for nye søknader" }
            check(tidligere.personId == grunnlag.personId) { "Kan ikke bytte person på $behandlingId" }
            check(grunnlag.søknader.take(tidligere.søknader.size) == tidligere.søknader) {
                "Lagrede søknader på $behandlingId kan ikke erstattes"
            }
            if (!grunnlag.tarImotNyeSøknader) {
                connection.execute("UPDATE under18_samling SET tar_imot = FALSE WHERE behandling_id = ?") {
                    setParams { setLong(1, behandlingId.id) }
                }
            }
            tidligere.søknader.size
        } else {
            connection.execute("INSERT INTO under18_samling (behandling_id, person_id, tar_imot) VALUES (?, ?, ?)") {
                setParams {
                    setLong(1, behandlingId.id)
                    setLong(2, grunnlag.personId)
                    setBoolean(3, grunnlag.tarImotNyeSøknader)
                }
            }
            0
        }
        connection.executeBatch(
            "INSERT INTO under18_soknad (behandling_id, posisjon, journalpost_id) VALUES (?, ?, ?)",
            grunnlag.søknader.withIndex().drop(antallLagret)
        ) {
            setParams { (posisjon, journalpostId) ->
                setLong(1, behandlingId.id)
                setInt(2, posisjon)
                setLong(3, journalpostId.referanse)
            }
        }
    }

    override fun hentSøknadsgrunnlag(behandlingId: BehandlingId): Under18Søknadsgrunnlag =
        checkNotNull(connection.queryFirstOrNull("""
            SELECT person_id, tar_imot, ARRAY(
                SELECT journalpost_id FROM under18_soknad WHERE behandling_id = s.behandling_id ORDER BY posisjon
            ) AS soknader FROM under18_samling s WHERE behandling_id = ?
        """.trimIndent()) {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper {
                Under18Søknadsgrunnlag(it.getLong("person_id"), it.getArray("soknader", Long::class).map(::JournalpostId), it.getBoolean("tar_imot"))
            }
        }) { "Mangler søknadsgrunnlag for $behandlingId" }

    override fun lagreAldersgrunnlag(behandlingId: BehandlingId, grunnlag: Under18Aldersgrunnlag) {
        connection.execute("""
            INSERT INTO under18_aldersgrunnlag (behandling_id, fodselsdato, aldersgruppe) VALUES (?, ?, ?)
            ON CONFLICT (behandling_id) DO UPDATE SET fodselsdato = EXCLUDED.fodselsdato, aldersgruppe = EXCLUDED.aldersgruppe
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
                setLocalDate(2, grunnlag.fødselsdato)
                setEnumName(3, grunnlag.aldersgruppe)
            }
        }
    }

    override fun hentAldersgrunnlag(behandlingId: BehandlingId): Under18Aldersgrunnlag? =
        connection.queryFirstOrNull("SELECT fodselsdato, aldersgruppe FROM under18_aldersgrunnlag WHERE behandling_id = ?") {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper { Under18Aldersgrunnlag(it.getLocalDate("fodselsdato"), it.getEnum("aldersgruppe")) }
        }

    override fun lagreSøknadsavklaring(behandlingId: BehandlingId, avklaring: Under18Søknadsavklaring) {
        connection.execute("INSERT INTO under18_soknadsavklaring (behandling_id, situasjon, vil_fortsette) VALUES (?, ?, ?)") {
            setParams {
                setLong(1, behandlingId.id)
                setEnumName(2, avklaring.situasjon)
                setBoolean(3, avklaring.søkerVilFortsette)
            }
        }
    }

    override fun hentSøknadsavklaring(behandlingId: BehandlingId): Under18Søknadsavklaring? =
        connection.queryFirstOrNull("SELECT situasjon, vil_fortsette FROM under18_soknadsavklaring WHERE behandling_id = ? ORDER BY id DESC LIMIT 1") {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper { Under18Søknadsavklaring(it.getEnum("situasjon"), it.getBoolean("vil_fortsette")) }
        }

    override fun lagreSamtykkevurdering(behandlingId: BehandlingId, vurdering: Under18SamtykkeVurdering) {
        connection.execute("""
            INSERT INTO under18_samtykkevurdering (behandling_id, begge_foreldre_samtykket, journalpost_ider)
            VALUES (?, ?, ?)
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
                setBoolean(2, vurdering.beggeForeldreSamtykket)
                setLongArray(3, vurdering.samtykkeJournalpostIder.map { it.referanse })
            }
        }
    }

    override fun hentAktivSamtykkevurdering(behandlingId: BehandlingId): Under18SamtykkeVurdering? =
        connection.queryFirstOrNull("""
            SELECT begge_foreldre_samtykket, journalpost_ider FROM under18_samtykkevurdering
            WHERE behandling_id = ? ORDER BY id DESC LIMIT 1
        """.trimIndent()) {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper {
                Under18SamtykkeVurdering(it.getBoolean("begge_foreldre_samtykket"), it.getArray("journalpost_ider", Long::class).map(::JournalpostId))
            }
        }

    override fun slett(behandlingId: BehandlingId) {
        for (tabell in listOf("under18_aldersgrunnlag", "under18_soknadsavklaring", "under18_samtykkevurdering", "under18_samling")) {
            connection.execute("DELETE FROM $tabell WHERE behandling_id = ?") { setParams { setLong(1, behandlingId.id) } }
        }
    }
}
