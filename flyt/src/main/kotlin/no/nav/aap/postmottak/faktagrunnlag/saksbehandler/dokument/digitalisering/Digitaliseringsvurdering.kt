package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import java.time.LocalDate


data class Digitaliseringsvurdering(
    val kategori: InnsendingType,
    val strukturertDokument: String?,
    val søknadsdato: LocalDate?
)
