package no.nav.aap.postmottak.prosessering

import no.nav.aap.behandlingsflyt.flyt.steg.internal.StegKonstruktørImpl
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravGrunnlagImpl
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.flyt.FlytOrkestrator
import no.nav.aap.postmottak.hendelse.avløp.BehandlingHendelseServiceImpl
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingFlytRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.lås.TaSkriveLåsRepository

class ProsesserBehandlingJobbUtfører(
    private val låsRepository: TaSkriveLåsRepository,
    private val kontroller: FlytOrkestrator
) : JobbUtfører {

    override fun utfør(input: JobbInput) {
        val skrivelås = låsRepository.lås(BehandlingId(input.behandlingId()))

        val kontekst = kontroller.opprettKontekst(skrivelås.id)

        kontroller.forberedBehandling(kontekst)
        kontroller.prosesserBehandling(kontekst)

        låsRepository.verifiserSkrivelås(skrivelås)
    }

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            val repositoryProvider = RepositoryProvider(connection)
            val journalpostRepository = repositoryProvider.provide(
                JournalpostRepository::class,
            )
            return ProsesserBehandlingJobbUtfører(
                repositoryProvider.provide(TaSkriveLåsRepository::class),
                FlytOrkestrator(
                    StegKonstruktørImpl(connection),
                    InformasjonskravGrunnlagImpl(connection),
                    repositoryProvider.provide(
                        AvklaringsbehovRepository::class,
                    ),
                    repositoryProvider.provide(BehandlingRepository::class),
                    repositoryProvider.provide(
                        BehandlingFlytRepository::class,
                    ),
                    BehandlingHendelseServiceImpl(
                        FlytJobbRepository(connection),
                        journalpostRepository
                    ),
                )
            )
        }

        override fun type(): String {
            return "flyt.prosesserBehandling"
        }

        override fun navn(): String {
            return "Prosesser behandling"
        }

        override fun beskrivelse(): String {
            return "Ansvarlig for å drive prosessen på en gitt behandling"
        }
    }
}
