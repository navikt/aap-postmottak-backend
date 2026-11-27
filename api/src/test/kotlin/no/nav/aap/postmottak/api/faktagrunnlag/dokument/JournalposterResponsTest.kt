package no.nav.aap.postmottak.api.faktagrunnlag.dokument

import kotlinx.coroutines.runBlocking
import no.nav.aap.postmottak.gateway.SafOversiktDokument
import no.nav.aap.postmottak.gateway.SafDokumentoversikt
import no.nav.aap.postmottak.gateway.SafJournalpostOversikt
import no.nav.aap.postmottak.gateway.SafSideInfo
import no.nav.aap.postmottak.gateway.SafVariantformat
import no.nav.aap.postmottak.gateway.SafVisningsvariant
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class JournalposterResponsTest {
    private val dato = LocalDateTime.of(2026, 11, 27, 12, 0)
    private val arkiv = SafVisningsvariant(SafVariantformat.ARKIV, true, null)
    private val dokument = SafOversiktDokument("456", "Legeerklæring", null, listOf(arkiv))
    private val journalpost = SafJournalpostOversikt("123", "Ettersendelse", dato, "AAP", null, listOf(dokument))

    @Test
    fun `utelater metadata uten lesetilgang eller med skjerming og beholder SAF sin paginering`() = runBlocking<Unit> {
        val oversikt = SafDokumentoversikt(
            listOf(
                journalpost,
                journalpost.copy(journalpostId = "124", tittel = "Hemmelig"),
                journalpost.copy(journalpostId = "125", skjerming = "SKJERMET"),
                journalpost.copy(journalpostId = "126", tema = "DAG")
            ),
            SafSideInfo("neste", true)
        )

        val respons = oversikt.tilRespons { it == "123" }

        assertThat(respons.journalposter).containsExactly(
            JournalpostOversiktDto("123", "Ettersendelse", dato, "AAP", listOf(
                JournalpostDokumentDto("456", "Legeerklæring", true)
            ))
        )
        assertThat(respons.sluttpeker).isEqualTo("neste")
        assertThat(respons.finnesNesteSide).isTrue()
        val tomSide = oversikt.tilRespons { false }
        assertThat(tomSide.journalposter).isEmpty()
        assertThat(tomSide.sluttpeker).isEqualTo("neste")
        assertThat(tomSide.finnesNesteSide).isTrue()
    }

    @Test
    fun `forhaandsvisning krever en tilgjengelig uskjermet ARKIV-variant og skjermede dokumenter utelates`() = runBlocking<Unit> {
        val dokumenter = listOf(
            dokument,
            dokument.copy(dokumentInfoId = "457", dokumentvarianter = listOf(arkiv.copy(saksbehandlerHarTilgang = false))),
            dokument.copy(dokumentInfoId = "458", dokumentvarianter = listOf(arkiv.copy(variantformat = SafVariantformat.SLADDET))),
            dokument.copy(dokumentInfoId = "459", dokumentvarianter = listOf(arkiv.copy(skjerming = "SKJERMET"))),
            dokument.copy(dokumentInfoId = "460", dokumentvarianter = emptyList()),
            dokument.copy(dokumentInfoId = "461", skjerming = "SKJERMET")
        )

        val respons = SafDokumentoversikt(
            listOf(journalpost.copy(dokumenter = dokumenter)), SafSideInfo(null, false)
        ).tilRespons { true }

        assertThat(respons.journalposter.single().dokumenter.map { it.dokumentInfoId })
            .containsExactly("456", "457", "458", "459", "460")
        assertThat(respons.journalposter.single().dokumenter.map { it.kanForhaandsvises })
            .containsExactly(true, false, false, false, false)
    }
}
