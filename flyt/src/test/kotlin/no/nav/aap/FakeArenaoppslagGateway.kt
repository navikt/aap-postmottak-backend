package no.nav.aap

import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaVedtak
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakMedSisteVedtakOgMaksdato
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SignifikantHistorikkResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.VedtakMedMaksdato
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.postmottak.gateway.ArenaoppslagGateway
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import java.time.LocalDate

class FakeArenaoppslagGateway : ArenaoppslagGateway {
    companion object : Factory<FakeArenaoppslagGateway> {
        override fun konstruer(): FakeArenaoppslagGateway {
            return FakeArenaoppslagGateway()
        }

        const val identHeltUtenSak = "ikke_funnet"
        const val identMedSak = "0000000333"
        const val identMedSignifikantSak = "09876543210"
    }

    override suspend fun harHistorikk(person: Person): Boolean {
        return person.identer().first().identifikator == identMedSignifikantSak
    }

    override suspend fun harSignifikantHistorikk(
        person: Person,
        mottattDato: LocalDate
    ): SignifikantHistorikkResponse {
        return if (person.identer().first().identifikator == identMedSignifikantSak) {
            SignifikantHistorikkResponse(
                true, listOf(
                    ArenaVedtak(
                        1234, "AKTIV", null, null, null, "AAP", null
                    )
                )
            )
        } else {
            SignifikantHistorikkResponse.ingen
        }
    }

    override suspend fun sisteVedtakMedMaksdato(ident: Ident): SakMedSisteVedtakOgMaksdato {
        return SakMedSisteVedtakOgMaksdato(
            sakId = 1234,
            saknummer = "2025-11",
            sakStatus = "AKTIV",
            sakRegistrert = LocalDate.of(2025, 1, 1),
            sakAvsluttet = LocalDate.of(2026, 12, 12),
            unntaksvilkaarGjelderFra = null,
            unntaksvilkaarInnvilget = null,
            sisteVedtak = VedtakMedMaksdato(
                vedtakId = 1,
                aktfaseKode = "INNV",
                vedtaktypeKode = "O",
                fra = LocalDate.of(2025, 1, 1),
                til = LocalDate.of(2026, 1, 1),
                maxdatoOrdinaer = LocalDate.of(2026, 12, 12),
                maxdatoUnntak = null,
                maxdatoAap = LocalDate.of(2026, 12, 12),

                )
        )
    }

    override suspend fun sisteUtbetalingsdatoForPerson(ident: Ident): LocalDate? {
        return LocalDate.of(2026, 12, 12)
    }
}
