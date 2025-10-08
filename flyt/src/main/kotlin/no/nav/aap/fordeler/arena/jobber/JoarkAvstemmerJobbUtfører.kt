package no.nav.aap.fordeler.arena.jobber

import io.micrometer.core.instrument.MeterRegistry
import no.nav.aap.fordeler.RegelRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import no.nav.aap.motor.cron.CronExpression
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.gateway.DoksikkerhetsnettGateway
import no.nav.aap.postmottak.gateway.GosysOppgaveGateway
import no.nav.aap.postmottak.gateway.JournalpostGateway
import no.nav.aap.postmottak.joarkavstemmer.JoarkAvstemmer
import no.nav.aap.unleash.UnleashGateway

class JoarkAvstemmerJobbUtfører(
    private val doksikkerhetsnettGateway: DoksikkerhetsnettGateway,
    private val regelRepository: RegelRepository,
    private val gosysOppgaveGateway: GosysOppgaveGateway,
    private val journalpostGateway: JournalpostGateway,
    private val unleashGateway: UnleashGateway,
    private val meterRegistry: MeterRegistry
) : JobbUtfører {

    override fun utfør(
        input: JobbInput
    ) {
        JoarkAvstemmer(
            doksikkerhetsnettGateway = doksikkerhetsnettGateway,
            regelRepository = regelRepository,
            gosysOppgaveGateway = gosysOppgaveGateway,
            journalpostGateway = journalpostGateway,
            meterRegistry = meterRegistry
        ).avstem()
    }

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): JobbUtfører {
            return JoarkAvstemmerJobbUtfører(
                doksikkerhetsnettGateway = gatewayProvider.provide(),
                regelRepository = repositoryProvider.provide(),
                gosysOppgaveGateway = gatewayProvider.provide(),
                journalpostGateway = gatewayProvider.provide(),
                unleashGateway = gatewayProvider.provide(),
                meterRegistry = PrometheusProvider.prometheus
            )
        }

        override val beskrivelse: String = "Avstemmer mot Joark"
        override val navn: String = "postmottak.avstemming"
        override val type: String = "Joark-avstemming"
        override val cron: CronExpression = CronExpression.create("0 0 3 * * *")
    }
}