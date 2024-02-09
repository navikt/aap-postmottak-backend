package no.nav.aap.behandlingsflyt.faktagrunnlag

import kotlinx.coroutines.runBlocking
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.dbtest.InitTestDatabase
import no.nav.aap.behandlingsflyt.dbtestdata.ident
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.adapter.FakePersonopplysningGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.adapter.YrkesskadeRegisterMock
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakOgBehandlingService
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FaktagrunnlagTest {

    @Test
    fun `Yrkesskadedata er oppdatert`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val (_, kontekst) = runBlocking { klargjør(connection) }
            val faktagrunnlag = Faktagrunnlag(connection)

            faktagrunnlag.oppdaterFaktagrunnlagForKravliste(listOf(YrkesskadeService), kontekst)
            val erOppdatert = faktagrunnlag.oppdaterFaktagrunnlagForKravliste(listOf(YrkesskadeService), kontekst)

            assertThat(erOppdatert).isEmpty()
        }
    }

    @Test
    fun `Yrkesskadedata er ikke oppdatert`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val (ident, kontekst) = runBlocking { klargjør(connection) }
            val faktagrunnlag = Faktagrunnlag(connection)

            YrkesskadeRegisterMock.konstruer(ident = ident, periode = periode)

            val erOppdatert = faktagrunnlag.oppdaterFaktagrunnlagForKravliste(listOf(YrkesskadeService), kontekst)

            assertThat(erOppdatert)
                .hasSize(1)
                .allMatch { it === YrkesskadeService }
        }
    }

    @Test
    fun `Yrkesskadedata er utdatert, men har ingen endring fra registeret`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val (_, kontekst) = runBlocking { klargjør(connection) }
            val faktagrunnlag = Faktagrunnlag(connection)

            val erOppdatert = faktagrunnlag.oppdaterFaktagrunnlagForKravliste(listOf(YrkesskadeService), kontekst)

            assertThat(erOppdatert).isEmpty()
        }
    }

    private suspend fun klargjør(connection: DBConnection): Pair<Ident, FlytKontekst> {
        val ident = ident()
        val sak = PersonOgSakService(connection, FakePdlGateway).finnEllerOpprett(ident, periode)
        val behandling = SakOgBehandlingService(connection).finnEllerOpprettBehandling(sak.saksnummer).behandling

        FakePersonopplysningGateway.konstruer(ident, Personopplysning(Fødselsdato(LocalDate.now().minusYears(18))))

        return ident to behandling.flytKontekst()
    }

    private companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }
}

object FakePdlGateway : IdentGateway {
    override suspend fun hentAlleIdenterForPerson(ident: Ident): List<Ident> {
        return listOf(ident)
    }
}