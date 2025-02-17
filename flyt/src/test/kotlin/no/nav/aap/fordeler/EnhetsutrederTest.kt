package no.nav.aap.fordeler

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.postmottak.gateway.Adressebeskyttelseskode
import no.nav.aap.postmottak.gateway.GeografiskTilknytning
import no.nav.aap.postmottak.gateway.GeografiskTilknytningOgAdressebeskyttelse
import no.nav.aap.postmottak.gateway.GeografiskTilknytningType
import no.nav.aap.postmottak.gateway.Gradering
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.gateway.Oppgavetype
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Dokument
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Filtype
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Variant
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Variantformat
import no.nav.aap.postmottak.klient.arena.VeilarbarenaKlient
import no.nav.aap.postmottak.klient.nom.NomKlient
import no.nav.aap.postmottak.klient.norg.NorgKlient
import no.nav.aap.postmottak.klient.pdl.PdlGraphqlKlient
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import java.util.*


class EnhetsutrederTest {
    val pdlKlient: PdlGraphqlKlient = mockk()
    val norgKlient: NorgKlient = mockk(relaxed = true)
    val nomKlient: NomKlient = mockk()
    val veilarbarenaKlient: VeilarbarenaKlient = mockk()

    val enhetsutleder = Enhetsutreder(norgKlient, pdlKlient, nomKlient, veilarbarenaKlient)

    @Test
    fun `alle avhengigheter blir kalt med forventede argumenter`() {
        val kommunenummer = "2342345"
        val erNavansatt = false
        val ident = Ident("test")

        every { pdlKlient.hentAdressebeskyttelseOgGeolokasjon(ident) } returns GeografiskTilknytningOgAdressebeskyttelse(
            adressebeskyttelse = listOf(Gradering(Adressebeskyttelseskode.STRENGT_FORTROLIG)),
            geografiskTilknytning = GeografiskTilknytning(GeografiskTilknytningType.KOMMUNE, gtKommune = kommunenummer)
        )

        every { nomKlient.erEgenAnsatt(any()) } returns erNavansatt

        enhetsutleder.finnEnhetMedOppfølgingskontor(personMedIDent(ident))

        verify(exactly = 1) {
            norgKlient.finnArbeidsfordelingsEnhet(
                kommunenummer,
                erNavansatt,
                Diskresjonskode.SPSF,
                any()
            )
        }
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "KOMMUNE, 1",
            "BYDEL, 2",
            "UTLAND, 3",
            "UDEFINERT, UDEFINERT"
        ]
    )
    fun `verifiser at geografisk tilknytning blir mappet riktig`(
        geografiskTilknytningType: GeografiskTilknytningType,
        forventetGeografiskKode: String
    ) {
        val ident = Ident("test")

        every { pdlKlient.hentAdressebeskyttelseOgGeolokasjon(ident) } returns GeografiskTilknytningOgAdressebeskyttelse(
            adressebeskyttelse = listOf(Gradering(Adressebeskyttelseskode.STRENGT_FORTROLIG)),
            geografiskTilknytning = GeografiskTilknytning(
                gtType = geografiskTilknytningType,
                gtKommune = "1",
                gtBydel = "2",
                gtLand = "3",
            )
        )

        every { nomKlient.erEgenAnsatt(any()) } returns true

        enhetsutleder.finnEnhetMedOppfølgingskontor(personMedIDent(ident))

        verify(exactly = 1) { norgKlient.finnArbeidsfordelingsEnhet(forventetGeografiskKode, any(), any(), any()) }
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "FORTROLIG, SPFO",
            "STRENGT_FORTROLIG, SPSF",
            "STRENGT_FORTROLIG_UTLAND, SPSF",
            ", ANY"
        ]
    )
    fun `verifiser at adressebeskyttelse blir riktig mappet til diskresjonskode`(
        adressebeskyttelseskode: Adressebeskyttelseskode?,
        forventetDiskresjonskode: Diskresjonskode
    ) {
        val ident = Ident("test")

        every { pdlKlient.hentAdressebeskyttelseOgGeolokasjon(ident) } returns GeografiskTilknytningOgAdressebeskyttelse(
            adressebeskyttelse = adressebeskyttelseskode?.let { listOf(Gradering(it)) } ?: emptyList(),
            geografiskTilknytning = GeografiskTilknytning(
                gtType = GeografiskTilknytningType.KOMMUNE,
                gtKommune = "1",
            )
        )

        every { nomKlient.erEgenAnsatt(any()) } returns true
        every { veilarbarenaKlient.hentOppfølgingsenhet("test") } returns "345"

        enhetsutleder.finnEnhetMedOppfølgingskontor(personMedIDent(ident))

        verify(exactly = 1) { norgKlient.finnArbeidsfordelingsEnhet(any(), any(), forventetDiskresjonskode, any()) }
        val expectedTimes = if (adressebeskyttelseskode in listOf(
                Adressebeskyttelseskode.STRENGT_FORTROLIG,
                Adressebeskyttelseskode.STRENGT_FORTROLIG_UTLAND
            )
        ) 0 else 1
        verify(exactly = expectedTimes) { veilarbarenaKlient.hentOppfølgingsenhet(any()) }

    }

    private fun personMedIDent(ident: Ident) = Person(1, UUID.randomUUID(), listOf(ident))

    @ParameterizedTest
    @ValueSource(strings = ["STRENGT_FORTROLIG", "STRENGT_FORTROLIG_UTLAND"])
    fun skalIkkeHenteOppfølgingskontorForSPSF(adressebeskyttelseskode: Adressebeskyttelseskode?) {
        val ident = Ident("test")

        every { pdlKlient.hentAdressebeskyttelseOgGeolokasjon(ident) } returns GeografiskTilknytningOgAdressebeskyttelse(
            adressebeskyttelse = if (adressebeskyttelseskode != null) listOf(Gradering(adressebeskyttelseskode)) else emptyList(),
            geografiskTilknytning = GeografiskTilknytning(GeografiskTilknytningType.KOMMUNE, gtKommune = "1")
        )
        every { nomKlient.erEgenAnsatt(any()) } returns false

        every { norgKlient.finnArbeidsfordelingsEnhet("1", false, Diskresjonskode.SPSF, any()) } returns "345"

        val enheter = enhetsutleder.finnEnhetMedOppfølgingskontor(personMedIDent(ident))

        verify(exactly = 0) { veilarbarenaKlient.hentOppfølgingsenhet(any()) }

        assertThat(enheter).isEqualTo(EnhetMedOppfølgingsKontor("345", null))
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["FORTROLIG", "UGRADERT"])
    fun skalHenteOppfølgingskontorForAndreEnnSPSF(adressebeskyttelseskode: Adressebeskyttelseskode?) {
        val ident = Ident("test")

        every { pdlKlient.hentAdressebeskyttelseOgGeolokasjon(ident) } returns GeografiskTilknytningOgAdressebeskyttelse(
            adressebeskyttelse = if (adressebeskyttelseskode != null) listOf(Gradering(adressebeskyttelseskode)) else emptyList(),
            geografiskTilknytning = GeografiskTilknytning(GeografiskTilknytningType.KOMMUNE, gtKommune = "1")
        )

        every { nomKlient.erEgenAnsatt(any()) } returns false

        every { veilarbarenaKlient.hentOppfølgingsenhet(any()) } returns "123"
        every { norgKlient.finnArbeidsfordelingsEnhet(any(), any(), any(), any()) } returns "345"

        val enheter = enhetsutleder.finnEnhetMedOppfølgingskontor(personMedIDent(ident))

        assertThat(enheter).isEqualTo(EnhetMedOppfølgingsKontor("345", "123"))
    }

    @Test
    fun skalSetteRiktigParametreForJournalføingsEnhet() {
        val ident = Ident("test")
        val journalpost = Journalpost(
            journalpostId = JournalpostId(1),
            person = personMedIDent(ident),
            tema = "AAP",
            behandlingstema = null,
            fagsystem = null,
            saksnummer = null,
            journalførendeEnhet = null,
            status = Journalstatus.MOTTATT,
            kanal = KanalFraKodeverk.NAV_NO,
            mottattDato = LocalDate.of(2025, 1, 1),
            dokumenter = listOf(Dokument(
                brevkode = Brevkoder.KLAGE.kode,
                dokumentInfoId = DokumentInfoId("1"),
                varianter = listOf(Variant(
                    filtype = Filtype.JSON,
                    variantformat = Variantformat.ORIGINAL
                ))
            ))
        )

        every { pdlKlient.hentAdressebeskyttelseOgGeolokasjon(ident) } returns GeografiskTilknytningOgAdressebeskyttelse(
            adressebeskyttelse = listOf(Gradering(Adressebeskyttelseskode.STRENGT_FORTROLIG)),
            geografiskTilknytning = GeografiskTilknytning(GeografiskTilknytningType.KOMMUNE, gtKommune = "1")
        )

        every { nomKlient.erEgenAnsatt(any()) } returns true

        enhetsutleder.finnJournalføringsenhet(journalpost)

        verify(exactly = 1) { norgKlient.finnArbeidsfordelingsEnhet(
            "1", 
            true, 
            Diskresjonskode.SPSF, 
            "ab0014",
            "ae0058", 
            Oppgavetype.JOURNALFØRING) }
    }

}