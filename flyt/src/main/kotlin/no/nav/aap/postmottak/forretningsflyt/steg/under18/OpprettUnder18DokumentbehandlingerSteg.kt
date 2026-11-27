package no.nav.aap.postmottak.forretningsflyt.steg.under18

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksvurdering
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.gateway.DokarkivGateway
import no.nav.aap.postmottak.gateway.Fagsystem
import no.nav.aap.postmottak.gateway.JournalpostGateway
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.gateway.MASKINELL_JOURNALFØRING_ENHET
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.postmottak.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.postmottak.under18.Under18Repository
import no.nav.aap.postmottak.under18.finnPåKelvinSak

class OpprettUnder18DokumentbehandlingerSteg(
    private val under18Repository: Under18Repository,
    private val saker: SaksnummerRepository,
    private val behandlinger: BehandlingRepository,
    private val journalposter: JournalpostRepository,
    private val jobber: FlytJobbRepository,
    private val journalpostService: JournalpostService,
    private val saf: JournalpostGateway,
    private val dokarkiv: DokarkivGateway,
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg = OpprettUnder18DokumentbehandlingerSteg(
            under18Repository = repositoryProvider.provide(),
            saker = repositoryProvider.provide(),
            behandlinger = repositoryProvider.provide(),
            journalposter = repositoryProvider.provide(),
            jobber = repositoryProvider.provide(),
            journalpostService = JournalpostService.konstruer(repositoryProvider, gatewayProvider),
            saf = gatewayProvider.provide(),
            dokarkiv = gatewayProvider.provide(),
        )

        override fun type(): StegType = StegType.OPPRETT_UNDER18_DOKUMENTBEHANDLINGER
    }

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val sak = saker.hentSakVurdering(kontekst.behandlingId)?.saksnummer
        if (sak == null) {
            check(under18Repository.hentSøknadsavklaring(kontekst.behandlingId)?.søkerVilFortsette == false) {
                "Mangler Kelvin-sak for ${kontekst.behandlingId}"
            }
            return Fullført
        }
        val grunnlag = under18Repository.hentSøknadsgrunnlag(kontekst.behandlingId)
        check(kontekst.journalpostId in grunnlag.søknader) { "Hovedsøknaden mangler på Under18-behandlingen" }
        val kilder = grunnlag.søknader.map(journalpostService::hentJournalpost)
        kilder.forEach(grunnlag::sjekkEier)
        check(kilder.none { it.erUgyldig() }) { "Journalpostene må være gyldige" }
        check(kilder.all { it.status == Journalstatus.JOURNALFOERT }) { "Søknaden er ikke ferdigstilt" }
        kilder.sortedWith(compareBy(
            { it.mottattTid ?: it.mottattDato.atStartOfDay() }, { it.journalpostId.referanse }
        )).forEach { kilde -> opprettDokumentbehandling(finnEllerKnyttTil(kilde, sak), sak) }
        return Fullført
    }

    private fun finnEllerKnyttTil(kilde: Journalpost, saksnummer: String): Journalpost {
        val dokumentIder = kilde.dokumenter.map { it.dokumentInfoId.dokumentInfoId }.toSet()
        val nyId = saf.hentTilknyttedeJournalposter(kilde.hoveddokument.dokumentInfoId)
            .finnPåKelvinSak(kilde.journalpostId, dokumentIder, saksnummer)
            ?: dokarkiv.knyttTilAnnenSak(kilde, saksnummer, MASKINELL_JOURNALFØRING_ENHET)
        check(nyId != kilde.journalpostId) { "Dokarkiv returnerte kildejournalposten ved tilknytning" }
        val ny = journalpostService.hentJournalpost(nyId)
        check(ny.person.id == kilde.person.id && ny.saksnummer == saksnummer && ny.fagsystem == Fagsystem.KELVIN.name &&
            ny.status == Journalstatus.JOURNALFOERT) { "Uventede opplysninger på tilknyttet journalpost $nyId" }
        return ny
    }

    private fun opprettDokumentbehandling(ny: Journalpost, sak: String) {
        val nyId = ny.journalpostId
        val eksisterende = behandlinger.hentAlleBehandlingerForJournalpost(nyId)
            .filter { it.typeBehandling == TypeBehandling.DokumentHåndtering }
        check(eksisterende.size <= 1) { "Flere dokumentbehandlinger for journalpost $nyId" }
        if (eksisterende.isNotEmpty()) return
        journalposter.lagre(ny)
        val id = behandlinger.opprettBehandling(nyId, TypeBehandling.DokumentHåndtering)
        saker.lagreSakVurdering(id, Saksvurdering(sak))
        jobber.leggTil(JobbInput(ProsesserBehandlingJobbUtfører).forBehandling(nyId.referanse, id.id).medCallId())
    }
}
