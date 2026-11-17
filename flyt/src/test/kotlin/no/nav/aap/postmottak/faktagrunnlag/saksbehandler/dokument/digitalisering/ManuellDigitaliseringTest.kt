package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksvurdering
import no.nav.aap.postmottak.gateway.serialiser
import no.nav.aap.postmottak.test.fakes.TestJournalposter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate

class ManuellDigitaliseringTest {
    private val mottattDato = LocalDate.of(2026, 1, 2)
    private val søknad = SøknadV0(student = null, yrkesskade = "Nei", oppgitteBarn = null)

    @Test
    fun `validerer og digitaliserer mot den valgte journalposten`() {
        val journalpost = TestJournalposter.papirsøknad().tilJournalpost(mottattDato = mottattDato)
        val tidligereJournalpost = TestJournalposter.papirsøknad()
            .tilJournalpost(mottattDato = mottattDato.minusDays(1))

        val vurdering = ManuellDigitalisering.lagVurdering(
            journalpost = journalpost,
            kategori = InnsendingType.SØKNAD,
            strukturertDokument = søknad.serialiser(),
            søknadsdato = mottattDato,
            saksvurdering = null
        )

        assertThat(vurdering).isEqualTo(
            Digitaliseringsvurdering(InnsendingType.SØKNAD, søknad.serialiser(), mottattDato, true)
        )
        val feil = assertThrows<IllegalArgumentException> {
            ManuellDigitalisering.lagVurdering(
                journalpost = tidligereJournalpost,
                kategori = InnsendingType.SØKNAD,
                strukturertDokument = søknad.serialiser(),
                søknadsdato = mottattDato,
                saksvurdering = null
            )
        }
        assertThat(feil).hasMessageContaining("Søknadsdato kan ikke være etter registrert dato")
    }

    @ParameterizedTest
    @CsvSource(
        "SØKNAD, null",
        "MELDEKORT, null",
        "KLAGE, 2026-01-02",
        "LEGEERKLÆRING, 2026-01-02",
        nullValues = ["null"]
    )
    fun `krever søknadsdato bare for søknader og meldekort`(kategori: InnsendingType, søknadsdato: LocalDate?) {
        val journalpost = TestJournalposter.leggTil().tilJournalpost(mottattDato = mottattDato)

        val feil = assertThrows<IllegalArgumentException> {
            ManuellDigitalisering.lagVurdering(
                journalpost = journalpost,
                kategori = kategori,
                strukturertDokument = null,
                søknadsdato = søknadsdato,
                saksvurdering = null
            )
        }

        assertThat(feil).hasMessage("Søknadsdato skal kun settes for søknader eller meldekort")
    }

    @Test
    fun `avviser klage på nyopprettet sak`() {
        val journalpost = TestJournalposter.leggTil().tilJournalpost()

        val feil = assertThrows<IllegalArgumentException> {
            ManuellDigitalisering.lagVurdering(
                journalpost = journalpost,
                kategori = InnsendingType.KLAGE,
                strukturertDokument = null,
                søknadsdato = null,
                saksvurdering = Saksvurdering(saksnummer = "123", opprettetNy = true)
            )
        }

        assertThat(feil).hasMessage("Klage skal knyttes mot eksisterende sak")
    }

    @Test
    fun `klage krever avklart sak`() {
        val journalpost = TestJournalposter.leggTil().tilJournalpost()

        val feil = assertThrows<IllegalStateException> {
            ManuellDigitalisering.lagVurdering(
                journalpost = journalpost,
                kategori = InnsendingType.KLAGE,
                strukturertDokument = null,
                søknadsdato = null,
                saksvurdering = null
            )
        }

        assertThat(feil).hasMessage("Saksvurdering mangler ved digitalisering av klage")
    }
}
