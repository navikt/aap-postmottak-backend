package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.adapters.saf.Ident
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.adapters.saf.Journalpost
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.adapters.saf.JournalpostStatus
import no.nav.aap.behandlingsflyt.joark.Joark
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Saksvurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Vurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Vurderinger
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate


// midelertidig demonstrasjon av JournalføringStegTest uten mocking bibliotek

class BehandlingRepositoryTestDouble: BehandlingRepository {

    lateinit var behandlingStub: Behandling

    override fun opprettBehandling(journalpostId: JournalpostId): Behandling {
        TODO("Not yet implemented")
    }

    override fun hent(behandlingId: BehandlingId): Behandling {
        return behandlingStub
    }

    override fun hent(journalpostId: JournalpostId): Behandling {
        TODO("Not yet implemented")
    }

}

class JournalpostRepositoryTestDouble: JournalpostRepository {

    lateinit var journalpostStub: Journalpost

    override fun hentHvisEksisterer(behandlingId: BehandlingId): Journalpost? {
        return journalpostStub
    }

    override fun lagre(journalpost: Journalpost, behandlingId: BehandlingId) {
        TODO("Not yet implemented")
    }
}

class JoarkTestDouble() : Joark {

    var oppdaterCounter = 0
    var ferdigstillCounter = 0

    var oppdaterCalls: List<Pair<Any, Any>> = mutableListOf()
    var ferdigstillCalls: List<Any> = mutableListOf()

    override fun oppdaterJournalpost(journalpost: Journalpost.MedIdent, fagsakId: String) {
        oppdaterCounter++
        oppdaterCalls += Pair(journalpost, fagsakId)
    }

    override fun ferdigstillJournalpost(journalpost: Journalpost) {
        ferdigstillCounter++
        ferdigstillCalls += journalpost

    }
}

fun generateBehandling(saksnummer: Saksnummer? = null) = Behandling(
    id = BehandlingId(1234),
    journalpostId = JournalpostId(1234),
    vurderinger = Vurderinger(saksvurdering = saksnummer?.let { Vurdering(Saksvurdering(saksnummer.toString(), false)) })
)

fun generateJournalpost() = Journalpost.MedIdent(
    personident = Ident.Personident("24234"),
    journalpostId = JournalpostId(1234),
    journalførendeEnhet = null,
    status = JournalpostStatus.JOURNALFØRT,
    mottattDato = LocalDate.now(),
    dokumenter = emptyList(),
)


class JournalføringStegTestWithoutLib {

    val behandlingRepository: BehandlingRepositoryTestDouble = BehandlingRepositoryTestDouble()
    val journalpostRepository: JournalpostRepositoryTestDouble = JournalpostRepositoryTestDouble()
    val joark: JoarkTestDouble = JoarkTestDouble()

    val journalføringSteg = JournalføringSteg(
        behandlingRepository, journalpostRepository, joark
    )

    @Test
    fun `verifiser at journalpost blir oppdatert med saksnummer og endelig journalført`() {
        val journalpost = generateJournalpost()

        val saksnummer = Saksnummer("123")
        val behandling = generateBehandling(saksnummer = saksnummer)

        behandlingRepository.behandlingStub = behandling
        journalpostRepository.journalpostStub = journalpost

        journalføringSteg.utfør(FlytKontekstMedPerioder(BehandlingId(1), TypeBehandling.DokumentHåndtering))

        assertThat(joark.oppdaterCounter).isOne()
        assertThat(joark.oppdaterCalls.first()).isEqualTo(Pair(journalpost, saksnummer.toString()))

        assertThat(joark.ferdigstillCounter).isOne()
        assertThat(joark.ferdigstillCalls.first()).isEqualTo(journalpost)

    }
}