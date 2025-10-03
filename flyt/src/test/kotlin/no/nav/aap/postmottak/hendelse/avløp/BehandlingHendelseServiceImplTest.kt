package no.nav.aap.postmottak.hendelse.avløp

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.avklaringsbehov.Avklaringsbehov
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.postmottak.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.postmottak.avklaringsbehov.Endring
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway
import no.nav.aap.postmottak.gateway.BehandlingsflytSak
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandling
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingsreferansePathParam
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.behandling.Status
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.hendelse.DokumentflytStoppetHendelse
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.kontrakt.steg.StegType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockKExtension::class)
class BehandlingHendelseServiceImplTest {
    @MockK
    private lateinit var behandlingsflytGateway: BehandlingsflytGateway

    @MockK
    private lateinit var flytJobbRepository: FlytJobbRepository

    @MockK
    private lateinit var journalpostRepository: JournalpostRepository

    @MockK
    private lateinit var avklaringsbehovRepository: AvklaringsbehovRepository


    var behandlingHendelseService: BehandlingHendelseServiceImpl? = null

    @BeforeEach
    fun setUp() {
        behandlingHendelseService =
            BehandlingHendelseServiceImpl(flytJobbRepository, journalpostRepository, behandlingsflytGateway)
    }

    @Test
    fun `Avklaringsbehov sorteres i rekkefølgen de kan løses i`() {
        val payloadSlot = slot<JobbInput>()
        every { flytJobbRepository.leggTil(capture(payloadSlot)) } returns Unit

        val behandling = Behandling(
            BehandlingId(1),
            JournalpostId(1),
            Status.OPPRETTET,
            mutableListOf(),
            LocalDateTime.now(),
            2,
            BehandlingsreferansePathParam(UUID.randomUUID()),
            TypeBehandling.Journalføring
        )

        every { journalpostRepository.hentHvisEksisterer(behandling.id) } returns mockk {
            every { person.aktivIdent().identifikator } returns "25652112526"
        }

        every { avklaringsbehovRepository.hent(any()) } returns mutableListOf(
            lagAvklaringsbehov(Definisjon.AVKLAR_SAK, StegType.AVKLAR_SAK, 1, "Dokument er tildelt sak"),
            lagAvklaringsbehov(Definisjon.AVKLAR_TEMA, StegType.AVKLAR_TEMA, 2, "Dokument er ment for AAP")
        )


        every { behandlingsflytGateway.finnSaker(any()) } returns listOf(
            BehandlingsflytSak(
                saksnummer = "52652222",
                periode = Periode(
                    fom = LocalDate.now().minusYears(2),
                    tom = LocalDate.now().minusMonths(6)
                ),
                resultat = null
            )
        )

        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, behandling.id)

        behandlingHendelseService!!.stoppet(behandling, avklaringsbehovene)

        val payload = payloadSlot.captured.payload()
        val hendelse = DefaultJsonMapper.fromJson<DokumentflytStoppetHendelse>(payload)

        assertThat(hendelse.avklaringsbehov.map { it.avklaringsbehovDefinisjon })
            .containsExactly(Definisjon.AVKLAR_TEMA, Definisjon.AVKLAR_SAK)
    }

    private fun lagAvklaringsbehov(
        definisjon: Definisjon,
        stegType: StegType,
        id: Long,
        begrunnelse: String
    ): Avklaringsbehov {
        val historikk = mutableListOf(
            Endring(
                status = no.nav.aap.postmottak.kontrakt.avklaringsbehov.Status.OPPRETTET,
                begrunnelse = "",
                endretAv = "Kelvin"
            ),
            Endring(
                status = no.nav.aap.postmottak.kontrakt.avklaringsbehov.Status.AVSLUTTET,
                begrunnelse = begrunnelse,
                endretAv = "testSaksbehandler"
            )
        )

        return Avklaringsbehov(
            historikk = historikk,
            id = id,
            definisjon = definisjon,
            funnetISteg = stegType,
            kreverToTrinn = null
        )
    }
}
