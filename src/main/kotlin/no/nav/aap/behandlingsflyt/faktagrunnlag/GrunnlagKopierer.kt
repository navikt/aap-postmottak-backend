package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.dbstuff.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.bistand.BistandsRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.PersoninformasjonRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.student.db.InMemoryStudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykdomsRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.YrkesskadeRepository

class GrunnlagKopierer(val connection: DBConnection) {

    fun overfør(fraBehandling: Behandling, tilBehandling: Behandling) {
        PersoninformasjonRepository.kopier(fraBehandling, tilBehandling)
        YrkesskadeRepository.kopier(fraBehandling, tilBehandling)
        SykdomsRepository.kopier(fraBehandling, tilBehandling)
        InMemoryStudentRepository.kopier(fraBehandling, tilBehandling)
        BistandsRepository.kopier(fraBehandling, tilBehandling)
        SykepengerErstatningRepository.kopier(fraBehandling, tilBehandling)
    }
}
