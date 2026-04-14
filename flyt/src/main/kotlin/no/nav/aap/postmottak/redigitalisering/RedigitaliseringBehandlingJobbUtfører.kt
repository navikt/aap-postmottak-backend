package no.nav.aap.postmottak.redigitalisering

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksvurdering
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.postmottak.prosessering.getJournalpostId
import no.nav.aap.postmottak.prosessering.getSaksnummer
import org.slf4j.LoggerFactory

class RedigitaliseringBehandlingJobbUtfører(
    private val behandlingRepository: BehandlingRepository,
    private val saksnummerRepository: SaksnummerRepository,
    private val flytJobbRepository: FlytJobbRepository,
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return RedigitaliseringBehandlingJobbUtfører(
                behandlingRepository = repositoryProvider.provide(),
                saksnummerRepository = repositoryProvider.provide(),
                flytJobbRepository = repositoryProvider.provide(),
            )
        }

        override val type = "redigitalisering.behandling"
        override val navn = "Opprett behandling for redigitalisering"
        override val beskrivelse = "Oppretter ny behandling for den kopierte journalposten og starter prosessering"
    }

    override fun utfør(input: JobbInput) {
        val nyJournalpostId = input.getJournalpostId()
        val saksnummer = input.getSaksnummer()

        log.info("Oppretter behandling for redigitalisert journalpost $nyJournalpostId")
        val nyBehandlingId = behandlingRepository.opprettBehandling(
            nyJournalpostId,
            TypeBehandling.DokumentHåndtering,
        )

        saksnummerRepository.lagreSakVurdering(nyBehandlingId, Saksvurdering(saksnummer, false))

        log.info("Legger til prosesseringsjobb for behandling ${nyBehandlingId.id}")
        flytJobbRepository.leggTil(
            JobbInput(ProsesserBehandlingJobbUtfører)
                .forBehandling(nyBehandlingId.id, nyBehandlingId.id)
                .medCallId()
        )
    }
}
