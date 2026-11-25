package no.nav.aap.postmottak.avklaringsbehov.løser

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.httpklient.exception.VerdiIkkeFunnetException
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.postmottak.avklaringsbehov.løsning.AvklarUnder18SamtykkeLøsning
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.gateway.BrukerIdType
import no.nav.aap.postmottak.gateway.Klokke
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.under18.Under18Aldersgruppe
import no.nav.aap.postmottak.under18.Under18Repository
import no.nav.aap.postmottak.under18.Under18SamtykkeVurdering
import no.nav.aap.postmottak.under18.hentFødselsdatoFor

class AvklarUnder18SamtykkeLøser(
    private val under18Repository: Under18Repository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val klokke: Klokke,
    private val journalpostRepository: JournalpostRepository,
    private val journalpostService: JournalpostService,
) : AvklaringsbehovsLøser<AvklarUnder18SamtykkeLøsning> {

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarUnder18SamtykkeLøsning): LøsningsResultat {
        val samtykkeJournalpostIder = løsning.samtykkeJournalpostIder.map(::JournalpostId)
        validerSamtykkereferanser(kontekst, samtykkeJournalpostIder)

        under18Repository.lagreSamtykkevurdering(
            kontekst.kontekst.behandlingId,
            Under18SamtykkeVurdering(
                beggeForeldreSamtykket = løsning.beggeForeldreSamtykket,
                samtykkeJournalpostIder = samtykkeJournalpostIder,
            )
        )

        if (!løsning.beggeForeldreSamtykket) {
            val fødselsdato = under18Repository.hentFødselsdatoFor(kontekst.kontekst.behandlingId)
            if (Under18Aldersgruppe.vurder(fødselsdato, klokke.dagensDato()) == Under18Aldersgruppe.SAMTYKKE) {
                val vent = Definisjon.VENT_PA_UNDER18_SAMTYKKE
                avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.kontekst.behandlingId).leggTil(
                    definisjon = vent,
                    stegType = vent.løsesISteg,
                    grunn = ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER,
                    frist = minOf(vent.utledFrist(null), fødselsdato.plusYears(18)),
                )
            }
        }

        return LøsningsResultat(
            if (løsning.beggeForeldreSamtykket) "Begge foreldre har samtykket" else "Samtykke mangler"
        )
    }

    override fun forBehov(): Definisjon = Definisjon.AVKLAR_UNDER18_SAMTYKKE

    private fun validerSamtykkereferanser(
        kontekst: AvklaringsbehovKontekst,
        journalpostIder: List<JournalpostId>,
    ) {
        if (journalpostIder.isEmpty()) return

        val person = checkNotNull(journalpostRepository.hentHvisEksisterer(kontekst.kontekst.journalpostId)?.person) {
            "Fant ikke person for hovedjournalposten til behandling ${kontekst.kontekst.behandlingId}"
        }
        journalpostIder.forEach { journalpostId ->
            val journalpost = try {
                journalpostService.hentSafJournalpost(journalpostId)
            } catch (_: VerdiIkkeFunnetException) {
                throw UgyldigForespørselException("Fant ikke journalposten som er oppgitt som samtykke.")
            }
            if (journalpost.bruker?.id.isNullOrBlank() ||
                journalpost.bruker?.type !in setOf(BrukerIdType.FNR, BrukerIdType.AKTOERID) ||
                journalpostService.hentIdenter(journalpost).none(person::er)
            ) {
                throw UgyldigForespørselException("Journalposten som er oppgitt som samtykke, tilhører ikke søkeren.")
            }
        }
    }

    companion object : LøserKonstruktør<AvklarUnder18SamtykkeLøsning> {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): AvklaringsbehovsLøser<AvklarUnder18SamtykkeLøsning> =
            AvklarUnder18SamtykkeLøser(
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                gatewayProvider.provide(),
                repositoryProvider.provide(),
                JournalpostService.konstruer(repositoryProvider, gatewayProvider),
            )
    }
}
