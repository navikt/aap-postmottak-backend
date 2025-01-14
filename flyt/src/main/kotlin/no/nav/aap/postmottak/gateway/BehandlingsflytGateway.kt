package no.nav.aap.postmottak.gateway

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Melding
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.gateway.Gateway
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import java.time.LocalDate

interface BehandlingsflytGateway: Gateway {
    fun finnEllerOpprettSak(ident: Ident, mottattDato: LocalDate): BehandlingsflytSak
    fun finnSaker(ident: Ident): List<BehandlingsflytSak>
    fun sendHendelse(journalpost: Journalpost, innsendingstype: InnsendingType, saksnummer: String, melding: Melding?)
}

data class BehandlingsflytSak(
    val saksnummer: String,
    val periode: Periode,
)
