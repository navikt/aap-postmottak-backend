package no.nav.aap.postmottak.hendelse.mottak

import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.postmottak.avklaringsbehov.løser.ÅrsakTilSettPåVent
import java.time.LocalDate

class BehandlingSattPåVent(
    val frist: LocalDate?,
    val begrunnelse: String,
    val grunn: ÅrsakTilSettPåVent,
    val bruker: Bruker,
    val behandlingVersjon: Long
) : BehandlingHendelse