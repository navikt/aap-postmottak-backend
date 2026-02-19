package no.nav.aap.postmottak.avklaringsbehov.løser

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
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

class AvklarSakLøser(
    private val saksnummerRepository: SaksnummerRepository,
    private val journalpostRepository: JournalpostRepository,
    private val behandlingsflytGateway: BehandlingsflytGateway,
) : AvklaringsbehovsLøser<AvklarSaksnummerLøsning> {

    companion object : LøserKonstruktør<AvklarSaksnummerLøsning> {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): AvklaringsbehovsLøser<AvklarSaksnummerLøsning> {

            return AvklarSakLøser(
                repositoryProvider.provide(SaksnummerRepository::class),
                repositoryProvider.provide(JournalpostRepository::class),
                gatewayProvider.provide(BehandlingsflytGateway::class),
            )
        }
    }

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarSaksnummerLøsning): LøsningsResultat {
        if (løsning.opprettNySak) {
            if (saksnummerRepository.eksistererAvslagPåTidligereBehandling(kontekst.kontekst.behandlingId)) {
                log.warn("Nytt dokument på sak ${løsning.saksnummer} der førstegangsbehandling endte i avslag. Journalpost: ${kontekst.kontekst.journalpostId.referanse}")
            }
            log.info("Spør behandlingsflyt om å finne eller opprette ny sak")
            avklarFagSakMaskinelt(kontekst.kontekst.behandlingId, løsning)
        } else {
            val saksvurdering = Saksvurdering(
                saksnummer = løsning.saksnummer,
                generellSak = løsning.førPåGenerellSak,
                opprettetNy = false,
                journalposttittel = løsning.journalposttittel,
                avsenderMottaker = løsning.avsenderMottaker,
                dokumenter = løsning.dokumenter
            )
            saksnummerRepository.lagreSakVurdering(kontekst.kontekst.behandlingId, saksvurdering)
        }

        return LøsningsResultat("Dokument er tildelt sak ${løsning.saksnummer}")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SAK
    }

    private fun avklarFagSakMaskinelt(behandlingId: BehandlingId, løsning: AvklarSaksnummerLøsning) {
        val journalpost = journalpostRepository.hentHvisEksisterer(behandlingId)
            ?: error("Fant ikke journalpost for behandling $behandlingId")
        require(journalpost.hoveddokumentbrevkode == "Ukjent" || journalpost.erSøknad()) {
            "Det skal kun være mulig å opprette ny sak for søknad"
        }

        val saksnummer = behandlingsflytGateway.finnEllerOpprettSak(
            Ident(journalpost.person.aktivIdent().identifikator),
            journalpost.mottattDato
        ).saksnummer

        val saksvurdering = Saksvurdering(
            saksnummer = saksnummer,
            generellSak = false,
            opprettetNy = true,
            journalposttittel = løsning.journalposttittel,
            avsenderMottaker = løsning.avsenderMottaker,
            dokumenter = løsning.dokumenter,
        )

        saksnummerRepository.lagreSakVurdering(behandlingId, saksvurdering)
    }
}
