package no.nav.aap.postmottak.redigitalisering

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon

class RedigitaliseringBehandlingJobbUtfører(
) : JobbUtfører {

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return RedigitaliseringBehandlingJobbUtfører(
            )
        }

        override val type = "redigitalisering.behandling"
        override val navn = "Opprett behandling for redigitalisering"
        override val beskrivelse = "Oppretter ny behandling for den kopierte journalposten og starter prosessering"
    }

    override fun utfør(input: JobbInput) {}
}
