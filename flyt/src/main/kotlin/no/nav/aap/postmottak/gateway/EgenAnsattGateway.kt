package no.nav.aap.postmottak.gateway

import no.nav.aap.lookup.gateway.Gateway
import no.nav.aap.postmottak.journalpostogbehandling.Ident

interface EgenAnsattGateway: Gateway {
    fun erEgenAnsatt(ident: Ident): Boolean
}