package no.nav.aap.behandlingsflyt.mottak

import no.nav.aap.behandlingsflyt.mottak.pliktkort.Pliktkort
import no.nav.aap.behandlingsflyt.sak.SakId

class MottaDokumentService(private val mottattDokumentRepository: MottattDokumentRepository) {

    fun håndter(sakId: SakId, pliktKort: Pliktkort) {
        // TODO: Lagre innslag i mottatt dokument

        // Lagre data knyttet til sak

        // Trigge hendelse på sak som vil innhente til rett behandling (hvis det er en åpen behandling)
    }
}