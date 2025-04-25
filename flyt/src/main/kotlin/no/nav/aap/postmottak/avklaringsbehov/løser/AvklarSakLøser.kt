package no.nav.aap.postmottak.avklaringsbehov.løser

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.AvslagException
import no.nav.aap.postmottak.avklaringsbehov.løsning.AvklarSaksnummerLøsning
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksvurdering
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(AvklarSakLøser::class.java)

class AvklarSakLøser(val connection: DBConnection) : AvklaringsbehovsLøser<AvklarSaksnummerLøsning> {

    private val repositoryProvider = RepositoryProvider(connection)
    private val gatewayProvider = GatewayProvider
    private val saksnummerRepository = repositoryProvider.provide(SaksnummerRepository::class)
    private val journalpostRepository = repositoryProvider.provide(JournalpostRepository::class)
    private val behandlingsflytGateway = gatewayProvider.provide(BehandlingsflytGateway::class)

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarSaksnummerLøsning): LøsningsResultat {
        if (løsning.opprettNySak) {
            if (saksnummerRepository.eksistererAvslagPåTidligereBehandling(kontekst.kontekst.behandlingId)) throw AvslagException()

            log.info("Spør behandlingsflyt om å finne eller opprette ny sak")
            avklarFagSakMaskinelt(kontekst.kontekst.behandlingId)
        } else {
            val saksvurdering = Saksvurdering(løsning.saksnummer, løsning.førPåGenerellSak)
            saksnummerRepository.lagreSakVurdering(kontekst.kontekst.behandlingId, saksvurdering)
        }

        return LøsningsResultat("Dokument er tildelt sak ${løsning.saksnummer}")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SAK
    }

    private fun avklarFagSakMaskinelt(behandlingId: BehandlingId) {
        val journalpost = journalpostRepository.hentHvisEksisterer(behandlingId)
            ?: error("Fant ikke journalpost for behandling $behandlingId")
        require(journalpost.hoveddokumentbrevkode == "Ukjent" || journalpost.erSøknad()) {
            "Det skal kun være mulig å opprette ny sak for søknad"
        }
        val saksnummer = behandlingsflytGateway.finnEllerOpprettSak(
            Ident(journalpost.person.aktivIdent().identifikator),
            journalpost.mottattDato()
        ).saksnummer
        saksnummerRepository.lagreSakVurdering(behandlingId, Saksvurdering(saksnummer,  false, opprettetNy = true))
    }
}