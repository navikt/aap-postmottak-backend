package no.nav.aap.mottak

import no.nav.aap.domene.behandling.BehandlingTjeneste
import no.nav.aap.domene.sak.SakTjeneste
import no.nav.aap.domene.person.PersonTjenesteMock
import no.nav.aap.domene.typer.Ident
import no.nav.aap.domene.typer.Saksnummer
import no.nav.aap.flyt.kontroll.FlytKontekst
import no.nav.aap.flyt.kontroll.FlytKontroller

object HendelsesMottak {

    private val kontroller = FlytKontroller()

    fun håndtere(key: Ident, hendelse: PersonHendelse) {
        val person = PersonTjenesteMock.finnEllerOpprett(key)

        val sak = SakTjeneste.finnEllerOpprett(person, hendelse.periode())

        // Legg til kø for sak, men mocker ved å kalle videre bare

        håndtere(sak.saksnummer, hendelse.tilSakshendelse())
    }

    fun håndtere(key: Saksnummer, hendelse: SakHendelse) {
        val sak = SakTjeneste.hent(key)
        val sisteBehandlingOpt = BehandlingTjeneste.finnSisteBehandlingFor(sak.id)

        val sisteBehandling = if (sisteBehandlingOpt.isPresent && !sisteBehandlingOpt.get().status().erAvsluttet()) {
            sisteBehandlingOpt.get()
        } else {
            // Har ikke behandling så oppretter en
            BehandlingTjeneste.opprettBehandling(sak.id)
        }
        håndtere(key = sisteBehandling.id, hendelse.tilBehandlingHendelse())
    }

    fun håndtere(key: Long, hendelse: LøsAvklaringsbehovBehandlingHendelse) {
        val behandling = BehandlingTjeneste.hent(key)
        kontroller.validerTilstandBehandling(behandling = behandling)

        val sak = SakTjeneste.hent(behandling.sakId)

        val kontekst = FlytKontekst(sakId = sak.id, behandlingId = behandling.id)
        kontroller.løsAvklaringsbehovOgFortsettProsessering(
            kontekst = kontekst,
            avklaringsbehov = listOf(hendelse.behov())
        )
    }

    fun håndtere(key: Long, hendelse: BehandlingHendelse) {
        val behandling = BehandlingTjeneste.hent(key)
        kontroller.validerTilstandBehandling(behandling = behandling)

        val sak = SakTjeneste.hent(behandling.sakId)

        val kontekst = FlytKontekst(sakId = sak.id, behandlingId = behandling.id)
        if (hendelse is LøsAvklaringsbehovBehandlingHendelse) {
            throw IllegalArgumentException("Skal håndteres mellom eksplisitt funksjon")
        } else {
            kontroller.prosesserBehandling(kontekst)
        }
    }
}
