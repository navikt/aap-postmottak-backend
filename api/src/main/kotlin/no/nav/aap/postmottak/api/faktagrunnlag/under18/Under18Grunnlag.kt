package no.nav.aap.postmottak.api.faktagrunnlag.under18

import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.behandling.Status
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.postmottak.under18.Under18Aldersgrunnlag
import no.nav.aap.postmottak.under18.Under18SamtykkeVurdering
import no.nav.aap.postmottak.under18.Under18Søknadsavklaring
import java.time.LocalDate
import java.util.UUID

data class Under18Grunnlag(
    val behandling: Under18BehandlingDto,
    val saksnummer: String?,
    val aldersgrunnlag: Under18Aldersgrunnlag?,
    val søknadsavklaring: Under18Søknadsavklaring?,
    val samtykkevurdering: Under18SamtykkeVurdering?,
    val søknader: List<Under18SøknadDto>,
)

data class Under18SøknadDto(
    val journalpostId: JournalpostId,
    val tilknyttetJournalpostId: JournalpostId?,
    val dokumentbehandling: Under18BehandlingDto?,
)

data class Under18BehandlingDto(
    val referanse: UUID,
    val versjon: Long,
    val status: Status,
    val aktivtSteg: StegType,
    val åpneBehov: List<Definisjon>,
    val vent: List<Under18VentDto>,
    val prosesseringFeilet: Boolean,
)

data class Under18VentDto(val definisjon: Definisjon, val frist: LocalDate?)
