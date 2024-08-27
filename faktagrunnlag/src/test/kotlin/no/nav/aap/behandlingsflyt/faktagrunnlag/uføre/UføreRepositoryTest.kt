package no.nav.aap.behandlingsflyt.faktagrunnlag.uføre

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.dbtest.InitTestDatabase
import no.nav.aap.behandlingsflyt.dbtestdata.ident
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.arbeidsevne.FakePdlGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.EndringType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate

class UføreRepositoryTest {

    @Test
    fun `Finner ikke uføre hvis ikke lagret`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val uføreRepository = UføreRepository(connection)
            val uføreGrunnlag = uføreRepository.hentHvisEksisterer(behandling.id)
            assertThat(uføreGrunnlag).isNull()
        }
    }

    @Test
    fun `Lagrer og henter uføre`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val uføreRepository = UføreRepository(connection)
            uføreRepository.lagre(behandling.id, Uføre(Prosent(100)))
            val uføreGrunnlag = uføreRepository.hentHvisEksisterer(behandling.id)
            assertThat(uføreGrunnlag?.vurdering).isEqualTo(Uføre(Prosent(100)))
        }
    }

    @Test
    fun `Lagrer ikke lik uføre flere ganger`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val uføreRepository = UføreRepository(connection)
            uføreRepository.lagre(behandling.id, Uføre(Prosent(100)))
            uføreRepository.lagre(behandling.id, Uføre(Prosent(80)))
            uføreRepository.lagre(behandling.id, Uføre(Prosent(80)))

            val opplysninger = connection.queryList(
                """
                    SELECT u.UFOREGRAD
                    FROM BEHANDLING b
                    INNER JOIN UFORE_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN UFORE u ON g.UFORE_ID = u.ID
                    WHERE b.SAK_ID = ?
                    """.trimIndent()
            ) {
                setParams {
                    setLong(1, sak.id.toLong())
                }
                setRowMapper { row -> row.getInt("UFOREGRAD") }
            }
            assertThat(opplysninger)
                .hasSize(2)
                .containsExactly(100, 80)
        }
    }

    @Test
    fun `Kopiering av uføre fra en behandling uten opplysningene skal ikke føre til feil`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val uføreRepository = UføreRepository(connection)
            assertDoesNotThrow {
                uføreRepository.kopier(BehandlingId(Long.MAX_VALUE - 1), BehandlingId(Long.MAX_VALUE))
            }
        }
    }

    @Test
    fun `Lagrer nye uføreopplysninger som ny rad og deaktiverer forrige versjon av opplysningene`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)
            val uføreRepository = UføreRepository(connection)

            uføreRepository.lagre(behandling.id, Uføre(Prosent(100)))
            val orginaltGrunnlag = uføreRepository.hentHvisEksisterer(behandling.id)
            assertThat(orginaltGrunnlag?.vurdering).isEqualTo(Uføre(Prosent(100)))

            uføreRepository.lagre(behandling.id, Uføre(Prosent(80)))
            val oppdatertGrunnlag = uføreRepository.hentHvisEksisterer(behandling.id)
            assertThat(oppdatertGrunnlag?.vurdering).isEqualTo(Uføre(Prosent(80)))

            data class Opplysning(
                val aktiv: Boolean,
                val uføregrad: Prosent
            )

            val opplysninger =
                connection.queryList(
                    """
                    SELECT g.AKTIV, u.UFOREGRAD
                    FROM BEHANDLING b
                    INNER JOIN UFORE_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN UFORE u ON g.UFORE_ID = u.ID
                    WHERE b.SAK_ID = ?
                    """.trimIndent()
                ) {
                    setParams {
                        setLong(1, sak.id.toLong())
                    }
                    setRowMapper { row ->
                        Opplysning(
                            aktiv = row.getBoolean("AKTIV"),
                            uføregrad = Prosent(row.getInt("UFOREGRAD"))
                        )
                    }
                }
            assertThat(opplysninger)
                .hasSize(2)
                .containsExactly(
                    Opplysning(aktiv = false, uføregrad = Prosent(100)),
                    Opplysning(aktiv = true, uføregrad = Prosent(80))
                )
        }
    }

    private companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(connection, FakePdlGateway).finnEllerOpprett(ident(), periode)
    }

    private fun behandling(connection: DBConnection, sak: Sak): Behandling {
        return SakOgBehandlingService(connection).finnEllerOpprettBehandling(
            sak.saksnummer,
            listOf(Årsak(EndringType.MOTTATT_SØKNAD))
        ).behandling
    }
}
