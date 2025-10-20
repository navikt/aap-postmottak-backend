package no.nav.aap.postmottak.forretningsflyt.steg.journalføring

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.faktagrunnlag.GrunnlagKopierer
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.AvklarTemaRepository
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.gateway.Fagsystem
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.postmottak.prosessering.ProsesserBehandlingJobbUtfører
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(VideresendSteg::class.java)

class VideresendSteg(
    private val saksnummerRepository: SaksnummerRepository,
    private val avklarTemaRepository: AvklarTemaRepository,
    private val behandlingRepository: BehandlingRepository,
    private val journalpostRepository: JournalpostRepository,
    private val flytJobbRepository: FlytJobbRepository,
    private val kopierer: GrunnlagKopierer
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): BehandlingSteg {
            return VideresendSteg(
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                repositoryProvider.provide(),
               repositoryProvider.provide(),
                GrunnlagKopierer(repositoryProvider)
            )
        }

        override fun type(): StegType {
            return StegType.VIDERESEND
        }

    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val journalpost =
            requireNotNull(journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)) { "Journalpost skal eksistere før VideresendSteg" }

        if (journalpost.erUgyldig()) {
            log.info("Journalpost skal ikke behandles - har status ${journalpost.status}")
            return Fullført
        }

        if (erJournalførtPåAnnetFagsystem(journalpost)) {
            log.info("Journalpost er journalført på annet fagsystem - videresender ikke")
            return Fullført
        }

        val saksnummervurdering = saksnummerRepository.hentSakVurdering(kontekst.behandlingId)
        val avklarTemaVurdering = avklarTemaRepository.hentTemaAvklaring(kontekst.behandlingId)

        requireNotNull(avklarTemaVurdering) { "Tema skal være avklart før VideresendSteg" }

        if (!avklarTemaVurdering.skalTilAap) {
            return Fullført
        }

        requireNotNull(saksnummervurdering) { "Saksnummer skal være avklart før VideresendSteg" }

        if (saksnummervurdering.generellSak) {
            return Fullført
        }

        val behandling = behandlingRepository.hent(kontekst.behandlingId)

        val dokumentbehandlingId =
            behandlingRepository.opprettBehandling(behandling.journalpostId, TypeBehandling.DokumentHåndtering)
        kopierer.overfør(kontekst.behandlingId, dokumentbehandlingId)

        log.info("Legger til prosesseringsjobb.")
        flytJobbRepository.leggTil(
            JobbInput(ProsesserBehandlingJobbUtfører)
                .forBehandling(behandling.journalpostId.referanse, dokumentbehandlingId.id).medCallId()
        )

        return Fullført
    }

    private fun erJournalførtPåAnnetFagsystem(journalpost: Journalpost): Boolean {
        return journalpost.status == Journalstatus.JOURNALFOERT && journalpost.fagsystem != Fagsystem.KELVIN.name
    }
}
