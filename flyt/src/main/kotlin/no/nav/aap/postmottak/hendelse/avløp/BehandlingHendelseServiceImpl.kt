package no.nav.aap.postmottak.hendelse.avløp

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.postmottak.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.flyt.utledType
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandling
import no.nav.aap.postmottak.kontrakt.hendelse.AvklaringsbehovHendelseDto
import no.nav.aap.postmottak.kontrakt.hendelse.DokumentflytStoppetHendelse
import no.nav.aap.postmottak.kontrakt.hendelse.EndringDTO
import no.nav.aap.postmottak.prosessering.StoppetHendelseJobbUtfører
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

private val log = LoggerFactory.getLogger(BehandlingHendelseServiceImpl::class.java)

class BehandlingHendelseServiceImpl(
    private val flytJobbRepository: FlytJobbRepository,
    private val journalpostRepository: JournalpostRepository,
    private val behandlingFlytGateway: BehandlingsflytGateway
) : BehandlingHendelseService {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        flytJobbRepository = repositoryProvider.provide(),
        journalpostRepository = repositoryProvider.provide(),
        behandlingFlytGateway = gatewayProvider.provide(),
    )

    override fun stoppet(behandling: Behandling, avklaringsbehovene: Avklaringsbehovene) {

        val ident = journalpostRepository.hentHvisEksisterer(behandling.id)!!.person.aktivIdent().identifikator
        val nyesteSakForBruker = behandlingFlytGateway.finnSaker(Ident(ident)).maxByOrNull { it.periode.tom }

        val hendelse = DokumentflytStoppetHendelse(
            journalpostId = behandling.journalpostId,
            saksnummer = nyesteSakForBruker?.saksnummer,
            ident = ident,
            referanse = behandling.referanse.referanse, // TODO må håndtere referanseendring i oppgave
            behandlingType = behandling.typeBehandling,
            status = behandling.status(),
            avklaringsbehov = avklaringsbehovene.alle()
                .sortedWith(compareBy((utledType(behandling.typeBehandling)).flyt().stegComparator) { it.funnetISteg })
                .map { avklaringsbehov ->
                    AvklaringsbehovHendelseDto(
                        avklaringsbehovDefinisjon = avklaringsbehov.definisjon,
                        status = avklaringsbehov.status(),
                        endringer = avklaringsbehov.historikk.map { endring ->
                            EndringDTO(
                                status = endring.status,
                                tidsstempel = endring.tidsstempel,
                                endretAv = endring.endretAv,
                                frist = endring.frist,
                                begrunnelse = endring.begrunnelse,
                                årsakTilSattPåVent = when (endring.grunn) {
                                    ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER -> no.nav.aap.postmottak.kontrakt.hendelse.ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER
                                    ÅrsakTilSettPåVent.VENTER_PÅ_SVAR_FRA_BRUKER -> no.nav.aap.postmottak.kontrakt.hendelse.ÅrsakTilSettPåVent.VENTER_PÅ_SVAR_FRA_BRUKER
                                    ÅrsakTilSettPåVent.VENTER_PÅ_BEHANDLING_I_GOSYS -> no.nav.aap.postmottak.kontrakt.hendelse.ÅrsakTilSettPåVent.VENTER_PÅ_BEHANDLING_I_GOSYS
                                    null -> null
                                    else -> error("Skal ikke kunne skje: ${endring.grunn}")
                                }
                            )
                        })
                },
            opprettetTidspunkt = behandling.opprettetTidspunkt,
            hendelsesTidspunkt = LocalDateTime.now(),
        )

        val payload = DefaultJsonMapper.toJson(hendelse)

        log.info("Legger til flytjobber og stoppethendelse for oppgave for behandling: ${behandling.id}")
        flytJobbRepository.leggTil(
            JobbInput(jobb = StoppetHendelseJobbUtfører).medPayload(payload)
                .forBehandling(behandling.journalpostId.referanse, behandling.id.id)
        )

    }
}
