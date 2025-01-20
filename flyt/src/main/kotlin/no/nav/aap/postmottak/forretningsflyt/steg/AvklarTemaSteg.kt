package no.nav.aap.postmottak.forretningsflyt.steg

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklartema.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklartema.Tema
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklartema.TemaVurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.SaksnummerRepository
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FantAvklaringsbehov
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.gateway.GosysOppgaveGateway
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.steg.StegType
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(AvklarTemaSteg::class.java)

class AvklarTemaSteg(
    private val journalpostRepository: JournalpostRepository,
    private val avklarTemaRepository: AvklarTemaRepository,
    private val gosysOppgaveGateway: GosysOppgaveGateway,
    private val saksnummerRepository: SaksnummerRepository,
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            return AvklarTemaSteg(
                repositoryProvider.provide(JournalpostRepository::class),
                repositoryProvider.provide(AvklarTemaRepository::class),
                GatewayProvider.provide(GosysOppgaveGateway::class),
                repositoryProvider.provide(SaksnummerRepository::class)
            )
        }

        override fun type(): StegType {
            return StegType.AVKLAR_TEMA
        }

    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val journalpost = journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?: error("Journalpost mangler i AvklarTemaSteg")

        if (journalpost.erUgyldig() || journalpost.status == Journalstatus.JOURNALFOERT)
            return Fullført.also {
                log.info(
                    "Journalpost skal ikke behandles - har status ${journalpost.status}"
                )
            }

        val temavurdering = avklarTemaRepository.hentTemaAvklaring(kontekst.behandlingId)

        return if (temavurdering == null) {
            if (journalpost.tema != "AAP") {
                log.info("Journalposten har blitt endret utenfra")
                avklarTemaMaskinelt(kontekst.behandlingId, TemaVurdering(false, Tema.UKJENT))
                Fullført
            } else if (journalpost.erDigitalLegeerklæring() || journalpost.erDigitalSøknad()) {
                avklarTemaMaskinelt(kontekst.behandlingId, journalpost)
                Fullført
            } else {
                FantAvklaringsbehov(Definisjon.AVKLAR_TEMA)
            }
        } else {
            if (venterPåBehandlingIGosys(journalpost, temavurdering)) {
                FantAvklaringsbehov(Definisjon.AVKLAR_TEMA)
            } else if (erFerdigBehandletIGosys(journalpost, temavurdering)) {
                log.info("Journalpost har endret tema. Nytt tema er: ${journalpost.tema}")
                gosysOppgaveGateway.finnOppgaverForJournalpost(journalpost.journalpostId)
                    .forEach { gosysOppgaveGateway.ferdigstillOppgave(it) }
                return Fullført
            } else Fullført
        }
    }

    private fun venterPåBehandlingIGosys(journalpost: Journalpost, temavurdering: TemaVurdering): Boolean {
        return journalpost.tema == "AAP" && temavurdering.tema == Tema.UKJENT
    }

    private fun erFerdigBehandletIGosys(journalpost: Journalpost, temavurdering: TemaVurdering): Boolean {
        return journalpost.tema != "AAP" && temavurdering.tema == Tema.UKJENT
    }

    private fun avklarTemaMaskinelt(behandlingId: BehandlingId, journalpost: Journalpost) {
        if (journalpost.erDigitalLegeerklæring()) {
            if (skalLegeerklæringTilAap(behandlingId)) {
                avklarTemaMaskinelt(behandlingId, TemaVurdering(true, Tema.AAP))
            } else {
                avklarTemaMaskinelt(behandlingId, TemaVurdering(false, Tema.OPP))
            }
        } else if (journalpost.erDigitalSøknad()) {
            avklarTemaMaskinelt(behandlingId, TemaVurdering(true, Tema.AAP))
        } else {
            throw IllegalStateException("Journalpost er ikke en digital søknad eller legeerklæring")
        }
    }

    private fun avklarTemaMaskinelt(behandlingId: BehandlingId, temavurdering: TemaVurdering) {
        avklarTemaRepository.lagreTemaAvklaring(behandlingId, temavurdering.skalTilAap, temavurdering.tema)
    }

    private fun skalLegeerklæringTilAap(behandlingId: BehandlingId): Boolean {
        val kelvinSaker = saksnummerRepository.hentSaksnumre(behandlingId)
        return kelvinSaker.isNotEmpty()
    }
}