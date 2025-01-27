package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType


data class Digitaliseringsvurdering(
    val kategori: InnsendingType,
    val strukturertDokument: String?)
