package no.nav.aap.postmottak.klient.arena

data class ArenaOpprettOppgaveForespørsel(
    val fnr : String,
    val enhet : String,
    val tittel : String,
    val titler : List<String> = emptyList())