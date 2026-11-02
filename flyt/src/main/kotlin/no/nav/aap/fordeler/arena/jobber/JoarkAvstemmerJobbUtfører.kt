package no.nav.aap.fordeler.arena.jobber

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import no.nav.aap.motor.cron.CronExpression
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.joarkavstemmer.Avstemmingsrapport
import no.nav.aap.postmottak.joarkavstemmer.AvstemmingsrapportRepository
import no.nav.aap.postmottak.joarkavstemmer.JoarkAvstemmer
import no.nav.aap.postmottak.joarkavstemmer.JournalpostAvstemmingsresultat
import java.time.Instant

class JoarkAvstemmerJobbUtfører(
    private val joarkAvstemmer: JoarkAvstemmer,
    private val rapportRepository: AvstemmingsrapportRepository,
) : JobbUtfører {

    override fun utfør(
        input: JobbInput
    ) {
        val startetTidspunkt = Instant.now()
        val resultater = joarkAvstemmer.avstem()
        val fullfortTidspunkt = Instant.now()
        rapportRepository.lagre(
            Avstemmingsrapport(
                startetTidspunkt = startetTidspunkt,
                fullførtTidspunkt = fullfortTidspunkt,
                resultater = resultater.map(JournalpostAvstemmingsresultat::fra),
            )
        )
    }

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): JobbUtfører {
            return JoarkAvstemmerJobbUtfører(
                joarkAvstemmer = JoarkAvstemmer(
                    doksikkerhetsnettGateway = gatewayProvider.provide(),
                    regelRepository = repositoryProvider.provide(),
                    behandlingRepository = repositoryProvider.provide(),
                    gosysOppgaveGateway = gatewayProvider.provide(),
                    journalpostGateway = gatewayProvider.provide(),
                    meterRegistry = PrometheusProvider.prometheus,
                ),
                rapportRepository = repositoryProvider.provide(),
            )
        }

        override val beskrivelse: String = "Avstemmer mot Joark"
        override val navn: String = "postmottak.avstemming"
        override val type: String = "Joark-avstemming"
        override val cron: CronExpression = CronExpression.create("0 0 3 * * *")
    }
}