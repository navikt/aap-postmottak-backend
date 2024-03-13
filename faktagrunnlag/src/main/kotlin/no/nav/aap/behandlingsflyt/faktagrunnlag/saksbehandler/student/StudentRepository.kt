package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.Row
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class StudentRepository(private val connection: DBConnection) {

    fun lagre(behandlingId: BehandlingId, oppgittStudent: OppgittStudent?) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = StudentGrunnlag(
            null,
            studentvurdering = eksisterendeGrunnlag?.studentvurdering,
            oppgittStudent = oppgittStudent
        )

        if (eksisterendeGrunnlag != nyttGrunnlag) {
            eksisterendeGrunnlag?.let {
                deaktiverGrunnlag(behandlingId)
            }

            val oppgittStudentId = lagreOppgittStudent(oppgittStudent)
            lagreGrunnlag(behandlingId, eksisterendeGrunnlag?.studentvurdering?.id, oppgittStudentId)
        }
    }

    private fun lagreOppgittStudent(oppgittStudent: OppgittStudent?): Long? {
        if (oppgittStudent == null) {
            return null
        }
        val query = """
                INSERT INTO OPPGITT_STUDENT (har_avbrutt, avbrutt_dato)
                VALUES (?, ?)
            """.trimIndent()

        return connection.executeReturnKey(query) {
            setParams {
                setBoolean(1, oppgittStudent.harAvbruttStudie)
                setLocalDate(2, oppgittStudent.avbruttDato) // TODO: Få inn strukturert
            }
        }
    }

    fun lagre(behandlingId: BehandlingId, studentvurdering: StudentVurdering?) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = StudentGrunnlag(
            null,
            studentvurdering = studentvurdering,
            oppgittStudent = eksisterendeGrunnlag?.oppgittStudent
        )

        if (eksisterendeGrunnlag != nyttGrunnlag) {
            eksisterendeGrunnlag?.let {
                deaktiverGrunnlag(behandlingId)
            }

            val vurderingId = lagreVurdering(studentvurdering)
            lagreGrunnlag(behandlingId, vurderingId, eksisterendeGrunnlag?.oppgittStudent?.id)
        }
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE STUDENT_GRUNNLAG set aktiv = false WHERE behandling_id = ? and aktiv = true") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) }
        }
    }

    private fun lagreGrunnlag(behandlingId: BehandlingId, vurderingId: Long?, oppgittStudentId: Long?) {
        val query = """
            INSERT INTO STUDENT_GRUNNLAG (behandling_id, student_id, oppgitt_student_id) VALUES (?, ?, ?)
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, vurderingId)
                setLong(3, oppgittStudentId)
            }
        }
    }

    private fun lagreVurdering(studentvurdering: StudentVurdering?): Long? {
        if (studentvurdering == null) {
            return null
        }
        val query = """
                INSERT INTO STUDENT_VURDERING (begrunnelse, oppfylt, avbrutt_dato)
                VALUES (?, ?, ?)
            """.trimIndent()

        val vurderingId = connection.executeReturnKey(query) {
            setParams {
                setString(1, studentvurdering.begrunnelse)
                setBoolean(2, studentvurdering.oppfyller11_14)
                setLocalDate(3, studentvurdering.avbruttStudieDato)
            }
        }

        studentvurdering.dokumenterBruktIVurdering.forEach { lagreDokument(vurderingId, it) }

        return vurderingId
    }

    private fun lagreDokument(vurderingId: Long, journalpostId: JournalpostId) {
        val query = """
            INSERT INTO STUDENT_VURDERING_DOKUMENTER (vurdering_id, journalpost) 
            VALUES (?, ?)
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, vurderingId)
                setString(2, journalpostId.identifikator)
            }
        }
    }

    fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val hentHvisEksisterer = hentHvisEksisterer(fraBehandling)
        if (hentHvisEksisterer == null) {
            return
        }

        val query = """
            INSERT INTO STUDENT_GRUNNLAG (behandling_id, student_id, oppgitt_student_id) 
            SELECT ?, student_id, oppgitt_student_id from STUDENT_GRUNNLAG where behandling_id = ? and aktiv
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): StudentGrunnlag? {
        val query = """
            SELECT * FROM STUDENT_GRUNNLAG WHERE behandling_id = ? AND aktiv = true
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                mapGrunnlag(it)
            }
        }
    }

    private fun mapGrunnlag(row: Row): StudentGrunnlag {
        return StudentGrunnlag(
            row.getLong("id"),
            mapStudentVurdering(row.getLongOrNull("student_id")),
            mapOppgittStudent(row.getLongOrNull("oppgitt_student_id"))
        )
    }

    private fun mapOppgittStudent(id: Long?): OppgittStudent? {
        if (id == null) {
            return null
        }
        val query = """
            SELECT * FROM OPPGITT_STUDENT WHERE id = ?
        """.trimIndent()
        //HAR_AVBRUTT, avbrutt_dato

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, id)
            }
            setRowMapper {
                OppgittStudent(
                    it.getLong("id"),
                    it.getBoolean("HAR_AVBRUTT"),
                    it.getLocalDateOrNull("avbrutt_dato")
                )
            }
        }
    }

    private fun mapStudentVurdering(studentId: Long?): StudentVurdering? {
        if (studentId == null) {
            return null
        }
        val query = """
            SELECT * FROM STUDENT_VURDERING WHERE id = ?
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, studentId)
            }
            setRowMapper {
                StudentVurdering(
                    it.getLong("id"),
                    it.getString("begrunnelse"),
                    hentDokumenterTilVurdering(studentId),
                    it.getBooleanOrNull("oppfylt"),
                    it.getLocalDateOrNull("avbrutt_dato")
                )
            }
        }
    }

    private fun hentDokumenterTilVurdering(studentId: Long): List<JournalpostId> {
        val query = """
            SELECT journalpost FROM STUDENT_VURDERING_DOKUMENTER WHERE vurdering_id = ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, studentId)
            }
            setRowMapper {
                JournalpostId(it.getString("journalpost"))
            }
        }
    }

    fun hent(behandlingId: BehandlingId): StudentGrunnlag {
        return requireNotNull(hentHvisEksisterer(behandlingId))
    }

}
