package no.nav.aap.postmottak.klient.gosysoppgave

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.get
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PatchRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.lookup.gateway.Factory
import no.nav.aap.postmottak.JournalføringsType
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.gateway.GosysOppgaveGateway
import no.nav.aap.postmottak.gateway.Oppgavetype
import no.nav.aap.postmottak.gateway.Statuskategori
import no.nav.aap.postmottak.journalføringCounter
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.slf4j.LoggerFactory
import java.net.URI


private val log = LoggerFactory.getLogger(GosysOppgaveKlient::class.java)

class GosysOppgaveKlient(val prometheus: MeterRegistry = SimpleMeterRegistry()) : GosysOppgaveGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.oppgaveapi.url"))
    val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.oppgaveapi.scope"),
    )
    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )

    companion object : Factory<GosysOppgaveKlient> {
        override fun konstruer(): GosysOppgaveKlient {
            return GosysOppgaveKlient(PrometheusProvider.prometheus)
        }
    }

    override fun opprettEndreTemaOppgaveHvisIkkeEksisterer(journalpostId: JournalpostId, personident: String) {
        opprettOppgaveHvisIkkeEksisterer(
            OpprettOppgaveRequest(
                oppgavetype = Oppgavetype.JOURNALFØRING.verdi,
                journalpostId = journalpostId.toString(),
                personident = personident,
                fristFerdigstillelse = finnStandardOppgavefrist(),
                beskrivelse = "Et dokument med feil tema har dukket opp hos AAP. Hjelp dokumentet på veien til sin rette mottaker"
            )
        )
    }

    private fun opprettOppgaveHvisIkkeEksisterer(oppgaveRequest: OpprettOppgaveRequest) {
        val oppgaver = finnOppgaverForJournalpost(
            JournalpostId(oppgaveRequest.journalpostId.toLong()),
            listOf(Oppgavetype.JOURNALFØRING, Oppgavetype.FORDELING),
            "AAP",
            Statuskategori.AAPEN
        )

        if (oppgaver.isNotEmpty()) {
            log.info("Åpen oppgave for journalpost ${oppgaveRequest.journalpostId} finnes allerede - oppretter ingen ny")
            return
        }

        log.info("Oppretter oppave (${oppgaveRequest.oppgavetype}) for journalpost ${oppgaveRequest.journalpostId} i gosys")

        val path = url.resolve("/api/v1/oppgaver")
        val request = PostRequest(oppgaveRequest)

        try {
            client.post(path, request) { _, _ -> }
        } catch (e: Exception) {
            log.warn("Feil mot oppgaveApi under opprettelse av oppgave: ${e.message}", e)
            throw e
        }

        if (oppgaveRequest.oppgavetype == Oppgavetype.JOURNALFØRING.verdi) {
            prometheus.journalføringCounter(type = JournalføringsType.jfr, enhet = oppgaveRequest.tildeltEnhetsnr)
                .increment()
        } else if (oppgaveRequest.oppgavetype == Oppgavetype.FORDELING.verdi) {
            prometheus.journalføringCounter(type = JournalføringsType.fdr).increment()
        }
    }

    override fun finnOppgaverForJournalpost(
        journalpostId: JournalpostId, oppgavetyper: List<Oppgavetype>, tema: String, statuskategori: Statuskategori
    ): List<Long> {
        log.info("Finn oppgaver for journalpost: $journalpostId")
        val oppgaveparams = oppgavetyper.map { "&oppgavetype=${it.verdi}" }.joinToString(separator = "")
        val path =
            url.resolve("/api/v1/oppgaver?journalpostId=$journalpostId${oppgaveparams}&tema=$tema&statuskategori=${statuskategori.name}")

        return client.get<FinnOppgaverResponse>(path, GetRequest())?.oppgaver?.map { it.id } ?: emptyList()
    }

    override fun ferdigstillOppgave(oppgaveId: Long) {
        log.info("Ferdigstiller oppgave $oppgaveId")
        val path = url.resolve("/api/v1/oppgaver/$oppgaveId")

        val request = PatchRequest(FerdigstillOppgaveRequest())

        try {
            client.patch(path, request) { _, _ -> }
        } catch (e: Exception) {
            log.warn("Feil mot oppgaveApi under lukking av oppgave: ${e.message}", e)
        }
    }

    override fun opprettJournalføringsOppgaveHvisIkkeEksisterer(
        journalpostId: JournalpostId, personIdent: Ident, beskrivelse: String, tildeltEnhetsnr: String
    ) {
        opprettOppgaveHvisIkkeEksisterer(
            OpprettOppgaveRequest(
                oppgavetype = Oppgavetype.JOURNALFØRING.verdi,
                journalpostId = journalpostId.toString(),
                personident = personIdent.identifikator,
                beskrivelse = beskrivelse,
                tildeltEnhetsnr = tildeltEnhetsnr,
                fristFerdigstillelse = finnStandardOppgavefrist()
            )
        )
    }

    override fun opprettFordelingsOppgaveHvisIkkeEksisterer(
        journalpostId: JournalpostId,
        orgnr: String?,
        personIdent: Ident?,
        beskrivelse: String
    ) =
        opprettOppgaveHvisIkkeEksisterer(
            OpprettOppgaveRequest(
                oppgavetype = Oppgavetype.FORDELING.verdi,
                journalpostId = journalpostId.toString(),
                personident = personIdent?.identifikator,
                orgnr = orgnr,
                beskrivelse = beskrivelse,
                fristFerdigstillelse = finnStandardOppgavefrist()
            )
        )
}

data class FinnOppgaverResponse(
    val oppgaver: List<Oppgave>
)

data class Oppgave(
    val id: Long,
)