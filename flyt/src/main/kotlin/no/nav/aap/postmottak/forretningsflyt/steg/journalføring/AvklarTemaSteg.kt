package no.nav.aap.postmottak.forretningsflyt.steg.journalføring

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
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.steg.StegType
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(AvklarTemaSteg::class.java)

class AvklarTemaSteg(
    private val journalpostRepository: JournalpostRepository,
    private val avklarTemaRepository: AvklarTemaRepository,
    private val gosysOppgaveGateway: GosysOppgaveGateway,
    private val saksnummerRepository: SaksnummerRepository
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return AvklarTemaSteg(
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                gatewayProvider.provide(),
                repositoryProvider.provide()
            )
        }

        override fun type(): StegType {
            return StegType.AVKLAR_TEMA
        }

    }

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val journalpost =
            requireNotNull(journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)) { "Journalpost for behandling ${kontekst.behandlingId} mangler i AvklarTemaSteg" }
        if (journalpost.erUgyldig()) return Fullført

        val temavurdering = avklarTemaRepository.hentTemaAvklaring(kontekst.behandlingId)

        return if (temavurdering == null) {
            if (journalpost.tema != "AAP") {
                log.info("Journalposten med ID ${journalpost.journalpostId} har blitt endret utenfra. Tema er ikke AAP.")
                avklarTemaMaskinelt(kontekst.behandlingId, TemaVurdering(false, Tema.UKJENT))
                Fullført
            } else if (journalpost.erDigitalLegeerklæring() || journalpost.erDigitalSøknad() || journalpost.erDigitaltMeldekort()) {
                avklarTemaMaskinelt(kontekst.behandlingId, journalpost)
                Fullført
            } else if (journalpost.status == Journalstatus.JOURNALFOERT) {
                avklarTemaMaskinelt(kontekst.behandlingId, TemaVurdering(true, Tema.AAP))
                log.info("Journalpost har allerede blitt journalført på tema AAP. Setter temaavklaring maskinelt til AAP")
                Fullført
            } else {
                FantAvklaringsbehov(Definisjon.AVKLAR_TEMA)
            }
        } else {
            if (venterPåBehandlingIGosys(journalpost, temavurdering)) { // tema fortsatt AAP
                val aktivIdent = journalpost.person.aktivIdent()
                gosysOppgaveGateway.opprettEndreTemaOppgaveHvisIkkeEksisterer(
                    journalpost.journalpostId,
                    aktivIdent.identifikator
                )

                FantAvklaringsbehov(Definisjon.AVKLAR_TEMA)
            } else if (erFerdigBehandletIGosys(journalpost, temavurdering)) { // har endret tema, fjern denne blokka
                log.info("Journalpost med ID ${journalpost.journalpostId} har endret tema. Nytt tema er: ${journalpost.tema}")
                gosysOppgaveGateway.finnOppgaverForJournalpost(journalpost.journalpostId, tema = "AAP")
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
                log.info("Avklarer tema for legeerklæring ${journalpost.journalpostId} maskinelt - Legeerklæring skal til AAP.")
                avklarTemaMaskinelt(behandlingId, TemaVurdering(skalTilAap = true, Tema.AAP))
            } else {
                log.info("Avklarer tema for legeerklærling ${journalpost.journalpostId} maskinelt - Legeerklæring skal ikke til AAP")
                avklarTemaMaskinelt(behandlingId, TemaVurdering(skalTilAap = false, Tema.OPP))
            }
        } else if (journalpost.erDigitalSøknad()) {
            log.info("Avklarer søknad maskinelt, skal til AAP. JournalpostId ${journalpost.journalpostId}.")
            avklarTemaMaskinelt(behandlingId, TemaVurdering(skalTilAap = true, Tema.AAP))
        } else if (journalpost.erDigitaltMeldekort()) {
            log.info("Avklarer digital meldekort maskinelt. JournalpostId ${journalpost.journalpostId}.")
            avklarTemaMaskinelt(behandlingId, TemaVurdering(skalTilAap = true, Tema.AAP))
        } else {
            error("Journalpost er ikke en digital søknad, legeerklæring eller meldekort. JournalpostId ${journalpost.journalpostId}.")
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