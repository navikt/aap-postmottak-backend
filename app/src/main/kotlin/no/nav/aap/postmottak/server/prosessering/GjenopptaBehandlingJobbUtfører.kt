package no.nav.aap.postmottak.server.prosessering

import no.nav.aap.behandlingsflyt.forretningsflyt.gjenopptak.GjenopptakRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.cron.CronExpression

class GjenopptaBehandlingJobbUtfører(
    private val gjenopptakRepository: GjenopptakRepository,
    private val flytJobbRepository: FlytJobbRepository
) : JobbUtfører {

    override fun utfør(input: JobbInput) {
        val behandlingerForGjennopptak = gjenopptakRepository.finnBehandlingerForGjennopptak()

        behandlingerForGjennopptak.forEach { journalpostOgBehandling ->
            val jobberPåBehandling = flytJobbRepository.hentJobberForBehandling(journalpostOgBehandling.behandlingId.id)

            if (jobberPåBehandling.none { it.type() == ProsesserBehandlingJobbUtfører.type() }) {
                flytJobbRepository.leggTil(
                    JobbInput(ProsesserBehandlingJobbUtfører).forBehandling(
                        sakID = journalpostOgBehandling.journalpostId.referanse,
                        behandlingId = journalpostOgBehandling.behandlingId.id
                    )
                )
            }
        }
    }

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return GjenopptaBehandlingJobbUtfører(
                GjenopptakRepository(connection),
                FlytJobbRepository(connection)
            )
        }

        override fun type(): String {
            return "batch.gjenopptaBehandlinger"
        }

        override fun navn(): String {
            return "Gjenoppta behandling"
        }

        override fun beskrivelse(): String {
            return "Finner behandlinger som er satt på vent og fristen har løpt ut. Gjenopptar behandlingen av disse slik at saksbehandler kan fortsette på saksbehandling av saken"
        }

        override fun cron(): CronExpression {
            return CronExpression.create("0 0 7 * * *")
        }
    }
}
