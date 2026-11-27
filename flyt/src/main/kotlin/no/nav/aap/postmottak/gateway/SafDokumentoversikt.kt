package no.nav.aap.postmottak.gateway

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import java.time.LocalDateTime

data class SafDokumentoversikt(
    @param:JsonSetter(contentNulls = Nulls.FAIL)
    val journalposter: List<SafJournalpostOversikt>,
    val sideInfo: SafSideInfo
)

data class SafSideInfo(val sluttpeker: String?, val finnesNesteSide: Boolean)

data class SafJournalpostOversikt(
    val journalpostId: String,
    val tittel: String?,
    val datoOpprettet: LocalDateTime,
    val tema: String,
    val skjerming: String?,
    @param:JsonSetter(contentNulls = Nulls.FAIL)
    val dokumenter: List<SafOversiktDokument>
)

data class SafOversiktDokument(
    val dokumentInfoId: String,
    val tittel: String?,
    val skjerming: String?,
    @param:JsonSetter(contentNulls = Nulls.FAIL)
    val dokumentvarianter: List<SafVisningsvariant>
)

data class SafVisningsvariant(
    val variantformat: SafVariantformat,
    val saksbehandlerHarTilgang: Boolean,
    val skjerming: String?
)
