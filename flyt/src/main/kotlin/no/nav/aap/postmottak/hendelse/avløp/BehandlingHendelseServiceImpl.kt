package no.nav.aap.postmottak.hendelse.avløp

import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.postmottak.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.flyt.utledType
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandling
import no.nav.aap.postmottak.kontrakt.hendelse.AvklaringsbehovHendelseDto
import no.nav.aap.postmottak.kontrakt.hendelse.DokumentflytStoppetHendelse
import no.nav.aap.postmottak.kontrakt.hendelse.EndringDTO
import no.nav.aap.postmottak.prosessering.StoppetHendelseJobbUtfører
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class BehandlingHendelseServiceImpl(
    private val flytJobbRepository: FlytJobbRepository,
    private val journalpostRepository: JournalpostRepository,
) : BehandlingHendelseService {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun stoppet(behandling: Behandling, avklaringsbehovene: Avklaringsbehovene) {

        val journalpost = journalpostRepository.hentHvisEksisterer(behandling.id)

        if (journalpost == null && avklaringsbehovene.alle().isEmpty()) {
            log.warn("Finner ingen lagret journalpost for behandling med id=${behandling.id}, og ingen avklaringsbehov på behandlingen. Avbryter videre behandling og sender ikke hendelse!")
            return
        }

        if (journalpost == null) {
            throw IllegalStateException("Finner ingen lagret journalpost for behandling med id=${behandling.id}, og behandlingen har avklaringsbehov. Behandlingen er i en ugyldig tilstand.")
        }

        val ident = journalpost.person.aktivIdent().identifikator

        val alleAvklaringsbehov = avklaringsbehovene.alle()
            .sortedWith(compareBy(utledType(behandling.typeBehandling).flyt().stegComparator) { it.funnetISteg })
        val aktivtAvklaringsbehov = alleAvklaringsbehov.firstOrNull {
            !it.erVentepunkt() && it.skalStoppeHer(behandling.aktivtSteg())
        }

        val hendelse = DokumentflytStoppetHendelse(
            journalpostId = behandling.journalpostId,
            ident = ident,
            referanse = behandling.referanse.referanse,
            behandlingType = behandling.typeBehandling,
            status = behandling.status(),
            aktivtAvklaringsbehov = aktivtAvklaringsbehov?.definisjon,
            avklaringsbehov = alleAvklaringsbehov
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
