package no.nav.aap.postmottak.hendelse.avløp

import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Status
import no.nav.aap.postmottak.kontrakt.hendelse.AvklaringsbehovHendelseDto
import no.nav.aap.postmottak.kontrakt.hendelse.DefinisjonDTO
import no.nav.aap.postmottak.kontrakt.hendelse.DokumentflytStoppetHendelse
import no.nav.aap.postmottak.kontrakt.hendelse.EndringDTO
import no.nav.aap.postmottak.sakogbehandling.behandling.Behandling
import no.nav.aap.postmottak.server.prosessering.StoppetHendelseJobbUtfører
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

private val log = LoggerFactory.getLogger(BehandlingHendelseService::class.java)

class BehandlingHendelseService(
    private val flytJobbRepository: FlytJobbRepository,
    private val journalpostRepository: JournalpostRepository
) {

    fun stoppet(behandling: Behandling, avklaringsbehovene: Avklaringsbehovene) {

        val ident = journalpostRepository.hentHvisEksisterer(behandling.id)!!.person.aktivIdent().identifikator

        val hendelse = DokumentflytStoppetHendelse(
            journalpostId = behandling.journalpostId,
            ident = ident,
            referanse = behandling.referanse.referanse, // TODO må håndtere referanseendring i oppgave
            behandlingType = behandling.typeBehandling,
            status = behandling.status(),
            avklaringsbehov = avklaringsbehovene.alle().map { avklaringsbehov ->
                AvklaringsbehovHendelseDto(definisjon = DefinisjonDTO(
                    type = avklaringsbehov.definisjon.kode,
                    behovType = avklaringsbehov.definisjon.type,
                    løsesISteg = avklaringsbehov.løsesISteg()
                ), status = avklaringsbehov.status(), endringer = avklaringsbehov.historikk.filter {
                    it.status in listOf(
                        Status.OPPRETTET, Status.SENDT_TILBAKE_FRA_BESLUTTER, Status.AVSLUTTET
                    )
                }.map { endring ->
                    EndringDTO(
                        status = endring.status,
                        tidsstempel = endring.tidsstempel,
                        endretAv = endring.endretAv,
                        frist = endring.frist
                    )
                })
            },
            opprettetTidspunkt = behandling.opprettetTidspunkt,
            hendelsesTidspunkt = LocalDateTime.now(),
            saksnummer = null
        )

        val payload = DefaultJsonMapper.toJson(hendelse)

        log.info("Legger til flytjobber og stoppethendelse for oppgave for behandling: ${behandling.id}")
        flytJobbRepository.leggTil(
            JobbInput(jobb = StoppetHendelseJobbUtfører).medPayload(payload)
        )

    }
}
