package no.nav.aap.hendelse.mottak

class DokumentMottattSakHendelse() : SakHendelse {

    override fun tilBehandlingHendelse(): BehandlingHendelse {
        return DokumentMottattBehandlingHendelse()
    }
}
