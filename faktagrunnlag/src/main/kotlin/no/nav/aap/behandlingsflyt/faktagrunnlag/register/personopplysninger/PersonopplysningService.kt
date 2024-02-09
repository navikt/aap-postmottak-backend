package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlagkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.adapter.FakePersonopplysningGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.verdityper.flyt.FlytKontekst

class PersonopplysningService private constructor(
    private val connection: DBConnection,
    private val personopplysningGateway: PersonopplysningGateway
) : Grunnlag {

    companion object : Grunnlagkonstruktør {
        override fun konstruer(connection: DBConnection): PersonopplysningService {
            return PersonopplysningService(
                connection,
                FakePersonopplysningGateway)
        }
    }

    override fun oppdater(kontekst: FlytKontekst): Boolean {
        val personopplysningRepository = PersonopplysningRepository(connection)
        val sakService = SakService(connection)
        val sak = sakService.hent(kontekst.sakId)

        val personopplysninger = personopplysningGateway.innhent(sak.person.identer())
        if (personopplysninger.size != 1) {
            error("fant ingen eller fler personer enn forventet")
        }

        val behandlingId = kontekst.behandlingId
        val gamleData = personopplysningRepository.hentHvisEksisterer(behandlingId)

        personopplysningRepository.lagre(behandlingId, personopplysninger.first())
        val nyeData = personopplysningRepository.hentHvisEksisterer(behandlingId)

        return nyeData == gamleData
    }
}
