package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.adapters.saf

import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.verdityper.dokument.DokumentInfoId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

import java.time.LocalDate

class JournalpostTest {



    @Test
    fun `forventer at en journalpost med dokument med søknadsbrevkode er søknad`() {
        val journalpost = genererJournalpost(dokumenter = listOf(genererDokument(brevkode = SKJEMANUMMER_SØKNAD)))

        val actual = journalpost.erSøknad()

        assertThat(actual).isTrue()
    }

    @Test
    fun `forventer at en journalpost uten dokument med søknadsbrevkode ikke er søknad`() {
        val journalpost = genererJournalpost(dokumenter = listOf(genererDokument(brevkode = "Annet")))

        val actual = journalpost.erSøknad()

        assertThat(actual).isFalse
    }

    @Test
    fun `forventer å få dokument med varianttype original når journalpost har dokument med varianttype original`() {
        val dokument = genererDokument(variantformat = Variantformat.ORIGINAL)
        val journalpost = genererJournalpost(dokumenter = listOf(dokument))

        val actual = journalpost.finnOriginal()

        assertThat(actual).isEqualTo(dokument)
    }

    @Test
    fun `forventer null når journalpost ikke har dokument med varianttype original`() {
        val dokument = genererDokument(variantformat = Variantformat.ARKIV)
        val journalpost = genererJournalpost(dokumenter = listOf(dokument))

        val actual = journalpost.finnOriginal()

        assertThat(actual).isNull()
    }

    @Test
    fun getDokumentNavn() {
    }

    @Test
    fun kanBehandlesAutomatisk() {
    }

    @Test
    fun erDigital() {
    }

    fun genererJournalpost(
        dokumenter: List<Dokument>? = null
    ) = Journalpost.MedIdent(
        personident = Ident.Personident("1123123"),
        journalpostId =  JournalpostId(1),
        status = JournalpostStatus.MOTTATT,
        mottattDato = LocalDate.of(2021, 1, 1),
        journalførendeEnhet = "YOLO",
        dokumenter = dokumenter ?: listOf(
            Dokument(
                tittel = "Søknad",
                brevkode = "Brev",
                filtype = Filtype.JSON,
                variantFormat = Variantformat.ORIGINAL,
                dokumentInfoId = DokumentInfoId("1")
            ),
            Dokument(
                tittel = "Søknad",
                brevkode = "Brev",
                filtype = Filtype.PDF,
                variantFormat = Variantformat.SLADDET,
                dokumentInfoId = DokumentInfoId("1")
            )
        )
    )

    fun genererDokument(
        variantformat: Variantformat? = null,
        brevkode: String? = null,
    ) = Dokument(
        dokumentInfoId = DokumentInfoId("1"),
        tittel = "Dokument tittel",
        brevkode = brevkode ?: "Brev",
        filtype = Filtype.JSON,
        variantFormat = variantformat ?: Variantformat.ORIGINAL
    )

}