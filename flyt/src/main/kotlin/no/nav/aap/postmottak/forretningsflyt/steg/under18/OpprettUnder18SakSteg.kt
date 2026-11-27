package no.nav.aap.postmottak.forretningsflyt.steg.under18

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksvurdering
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.postmottak.under18.Under18Aldersgruppe
import no.nav.aap.postmottak.under18.Under18Repository
import no.nav.aap.postmottak.under18.Under18Søknadsavklaring
import no.nav.aap.postmottak.under18.Under18Søknadsgrunnlag
import no.nav.aap.postmottak.under18.krevAldersgrunnlag

class OpprettUnder18SakSteg(
    private val under18Repository: Under18Repository,
    private val saker: SaksnummerRepository,
    private val kelvin: BehandlingsflytGateway,
    private val journalpostService: JournalpostService,
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg = OpprettUnder18SakSteg(
            under18Repository = repositoryProvider.provide(),
            saker = repositoryProvider.provide(),
            kelvin = gatewayProvider.provide(),
            journalpostService = JournalpostService.konstruer(repositoryProvider, gatewayProvider),
        )

        override fun type(): StegType = StegType.OPPRETT_UNDER18_SAK
    }

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val id = kontekst.behandlingId
        saker.hentSakVurdering(id)?.let {
            checkNotNull(it.saksnummer)
            return Fullført
        }
        val aldersgruppe = under18Repository.krevAldersgrunnlag(id).aldersgruppe
        val avklaring = under18Repository.hentSøknadsavklaring(id)
        val søknadsgrunnlag = under18Repository.hentSøknadsgrunnlag(id)
        val erHenlagt = avklaring?.erHenlagt(aldersgruppe) == true
        if (!erHenlagt) {
            sjekkAvklaringForSaksopprettelse(id, aldersgruppe, avklaring)
            val saksnummer = opprettKelvinSak(kontekst, søknadsgrunnlag)
            saker.lagreSakVurdering(id, Saksvurdering(saksnummer))
        }
        if (søknadsgrunnlag.tarImotNyeSøknader) {
            under18Repository.lagreSøknadsgrunnlag(id, søknadsgrunnlag.stengForNyeSøknader())
        }
        return Fullført
    }

    private fun sjekkAvklaringForSaksopprettelse(
        id: BehandlingId,
        aldersgruppe: Under18Aldersgruppe,
        avklaring: Under18Søknadsavklaring?,
    ) {
        val gyldigAvklaring = when (aldersgruppe) {
            Under18Aldersgruppe.FOR_UNG -> false
            Under18Aldersgruppe.SAMTYKKE -> under18Repository.hentAktivSamtykkevurdering(id)?.beggeForeldreSamtykket == true
            Under18Aldersgruppe.FYLT_18 -> avklaring?.søkerVilFortsette == true &&
                avklaring.situasjon == aldersgruppe.påkrevdSøknadssituasjon
        }
        check(gyldigAvklaring) { "Mangler gyldig avklaring før saksopprettelse for $id" }
    }

    private fun opprettKelvinSak(kontekst: FlytKontekst, søknadsgrunnlag: Under18Søknadsgrunnlag): String {
        check(kontekst.journalpostId in søknadsgrunnlag.søknader) { "Hovedsøknaden mangler på Under18-behandlingen" }
        val hovedsøknad = journalpostService.hentJournalpost(kontekst.journalpostId)
        søknadsgrunnlag.sjekkEier(hovedsøknad)
        check(hovedsøknad.status == Journalstatus.JOURNALFOERT) { "Hovedsøknaden er ikke ferdigstilt" }
        val ident = hovedsøknad.person.aktivIdent()
        check(kelvin.finnSaker(ident).isEmpty()) { "Uventet Kelvin-sak før saksopprettelse for Under18-behandling ${kontekst.behandlingId}" }
        return kelvin.finnEllerOpprettSak(ident, hovedsøknad.mottattDato).saksnummer
    }
}
