package no.nav.aap.postmottak.forretningsflyt.steg.journalføring

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksvurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.Tema
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FantAvklaringsbehov
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway
import no.nav.aap.postmottak.gateway.Fagsystem
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.steg.StegType
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(AvklarSakSteg::class.java)

class AvklarSakSteg(
    private val saksnummerRepository: SaksnummerRepository,
    private val journalpostRepository: JournalpostRepository,
    private val behandlingsflytClient: BehandlingsflytGateway,
    private val avklarTemaRepository: AvklarTemaRepository
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            return AvklarSakSteg(
                repositoryProvider.provide(SaksnummerRepository::class),
                repositoryProvider.provide(JournalpostRepository::class),
                GatewayProvider.provide(BehandlingsflytGateway::class),
                repositoryProvider.provide(AvklarTemaRepository::class)
            )
        }

        override fun type(): StegType {
            return StegType.AVKLAR_SAK
        }

    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        /* 
         * TODO: Når vi skal tillate at person kan ha Arena-historikk må vi avklare manuelt dersom det finnes sak både i Arena og Kelvin. 
         * Må da lagre ned fagsystem i saksvurdering. Hvis fagsystem er Arena: opprett en jobb for ArenaVideresending,
         * og no-op resten av denne journalføringsflyten basert på fagsystem
         */
        val journalpost = journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)
        requireNotNull(journalpost)
        if (journalpost.erUgyldig()) return Fullført

        val temavurdering = avklarTemaRepository.hentTemaAvklaring(kontekst.behandlingId)
        requireNotNull(temavurdering) { "Tema skal være avklart før AvklarSakSteg" }

        if (temavurdering.tema == Tema.UKJENT) {
            return Fullført
        } else if (temavurdering.tema == Tema.OPP) {
            avklarGenerellSakMaskinelt(kontekst.behandlingId)
            return Fullført
        } else if (journalpost.status == Journalstatus.JOURNALFOERT) {
            if (journalpost.fagsystem != Fagsystem.KELVIN.name) {
                log.info("Journalpost har blitt journalført på annet fagsystem, går videre til neste steg")
                return Fullført
            }
            log.info("Journalpost har alt blitt journalført på Kelvin-sak. Setter saksavklaring tilsvarende journalpost")
            saksnummerRepository.lagreSakVurdering(
                kontekst.behandlingId,
                Saksvurdering(saksnummer = journalpost.saksnummer.toString())
            )
            return Fullført
        }

        val saksnummerVurdering = saksnummerRepository.hentSakVurdering(kontekst.behandlingId)

        return if (journalpost.erDigitalSøknad() || journalpost.erDigitalLegeerklæring() || journalpost.erDigitaltMeldekort()) {
            avklarFagSakMaskinelt(kontekst.behandlingId, journalpost)
            Fullført
        } else if (saksnummerVurdering != null) {
            Fullført
        } else {
            return FantAvklaringsbehov(
                Definisjon.AVKLAR_SAK
            )
        }
    }

    private fun avklarFagSakMaskinelt(behandlingId: BehandlingId, journalpost: Journalpost) {
        val saksnummer = behandlingsflytClient.finnEllerOpprettSak(
            Ident(journalpost.person.aktivIdent().identifikator),
            journalpost.mottattDato()
        ).saksnummer
        saksnummerRepository.lagreSakVurdering(behandlingId, Saksvurdering(saksnummer, false))
    }

    private fun avklarGenerellSakMaskinelt(behandlingId: BehandlingId) {
        saksnummerRepository.lagreSakVurdering(behandlingId, Saksvurdering(null, true))
    }
}