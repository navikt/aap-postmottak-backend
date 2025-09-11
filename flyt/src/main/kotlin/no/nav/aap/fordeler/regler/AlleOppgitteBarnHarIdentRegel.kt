package no.nav.aap.fordeler.regler

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.OppgitteBarn
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.json.DeserializationException
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.gateway.DokumentGateway
import no.nav.aap.postmottak.gateway.DokumentTilMeldingParser
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.BrevkoderHelper
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.slf4j.LoggerFactory

/**
 * Frem til Kelvin har støtte for å håndtere oppgitte barn som mangler ident,
 * må søknader som mangler dette rutes til Arena hvor det kan håndteres manuelt.
 **/
class AlleOppgitteBarnHarIdentRegel : Regel<OppgitteBarnRegelInput> {
    companion object : RegelFactory<OppgitteBarnRegelInput> {
        override val erAktiv = miljøConfig(prod = true, dev = false)

        override fun medDataInnhenting(
            repositoryProvider: RepositoryProvider?,
            gatewayProvider: GatewayProvider?
        ): RegelMedInputgenerator<OppgitteBarnRegelInput> {
            requireNotNull(gatewayProvider)
            requireNotNull(repositoryProvider)

            val journalpostService = JournalpostService.konstruer(repositoryProvider, gatewayProvider)
            val dokumentGateway = gatewayProvider.provide(DokumentGateway::class)

            return RegelMedInputgenerator(
                AlleOppgitteBarnHarIdentRegel(),
                AlleOppgitteBarnHarIdentRegelInputGenerator(journalpostService, dokumentGateway)
            )
        }

    }

    override fun regelNavn(): String = this::class.simpleName!!

    override fun vurder(input: OppgitteBarnRegelInput): Boolean {
        val oppgitteBarn = input.oppgitteBarn?.barn.orEmpty()

        return oppgitteBarn.none { it.ident == null }
    }

}

class AlleOppgitteBarnHarIdentRegelInputGenerator(
    private val journalpostService: JournalpostService,
    private val dokumentGateway: DokumentGateway,
) : InputGenerator<OppgitteBarnRegelInput> {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun generer(input: RegelInput): OppgitteBarnRegelInput {
        val journalpost = journalpostService
            .hentJournalpost(JournalpostId(input.journalpostId))

        return if (journalpost.erDigitalSøknad()) {
            val strukturertDokument = requireNotNull(journalpost.finnOriginal()) {
                "Journalpost er digital søknad, men mangler strukturert dokument"
            }

            val dokumentBytes = dokumentGateway
                .hentDokument(journalpost.journalpostId, strukturertDokument.dokumentInfoId)
                .dokument
                .readBytes()

            val innsending = BrevkoderHelper.mapTilInnsendingType(journalpost.hoveddokumentbrevkode)

            val søknad = try {
                DokumentTilMeldingParser.parseTilMelding(dokumentBytes, innsending) as SøknadV0
            } catch (e: DeserializationException) {
                if (e.cause?.message?.contains("Ugyldig identifikator") == true) {
                    log.warn("Feil i parsing av identer fra søknad. Returnerer at ident mangler", e)
                    return OppgitteBarnRegelInput(null)
                } else {
                    throw e
                }
            }

            OppgitteBarnRegelInput(søknad.oppgitteBarn)
        } else {
            OppgitteBarnRegelInput(null)
        }
    }

}

data class OppgitteBarnRegelInput(
    val oppgitteBarn: OppgitteBarn?,
)
