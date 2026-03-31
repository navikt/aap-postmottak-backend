package no.nav.aap.postmottak.forretningsflyt.steg.journalføring

import no.nav.aap.api.intern.Status
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.Tema
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.gateway.JournalføringService
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.steg.StegType

class SettFagsakSteg(
    private val journalpostRepository: JournalpostRepository,
    private val saksnummerRepository: SaksnummerRepository,
    private val avklarTemaRepository: AvklarTemaRepository,
    private val journalføringService: JournalføringService,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return SettFagsakSteg(
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                JournalføringService(gatewayProvider),
                repositoryProvider.provide(),
            )
        }

        override fun type(): StegType {
            return StegType.SETT_FAGSAK
        }
    }

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val journalpost = requireNotNull(journalpostRepository.hentHvisEksisterer(kontekst.behandlingId))

        if (journalpost.erUgyldig() || journalpost.status == Journalstatus.JOURNALFOERT) return Fullført

        val temaavklaring = requireNotNull(avklarTemaRepository.hentTemaAvklaring(kontekst.behandlingId)) {
            "Tema skal være avklart før SettFagsakSteg"
        }

        if (temaavklaring.tema == Tema.UKJENT) {
            return Fullført
        }

        val saksvurdering = requireNotNull(saksnummerRepository.hentSakVurdering(kontekst.behandlingId))

        val avsenderMottaker = saksvurdering.avsenderMottaker?.takeUnless { journalpost.kanal.erDigitalKanal() }

        val endretAv = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
            .hvemSomLøste(Definisjon.AVKLAR_SAK)


        if (saksvurdering.generellSak) {
            journalføringService.førJournalpostPåGenerellSak(
                journalpost = journalpost,
                tema = temaavklaring.tema.name,
                tittel = saksvurdering.journalposttittel,
                avsenderMottaker = avsenderMottaker,
                dokumenter = saksvurdering.dokumenter,
                endretAv = endretAv,
            )
        } else {
            journalføringService.førJournalpostPåFagsak(
                journalpostId = journalpost.journalpostId,
                ident = journalpost.person.aktivIdent(),
                fagsakId = saksvurdering.saksnummer!!,
                tittel = saksvurdering.journalposttittel,
                avsenderMottaker = avsenderMottaker,
                dokumenter = saksvurdering.dokumenter,
                endretAv = endretAv,
            )
        }

        return Fullført
    }
}