package no.nav.aap.postmottak.journalpostogbehandling

import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Dokument
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Filtype
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Variant
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Variantformat
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
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
        journalpostId = JournalpostId(1),
        status = Journalstatus.MOTTATT,
        tema = "AAP",
        mottattDato = LocalDate.of(2021, 1, 1),
        journalførendeEnhet = "YOLO",
        kanal = KanalFraKodeverk.UKJENT,
        saksnummer = null,
        dokumenter = dokumenter ?: listOf(
            Dokument(
                brevkode = "Brev",
                dokumentInfoId = DokumentInfoId("1"),
                varianter = listOf(
                    Variant(
                        filtype = Filtype.JSON,
                        variantformat = Variantformat.ORIGINAL,
                    )
                )
            ),
            Dokument(
                brevkode = "Vedleg",
                dokumentInfoId = DokumentInfoId("2"),
                varianter = listOf(
                    Variant(
                        filtype = Filtype.PDF,
                        variantformat = Variantformat.SLADDET,
                    )
                )
            ),
            Dokument(
                brevkode = "Kattebilde",
                dokumentInfoId = DokumentInfoId("3"),
                varianter = listOf(
                    Variant(
                        filtype = Filtype.PDF,
                        variantformat = Variantformat.SLADDET,
                    )
                )
            )
        )
    )

    fun genererDokument(
        variantformat: Variantformat? = null,
        brevkode: String? = null,
    ) = Dokument(
        dokumentInfoId = DokumentInfoId("1"),
        brevkode = brevkode ?: "Brev",
        varianter = listOf(
            Variant(
                filtype = Filtype.JSON,
                variantformat = variantformat ?: Variantformat.ORIGINAL
            )
        )
    )

}