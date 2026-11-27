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

    override fun hentBehandlingForSøknad(journalpostId: JournalpostId): BehandlingId? =
        connection.queryFirstOrNull("SELECT behandling_id FROM under18_soknadstilknytning WHERE journalpost_id = ?") {
            setParams { setLong(1, journalpostId.referanse) }
            setRowMapper { BehandlingId(it.getLong("behandling_id")) }
        }

    override fun lagreSøknadsgrunnlag(behandlingId: BehandlingId, grunnlag: Under18Søknadsgrunnlag) {
        val samling = Samlingslagring(connection, behandlingId)
        val tidligere = if (samling.låsHvisFinnes()) hentSøknadsgrunnlag(behandlingId) else null
        if (tidligere == grunnlag) return
        tidligere?.validerEndringTil(grunnlag, behandlingId)

        val samlingId = samling.lagre(grunnlag)
        erstattGrunnlag(behandlingId, "under18_samlingsgrunnlag", "samling_id", samlingId)
        samling.knyttTilSøknader(grunnlag.søknader - tidligere?.søknader.orEmpty())
    }

    override fun hentSøknadsgrunnlag(behandlingId: BehandlingId): Under18Søknadsgrunnlag =
        checkNotNull(connection.queryFirstOrNull("""
            SELECT s.person_id, s.journalpost_ider FROM under18_samlingsgrunnlag g
            JOIN under18_samling s ON s.id = g.samling_id WHERE g.behandling_id = ? AND g.aktiv
        """.trimIndent()) {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper {
                Under18Søknadsgrunnlag(
                    personId = it.getLong("person_id"),
                    søknader = it.getArray("journalpost_ider", Long::class).mapTo(mutableSetOf(), ::JournalpostId),
                )
            }
        }) { "Mangler søknadsgrunnlag for $behandlingId" }

    override fun lagreAldersgrunnlag(behandlingId: BehandlingId, grunnlag: Under18Aldersgrunnlag) {
        if (hentAldersgrunnlag(behandlingId) == grunnlag) return
        val id = connection.executeReturnKey("INSERT INTO under18_aldersvurdering (fodselsdato, aldersgruppe) VALUES (?, ?)") {
            setParams {
                setLocalDate(1, grunnlag.fødselsdato)
                setEnumName(2, grunnlag.aldersgruppe)
            }
        }
        erstattGrunnlag(behandlingId, "under18_aldersgrunnlag", "aldersvurdering_id", id)
    }

    override fun hentAldersgrunnlag(behandlingId: BehandlingId): Under18Aldersgrunnlag? =
        connection.queryFirstOrNull("""
            SELECT fodselsdato, aldersgruppe
            FROM under18_aldersgrunnlag g JOIN under18_aldersvurdering v ON v.id = g.aldersvurdering_id
            WHERE g.behandling_id = ? AND g.aktiv
        """.trimIndent()) {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper {
                Under18Aldersgrunnlag(it.getLocalDate("fodselsdato"), it.getEnum("aldersgruppe"))
            }
        }

    override fun lagreSøknadsavklaring(behandlingId: BehandlingId, avklaring: Under18Søknadsavklaring) {
        val id = connection.executeReturnKey("""
            INSERT INTO under18_soknadsavklaring (situasjon, vil_fortsette, fylt_17_aar_og_9_maaneder)
            VALUES (?, ?, ?)
        """.trimIndent()) {
            setParams {
                setEnumName(1, avklaring.situasjon)
                setBoolean(2, avklaring.søkerVilFortsette)
                setBoolean(3, avklaring.fylt17ÅrOg9Måneder)
            }
        }
        erstattGrunnlag(behandlingId, "under18_soknadsavklaring_grunnlag", "soknadsavklaring_id", id)
    }

    override fun hentSøknadsavklaring(behandlingId: BehandlingId): Under18Søknadsavklaring? =
        connection.queryFirstOrNull("""
            SELECT situasjon, vil_fortsette, fylt_17_aar_og_9_maaneder
            FROM under18_soknadsavklaring_grunnlag g JOIN under18_soknadsavklaring v ON v.id = g.soknadsavklaring_id
            WHERE g.behandling_id = ? AND g.aktiv
        """.trimIndent()) {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper {
                Under18Søknadsavklaring(
                    it.getEnumOrNull("situasjon"), it.getBooleanOrNull("vil_fortsette"), it.getBooleanOrNull("fylt_17_aar_og_9_maaneder")
                )
            }
        }

    override fun lagreSamtykkevurdering(behandlingId: BehandlingId, vurdering: Under18SamtykkeVurdering) {
        val id = connection.executeReturnKey("""
            INSERT INTO under18_samtykkevurdering (begge_foreldre_samtykket, journalpost_ider)
            VALUES (?, ?)
        """.trimIndent()) {
            setParams {
                setBoolean(1, vurdering.beggeForeldreSamtykket)
                setLongArray(2, vurdering.samtykkeJournalpostIder.map { it.referanse })
            }
        }
        erstattGrunnlag(behandlingId, "under18_samtykkegrunnlag", "samtykkevurdering_id", id)
    }

    override fun hentAktivSamtykkevurdering(behandlingId: BehandlingId): Under18SamtykkeVurdering? =
        connection.queryFirstOrNull("""
            SELECT begge_foreldre_samtykket, journalpost_ider FROM under18_samtykkegrunnlag g
            JOIN under18_samtykkevurdering v ON v.id = g.samtykkevurdering_id WHERE g.behandling_id = ? AND g.aktiv
        """.trimIndent()) {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper {
                Under18SamtykkeVurdering(it.getBoolean("begge_foreldre_samtykket"), it.getArray("journalpost_ider", Long::class).map(::JournalpostId))
            }
        }

    private fun erstattGrunnlag(behandlingId: BehandlingId, tabell: String, vurderingskolonne: String, vurderingId: Long) {
        connection.execute("UPDATE $tabell SET aktiv = FALSE WHERE behandling_id = ? AND aktiv") {
            setParams { setLong(1, behandlingId.id) }
        }
        connection.execute("INSERT INTO $tabell (behandling_id, $vurderingskolonne) VALUES (?, ?)") {
            setParams {
                setLong(1, behandlingId.id)
                setLong(2, vurderingId)
            }
        }
    }
}

private class Samlingslagring(private val connection: DBConnection, private val behandlingId: BehandlingId) {
    // Lås alltid første versjon, også etter at en annen transaksjon har erstattet den aktive.
    fun låsHvisFinnes(): Boolean = connection.queryFirstOrNull(
        "SELECT id FROM under18_samlingsgrunnlag WHERE behandling_id = ? ORDER BY id LIMIT 1 FOR UPDATE"
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { it.getLong("id") }
    } != null

    fun lagre(grunnlag: Under18Søknadsgrunnlag): Long = connection.executeReturnKey(
        "INSERT INTO under18_samling (person_id, journalpost_ider) VALUES (?, ?)"
    ) {
        setParams {
            setLong(1, grunnlag.personId)
            setLongArray(2, grunnlag.søknader.map { it.referanse })
        }
    }

    fun knyttTilSøknader(nyeSøknader: Set<JournalpostId>) {
        connection.executeBatch(
            "INSERT INTO under18_soknadstilknytning (behandling_id, journalpost_id) VALUES (?, ?)",
            nyeSøknader.toList()
        ) {
            setParams { journalpostId ->
                setLong(1, behandlingId.id)
                setLong(2, journalpostId.referanse)
            }
        }
    }
}
