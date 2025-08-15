package no.nav.aap.postmottak.prosessering

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import no.nav.aap.motor.cron.CronExpression
import no.nav.aap.postmottak.forretningsflyt.gjenopptak.GjenopptakRepository

class GjenopptaBehandlingJobbUtfører(
    private val gjenopptakRepository: GjenopptakRepository,
    private val flytJobbRepository: FlytJobbRepository
) : JobbUtfører {

    override fun utfør(input: JobbInput) {
        val behandlingerForGjennopptak = gjenopptakRepository.finnBehandlingerForGjennopptak()

        behandlingerForGjennopptak.forEach { journalpostOgBehandling ->
            val jobberPåBehandling = flytJobbRepository.hentJobberForBehandling(journalpostOgBehandling.behandlingId.id)

            if (jobberPåBehandling.none { it.type() == ProsesserBehandlingJobbUtfører.type }) {
                flytJobbRepository.leggTil(
                    JobbInput(ProsesserBehandlingJobbUtfører).forBehandling(
                        sakID = journalpostOgBehandling.journalpostId.referanse,
                        behandlingId = journalpostOgBehandling.behandlingId.id
                    )
                )
            }
        }
    }

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return GjenopptaBehandlingJobbUtfører(
                repositoryProvider.provide(),
                repositoryProvider.provide(),
            )
        }

        override val type: String = "batch.gjenopptaBehandlinger"

        override val navn: String = "Gjenoppta behandling"

        override val beskrivelse: String =
            "Finner behandlinger som er satt på vent og fristen har løpt ut. Gjenopptar behandlingen av disse slik at saksbehandler kan fortsette på saksbehandling av saken"

        override val cron: CronExpression = CronExpression.create("0 0 7 * * *")
    }
}
