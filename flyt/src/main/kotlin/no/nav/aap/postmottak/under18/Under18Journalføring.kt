package no.nav.aap.postmottak.under18

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.Tema
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.TemaVurdering
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.gateway.Klokke
import no.nav.aap.postmottak.gateway.PersondataGateway
import no.nav.aap.postmottak.gateway.Sakstype
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.unleash.PostmottakFeature
import no.nav.aap.unleash.UnleashGateway

class Under18Journalføring(
    private val under18Repository: Under18Repository,
    private val behandlingRepository: BehandlingRepository,
    private val flytJobbRepository: FlytJobbRepository,
    private val journalpostService: JournalpostService,
    private val behandlingsflytGateway: BehandlingsflytGateway,
    private val persondataGateway: PersondataGateway,
    private val klokke: Klokke,
    private val unleashGateway: UnleashGateway,
) {
    companion object {
        fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) = Under18Journalføring(
            under18Repository = repositoryProvider.provide(),
            behandlingRepository = repositoryProvider.provide(),
            flytJobbRepository = repositoryProvider.provide(),
            journalpostService = JournalpostService.konstruer(repositoryProvider, gatewayProvider),
            behandlingsflytGateway = gatewayProvider.provide(),
            persondataGateway = gatewayProvider.provide(),
            klokke = gatewayProvider.provide(),
            unleashGateway = gatewayProvider.provide(),
        )
    }

    fun erAktuell(journalpost: Journalpost, tema: TemaVurdering?, harKelvinSak: Boolean): Boolean {
        if (harKelvinSak || tema?.tema != Tema.AAP || !tema.skalTilAap) return false
        if (journalpost.hoveddokumentbrevkode != Brevkoder.SØKNAD.kode) return false

        if (!unleashGateway.isEnabled(PostmottakFeature.PostmottakUnder18)) return false

        val person = journalpost.person
        if (under18Repository.finnBehandlingForNyeSøknader(person.id) != null) return true
        val fødselsdato = checkNotNull(persondataGateway.hentFødselsdato(person.aktivIdent().identifikator)) {
            "Mangler fødselsdato ved vurdering av papirsøknad ${journalpost.journalpostId}"
        }
        return klokke.dagensDato().isBefore(fødselsdato.plusYears(18))
    }

    fun videresend(journalpost: Journalpost) {
        val journalpostId = journalpost.journalpostId
        val saf = journalpostService.hentSafJournalpost(journalpostId)
        check(saf.journalstatus == Journalstatus.JOURNALFOERT && saf.tema == Tema.AAP.name && saf.sak?.sakstype == Sakstype.GENERELL_SAK) {
            "Journalpost $journalpostId er ikke ferdigstilt på generell sak med tema AAP."
        }
        check(journalpostService.hentIdenter(saf).any(journalpost.person::er)) {
            "Journalpost $journalpostId tilhører ikke lenger søkeren."
        }

        val person = journalpost.person
        if (under18Repository.hentBehandlingForSøknad(journalpostId) != null) return
        check(behandlingsflytGateway.finnSaker(person.aktivIdent()).isEmpty()) {
            "Kelvin-sak er opprettet etter valg av generell sak for $journalpostId. Krever manuell oppfølging."
        }
        val behandling = under18Repository.finnBehandlingForNyeSøknader(person.id)
            ?: behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.Under18)
                .also { under18Repository.registrerBehandlingForNyeSøknader(person.id, it) }
        under18Repository.lagreSøknad(behandling, journalpostId)
        val hovedjournalpost = behandlingRepository.hent(behandling).journalpostId
        flytJobbRepository.leggTil(
            JobbInput(ProsesserBehandlingJobbUtfører).forBehandling(hovedjournalpost.referanse, behandling.id)
                .medCallId()
        )
    }
}
