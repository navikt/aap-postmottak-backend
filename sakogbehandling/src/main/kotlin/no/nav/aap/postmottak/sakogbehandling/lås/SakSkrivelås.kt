package no.nav.aap.postmottak.sakogbehandling.lås

import no.nav.aap.verdityper.sakogbehandling.SakId

data class SakSkrivelås(val id: SakId, val versjon: Long)
