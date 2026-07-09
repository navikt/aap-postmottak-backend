package no.nav.aap.postmottak.avklaringsbehov.løser

import no.nav.aap.fordeler.InnkommendeJournalpostRepository
import no.nav.aap.fordeler.arena.ArenaVideresender
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.løsning.VurderOpprettelseAvSakLøsning
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksvurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.VurderOpprettelseAvSakRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.VurderOpprettelseAvSakVurdering
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.VurderOpprettelseAvSakValg
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(VurderOpprettelseAvSakLøser::class.java)

/**
 * Løser det manuelle avklaringsbehovet [Definisjon.VURDER_OPPRETTELSE_AV_SAK] der saksbehandler har
 * vurdert hvor søknaden skal behandles videre.
 *
 * - [VurderOpprettelseAvSakValg.KELVIN]: oppretter ny sak i Kelvin, som ved ordinær fordeling.
 * - [VurderOpprettelseAvSakValg.ARENA]: videresender journalposten til Arena, som ved ordinær fordeling.
 * - [VurderOpprettelseAvSakValg.BEGGE]: ikke støttet i backend enda.
 */
class VurderOpprettelseAvSakLøser(
    private val saksnummerRepository: SaksnummerRepository,
    private val journalpostRepository: JournalpostRepository,
    private val behandlingsflytGateway: BehandlingsflytGateway,
    private val vurderOpprettelseAvSakRepository: VurderOpprettelseAvSakRepository,
    private val innkommendeJournalpostRepository: InnkommendeJournalpostRepository,
    private val arenaVideresenderFactory: () -> ArenaVideresender,
) : AvklaringsbehovsLøser<VurderOpprettelseAvSakLøsning> {

    companion object : LøserKonstruktør<VurderOpprettelseAvSakLøsning> {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): AvklaringsbehovsLøser<VurderOpprettelseAvSakLøsning> {
            return VurderOpprettelseAvSakLøser(
                repositoryProvider.provide(SaksnummerRepository::class),
                repositoryProvider.provide(JournalpostRepository::class),
                gatewayProvider.provide(BehandlingsflytGateway::class),
                repositoryProvider.provide(VurderOpprettelseAvSakRepository::class),
                repositoryProvider.provide(InnkommendeJournalpostRepository::class),
                // Bygges lazy slik at konstruksjon av løseren holder seg billig (ArenaVideresender drar inn flere gateways).
                arenaVideresenderFactory = { ArenaVideresender.konstruer(repositoryProvider, gatewayProvider) },
            )
        }
    }

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: VurderOpprettelseAvSakLøsning
    ): LøsningsResultat {
        val behandlingId = kontekst.kontekst.behandlingId

        vurderOpprettelseAvSakRepository.lagre(
            behandlingId,
            VurderOpprettelseAvSakVurdering(valg = løsning.valg, begrunnelse = løsning.begrunnelse)
        )

        return when (løsning.valg) {
            VurderOpprettelseAvSakValg.KELVIN -> {
                val saksnummer = opprettSakIKelvin(behandlingId)
                LøsningsResultat("Ny sak ($saksnummer) opprettet i Kelvin")
            }

            VurderOpprettelseAvSakValg.ARENA -> {
                routeTilArena(kontekst.kontekst.journalpostId)
                LøsningsResultat("Søknad videresendt til Arena")
            }

            VurderOpprettelseAvSakValg.BEGGE ->
                throw NotImplementedError("Valget BEGGE er ikke støttet i backend enda.")

            null ->
                error("Mangler valg i løsning for VURDER_OPPRETTELSE_AV_SAK. BehandlingId=$behandlingId")
        }
    }

    override fun forBehov(): Definisjon {
        return Definisjon.VURDER_OPPRETTELSE_AV_SAK
    }

    private fun routeTilArena(journalpostId: JournalpostId) {
        val innkommendeJournalpostId = requireNotNull(innkommendeJournalpostRepository.hentId(journalpostId)) {
            "Fant ikke innkommende journalpost for $journalpostId ved videresending til Arena"
        }
        log.info("Saksbehandler valgte Arena. Videresender journalpost $journalpostId til Arena.")
        arenaVideresenderFactory().videresendJournalpostTilArena(journalpostId, innkommendeJournalpostId)
    }

    private fun opprettSakIKelvin(behandlingId: BehandlingId): String {
        val journalpost = journalpostRepository.hentHvisEksisterer(behandlingId)
            ?: error("Fant ikke journalpost for behandling $behandlingId")

        val saksnummer = behandlingsflytGateway.finnEllerOpprettSak(
            Ident(journalpost.person.aktivIdent().identifikator),
            journalpost.mottattDato
        ).saksnummer

        saksnummerRepository.lagreSakVurdering(
            behandlingId,
            Saksvurdering(saksnummer = saksnummer, generellSak = false, opprettetNy = true)
        )

        return saksnummer
    }
}

