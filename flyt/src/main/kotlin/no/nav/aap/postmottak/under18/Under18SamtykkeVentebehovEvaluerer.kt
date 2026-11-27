package no.nav.aap.postmottak.under18

import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.fordeler.InnkommendeJournalpostRepository
import no.nav.aap.postmottak.avklaringsbehov.Avklaringsbehov
import no.nav.aap.postmottak.flyt.ventebehov.SpesifikkVentebehovEvaluerer
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Status

class Under18SamtykkeVentebehovEvaluerer(
    private val repositoryProvider: RepositoryProvider,
) : SpesifikkVentebehovEvaluerer {
    override fun definisjon() = Definisjon.VENT_PA_UNDER18_SAMTYKKE

    override fun ansesSomLøst(kontekst: FlytKontekst, avklaringsbehov: Avklaringsbehov): Boolean {
        val ventSatt = avklaringsbehov.historikk
            .filter { it.status == Status.OPPRETTET }
            .maxOf { it.tidsstempel }

        val journalpost = checkNotNull(repositoryProvider.provide<JournalpostRepository>().hentHvisEksisterer(kontekst.journalpostId)) {
            "Fant ikke hovedjournalposten for ${kontekst.behandlingId}"
        }
        return repositoryProvider.provide<InnkommendeJournalpostRepository>()
            .harNyJournalpostEtter(journalpost.person.identer(), ventSatt, kontekst.journalpostId)
    }
}
