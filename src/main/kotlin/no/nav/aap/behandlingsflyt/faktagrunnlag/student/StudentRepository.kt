package no.nav.aap.behandlingsflyt.faktagrunnlag.student

import no.nav.aap.behandlingsflyt.avklaringsbehov.student.StudentVurdering
import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.behandling.BehandlingId

interface StudentRepository {
    fun lagre(behandlingId: BehandlingId, studentvurdering: StudentVurdering?)
    fun kopier(fraBehandling: Behandling, tilBehandling: Behandling)
    fun hentHvisEksisterer(behandlingId: BehandlingId): StudentGrunnlag?
    fun hent(behandlingId: BehandlingId): StudentGrunnlag
}