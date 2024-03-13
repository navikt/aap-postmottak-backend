package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.søknad

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlagkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.OppgittStudent
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.verdityper.flyt.FlytKontekst

class SøknadService private constructor(
    private val mottaDokumentService: MottaDokumentService,
    private val studentRepository: StudentRepository
) : Grunnlag {

    companion object : Grunnlagkonstruktør {
        override fun konstruer(connection: DBConnection): SøknadService {
            return SøknadService(
                MottaDokumentService(
                    MottattDokumentRepository(connection)
                ),
                StudentRepository(connection)
            )
        }
    }

    override fun oppdater(kontekst: FlytKontekst): Boolean {
        val ubehandletSøknader = mottaDokumentService.søknaderSomIkkeHarBlittBehandlet(kontekst.sakId)
        if (ubehandletSøknader.isEmpty()) {
            return false
        }

        val behandlingId = kontekst.behandlingId

        for (ubehandletSøknad in ubehandletSøknader) {
            studentRepository.lagre(behandlingId = behandlingId, OppgittStudent(harAvbruttStudie = ubehandletSøknad.student))

            mottaDokumentService.knyttTilBehandling(
                sakId = kontekst.sakId,
                behandlingId = kontekst.behandlingId,
                journalpostId = ubehandletSøknad.journalpostId
            )
        }

        return true // Antar her at alle nye søknader gir en endring vi må ta hensyn til
    }
}
