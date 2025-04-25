package no.nav.aap.postmottak.forretningsflyt.steg.journalføring

import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.Tema
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.gateway.JournalføringsGateway
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.postmottak.kontrakt.steg.StegType
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(SettFagsakSteg::class.java)

class SettFagsakSteg(
    private val journalpostRepository: JournalpostRepository,
    private val saksnummerRepository: SaksnummerRepository,
    private val avklarTemaRepository: AvklarTemaRepository,
    private val joarkKlient: JournalføringsGateway
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            return SettFagsakSteg(
                repositoryProvider.provide(JournalpostRepository::class),
                repositoryProvider.provide(SaksnummerRepository::class),
                repositoryProvider.provide(AvklarTemaRepository::class),
                GatewayProvider.provide(JournalføringsGateway::class)
            )
        }

        override fun type(): StegType {
            return StegType.SETT_FAGSAK
        }
    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val journalpost = journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)
        requireNotNull(journalpost)
        if (journalpost.erUgyldig() || journalpost.status == Journalstatus.JOURNALFOERT) return Fullført
        
        val temaavklaring = avklarTemaRepository.hentTemaAvklaring(kontekst.behandlingId)
        requireNotNull(temaavklaring) {
            "Tema skal være avklart før SettFagsakSteg"
        }

        if (temaavklaring.tema == Tema.UKJENT) {
            return Fullført
        }

        val saksvurdering = saksnummerRepository.hentSakVurdering(kontekst.behandlingId)
        requireNotNull(saksvurdering)

        if (saksvurdering.generellSak) {
            joarkKlient.førJournalpostPåGenerellSak(journalpost, temaavklaring.tema.name)
        } else {
            joarkKlient.førJournalpostPåFagsak(
                journalpost.journalpostId,
                journalpost.person.aktivIdent(),
                saksvurdering.saksnummer!!
            )
        }

        return Fullført
    }

}