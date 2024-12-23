package no.nav.aap.postmottak.journalpostogbehandling

import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Dokument
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Filtype
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.JournalpostStatus
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Variantformat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

import java.time.LocalDate
import java.util.*

class JournalpostTest {

    @Test
    fun `forventer at en journalpost med dokument med søknadsbrevkode er søknad`() {
        val journalpost = genererJournalpost(dokumenter = listOf(genererDokument(brevkode = Brevkoder.SØKNAD.kode)))

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
    fun `hoveddokumentbrevkode returnerer brevkode til dokument med lavest dokument id`() {
        val journalpost = genererJournalpost()

        val hovedkode = journalpost.hoveddokumentbrevkode

        assertThat(hovedkode).isEqualTo("Brev")
    }

    @Test
    fun getDokumentNavn() {
    }

    @Test
    fun erDigitalSøknad() {
    }

    @Test
    fun erDigital() {
    }


    fun genererJournalpost(
        dokumenter: List<Dokument>? = null
    ) = Journalpost(
        person = Person(
            123,
            identifikator = UUID.randomUUID(),
            identer = listOf(Ident("12345678"))
        ),
        journalpostId =  JournalpostId(1),
        status = JournalpostStatus.MOTTATT,
        tema = "AAP",
        mottattDato = LocalDate.of(2021, 1, 1),
        journalførendeEnhet = "YOLO",
        kanal = KanalFraKodeverk.UKJENT,
        dokumenter = dokumenter ?: listOf(
            Dokument(
                brevkode = "Brev",
                filtype = Filtype.JSON,
                variantFormat = Variantformat.ORIGINAL,
                dokumentInfoId = DokumentInfoId("1"),
            ),
            Dokument(
                brevkode = "Vedleg",
                filtype = Filtype.PDF,
                variantFormat = Variantformat.SLADDET,
                dokumentInfoId = DokumentInfoId("2")
            ),
            Dokument(
                brevkode = "Kattebilde",
                filtype = Filtype.PDF,
                variantFormat = Variantformat.SLADDET,
                dokumentInfoId = DokumentInfoId("3")
            )
        )
    )

    fun genererDokument(
        variantformat: Variantformat? = null,
        brevkode: String? = null,
    ) = Dokument(
        dokumentInfoId = DokumentInfoId("1"),
        brevkode = brevkode ?: "Brev",
        filtype = Filtype.JSON,
        variantFormat = variantformat ?: Variantformat.ORIGINAL
    )

}