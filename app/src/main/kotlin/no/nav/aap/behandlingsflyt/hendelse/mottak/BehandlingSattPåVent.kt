package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.auth.Bruker
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import java.time.LocalDate

class BehandlingSattPåVent(
    val frist: LocalDate?,
    val begrunnelse: String,
    val grunn: ÅrsakTilSettPåVent,
    val bruker: Bruker,
    val behandlingVersjon: Long
) : BehandlingHendelse