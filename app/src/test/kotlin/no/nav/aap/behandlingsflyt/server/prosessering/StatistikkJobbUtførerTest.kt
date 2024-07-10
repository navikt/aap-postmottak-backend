package no.nav.aap.behandlingsflyt.server.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.hendelse.statistikk.StatistikkGateway
import no.nav.aap.behandlingsflyt.hendelse.statistikk.StatistikkHendelseDTO
import no.nav.aap.behandlingsflyt.hendelse.statistikk.VilkårDTO
import no.nav.aap.behandlingsflyt.hendelse.statistikk.VilkårsResultatDTO
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.json.DefaultJsonMapper
import no.nav.aap.motor.JobbInput
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.Status
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class StatistikkJobbUtførerTest {
    companion object {
        private val fakes = Fakes()

        @JvmStatic
        @AfterAll
        fun afterAll() {
            fakes.close()
        }
    }

    @Test
    fun `statistikk-jobb avgir vilkårs-resultat korrekt`() {
        val utfører = StatistikkJobbUtfører(StatistikkGateway())

        val payload =
            VilkårsResultatDTO(
                Saksnummer("1234"), typeBehandling = TypeBehandling.Førstegangsbehandling, listOf(
                    VilkårDTO.fraDomeneObjekt(
                        Vilkår(
                            type = Vilkårtype.MEDLEMSKAP,
                            vilkårsperioder = setOf(
                                Vilkårsperiode(
                                    Periode(
                                        fom = LocalDate.now().minusDays(1),
                                        tom = LocalDate.now().plusDays(1)
                                    ),
                                    Utfall.OPPFYLT,
                                    false,
                                    "ignorert",
                                    null,
                                    null,
                                    null,
                                    "123"
                                )
                            ),
                        )
                    )
                )
            )
        val hendelse = DefaultJsonMapper.toJson(payload)


        utfører.utfør(
            JobbInput(StatistikkJobbUtfører).medPayload(hendelse)
                .medParameter("statistikk-type", StatistikkType.VilkårsResultat.toString())
        )
    }

    @Test
    fun `prosesserings-kall avgir statistikk korrekt`() {
        val utfører = StatistikkJobbUtfører(StatistikkGateway())

        val payload = BehandlingFlytStoppetHendelse(
            personident = "123",
            status = Status.UTREDES,
            behandlingType = TypeBehandling.Klage,
            referanse = BehandlingReferanse("123"),
            saksnummer = Saksnummer("456"),
            opprettetTidspunkt = LocalDateTime.now(),
            avklaringsbehov = listOf()
        )

        val hendelse = DefaultJsonMapper.toJson(payload)

        utfører.utfør(
            JobbInput(StatistikkJobbUtfører).medPayload(hendelse)
                .medParameter("statistikk-type", StatistikkType.BehandlingStoppet.toString())
        )

        assertThat(fakes.statistikkHendelser).isNotEmpty()
        assertThat(fakes.statistikkHendelser.size).isEqualTo(1)
        assertThat(fakes.statistikkHendelser.first()).isEqualTo(
            StatistikkHendelseDTO(
                saksnummer = "456",
                status = Status.UTREDES,
                behandlingType = TypeBehandling.Klage,
            )
        )
    }
}