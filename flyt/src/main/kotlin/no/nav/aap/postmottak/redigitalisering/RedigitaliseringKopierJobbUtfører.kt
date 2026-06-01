package no.nav.aap.postmottak.redigitalisering

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon

class RedigitaliseringKopierJobbUtfører(
) : JobbUtfører {
    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return RedigitaliseringKopierJobbUtfører()
        }

        override val type = "redigitalisering.kopier"
        override val navn = "Kopier journalpost for redigitalisering"
        override val beskrivelse = "Kopierer en journalpost i joark og lagrer den nye journalposten lokalt"
    }

    override fun utfør(input: JobbInput) {}
}
