package no.nav.aap.postmottak.forretningsflyt.steg.journalføring

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.Tema
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.TemaVurdering
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
        if (journalpost.erUgyldig()) return Fullført

        val temavurdering = avklarTemaRepository.hentTemaAvklaring(kontekst.behandlingId)

        return if (temavurdering == null) {
            if (journalpost.tema != "AAP") {
                log.info("Journalposten har blitt endret utenfra")
                avklarTemaMaskinelt(kontekst.behandlingId, TemaVurdering(false, Tema.UKJENT))
                Fullført
            } else if (journalpost.erDigitalLegeerklæring() || journalpost.erDigitalSøknad() || journalpost.erDigitaltMeldekort()) {
                avklarTemaMaskinelt(kontekst.behandlingId, journalpost)
                Fullført
            } else if (journalpost.status == Journalstatus.JOURNALFOERT) {
                avklarTemaMaskinelt(kontekst.behandlingId, TemaVurdering(true, Tema.AAP))
                log.info("Journalpost har alt blitt journalført på tema AAP. Setter temaavklaring maskinelt til AAP")
                Fullført
            } else {
                FantAvklaringsbehov(Definisjon.AVKLAR_TEMA)
            }
        } else {
            if (venterPåBehandlingIGosys(journalpost, temavurdering)) {
                log.info("Oppretter oppgave i Gosys for journalpost ${journalpost.journalpostId}")

                gosysOppgaveGateway.opprettEndreTemaOppgaveHvisIkkeEksisterer(
                    journalpostId = journalpost.journalpostId,
                    personident = journalpost.person.aktivIdent().identifikator
                )
            }

            Fullført
        }
    }

    private fun venterPåBehandlingIGosys(journalpost: Journalpost, temavurdering: TemaVurdering): Boolean {
        return journalpost.tema == "AAP" && temavurdering.tema == Tema.UKJENT
    }

    private fun avklarTemaMaskinelt(behandlingId: BehandlingId, journalpost: Journalpost) {
        if (journalpost.erDigitalLegeerklæring()) {
            if (skalLegeerklæringTilAap(behandlingId)) {
                log.info("Avklarer maskinelt - Legeerklæring skal til AAP")
                avklarTemaMaskinelt(behandlingId, TemaVurdering(skalTilAap = true, Tema.AAP))
            } else {
                log.info("Avklarer maskinelt - Legeerklæring skal ikke til AAP")
                avklarTemaMaskinelt(behandlingId, TemaVurdering(skalTilAap = false, Tema.OPP))
            }
        } else if (journalpost.erDigitalSøknad()) {
            log.info("Avklarer maskinelt - Legeerklæring skal til AAP")
            avklarTemaMaskinelt(behandlingId, TemaVurdering(skalTilAap = true, Tema.AAP))
        } else if (journalpost.erDigitaltMeldekort()) {
            avklarTemaMaskinelt(behandlingId, TemaVurdering(skalTilAap = true, Tema.AAP))
        } else {
            throw IllegalStateException("Journalpost er ikke en digital søknad, legeerklæring eller meldekort")
        }
    }

    private fun avklarTemaMaskinelt(behandlingId: BehandlingId, temavurdering: TemaVurdering) {
        avklarTemaRepository.lagreTemaAvklaring(behandlingId, temavurdering.skalTilAap, temavurdering.tema)
    }

    private fun skalLegeerklæringTilAap(behandlingId: BehandlingId): Boolean {
        val kelvinSaker = saksnummerRepository.hentKelvinSaker(behandlingId)
        return kelvinSaker.isNotEmpty()
    }
}