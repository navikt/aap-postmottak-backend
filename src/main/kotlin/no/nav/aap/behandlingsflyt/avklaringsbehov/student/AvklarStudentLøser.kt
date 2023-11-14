
package no.nav.aap.behandlingsflyt.avklaringsbehov.student

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovsLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.student.StudentRepository
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

class AvklarStudentLøser(val connection: DBConnection) : AvklaringsbehovsLøser<AvklarStudentLøsning> {

    private val behandlingRepository = BehandlingRepository(connection)
    private val studentRepository = StudentRepository(connection)

    override fun løs(kontekst: FlytKontekst, løsning: AvklarStudentLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)

        studentRepository.lagre(
            behandlingId = behandling.id,
            studentvurdering = løsning.studentvurdering,
        )

        return LøsningsResultat(
            begrunnelse = løsning.studentvurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_STUDENT
    }
}