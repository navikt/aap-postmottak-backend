package no.nav.aap.postmottak.klient.statistikk

import no.nav.aap.postmottak.kontrakt.hendelse.DokumentflytStoppetHendelse

interface Statistikklient {
    fun avlever(hendelse: DokumentflytStoppetHendelse)
}