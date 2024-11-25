package no.nav.aap.postmottak.fordeler

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.postmottak.klient.nom.NomKlient
import no.nav.aap.postmottak.klient.norg.Diskresjonskode
import no.nav.aap.postmottak.klient.norg.NorgKlient
import no.nav.aap.postmottak.klient.pdl.Adressebeskyttelseskode
import no.nav.aap.postmottak.klient.pdl.GeografiskTilknytning
import no.nav.aap.postmottak.klient.pdl.GeografiskTilknytningType
import no.nav.aap.postmottak.klient.pdl.HentPersonResult
import no.nav.aap.postmottak.klient.pdl.PdlData
import no.nav.aap.postmottak.klient.pdl.PdlGraphQLClient
import no.nav.aap.postmottak.sakogbehandling.journalpost.Person
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.*

class EnhetsutrederTest {


    val pdlKlient: PdlGraphQLClient = mockk()
    val norgKlient: NorgKlient = mockk(relaxed = true)
    val nomKlient: NomKlient = mockk()

    val enhetsutleder = Enhetsutreder(norgKlient, pdlKlient, nomKlient)

     @Test
     fun `alle avhengigheter blir kalt med forventede argumenter`() {
         val kommunenummer = "2342345"
         val erNavansatt = false
         val ident = Ident("test")

         every { pdlKlient.hentAdressebeskyttelseOgGeolokasjon(ident) } returns PdlData(
             HentPersonResult(adressebeskyttelse = listOf(Adressebeskyttelseskode.STRENGT_FORTROLIG)),
             hentGeografiskTilknytning = GeografiskTilknytning(GeografiskTilknytningType.KOMMUNE, gtKommune = kommunenummer)
         )

         every { nomKlient.erEgenansatt(any()) } returns erNavansatt

         enhetsutleder.finnNavenhetForPerson(personMedIDent(ident))

         verify(exactly = 1) { norgKlient.finnEnhet(kommunenummer, erNavansatt, Diskresjonskode.SPSF) }

     }

    @ParameterizedTest
    @CsvSource(value = [
        "KOMMUNE, 1",
        "BYDEL, 2",
        "UTLAND, 3",
        "UDEFINERT, UDEFINERT"
    ])
    fun `verifiser at geografisk tilknytning blir mappet riktig`(
        geografiskTilknytningType: GeografiskTilknytningType,
        forventetGeografiskKode: String
    ) {
        val ident = Ident("test")

        every { pdlKlient.hentAdressebeskyttelseOgGeolokasjon(ident) } returns PdlData(
            HentPersonResult(adressebeskyttelse = listOf(Adressebeskyttelseskode.STRENGT_FORTROLIG)),
            hentGeografiskTilknytning = GeografiskTilknytning(
                gtType = geografiskTilknytningType,
                gtKommune = "1",
                gtBydel = "2",
                gtLand = "3",
            )
        )

        every { nomKlient.erEgenansatt(any()) } returns true

        enhetsutleder.finnNavenhetForPerson(personMedIDent(ident))

        verify(exactly = 1) { norgKlient.finnEnhet(forventetGeografiskKode, any(), any()) }

    }

    @ParameterizedTest
    @CsvSource(value = [
        "FORTROLIG, SPFO",
        "STRENGT_FORTROLIG, SPSF",
        "STRENGT_FORTROLIG_UTLAND, SPSF",
        ", ANY"
    ])
    fun `verifiser at adressebeskyttelse blir riktig mappet til diskresjonskode`(
        adressebeskyttelseskode: Adressebeskyttelseskode?,
        forventetDiskresjonskode: Diskresjonskode
    ) {
        val ident = Ident("test")

        every { pdlKlient.hentAdressebeskyttelseOgGeolokasjon(ident) } returns PdlData(
            HentPersonResult(adressebeskyttelse = adressebeskyttelseskode?.let { listOf(it) } ?: emptyList()),
                hentGeografiskTilknytning = GeografiskTilknytning(
                    gtType = GeografiskTilknytningType.KOMMUNE,
                    gtKommune = "1",
                )
            )

        every { nomKlient.erEgenansatt(any()) } returns true

        enhetsutleder.finnNavenhetForPerson(personMedIDent(ident))

        verify(exactly = 1) { norgKlient.finnEnhet(any(), any(), forventetDiskresjonskode) }

    }

    private fun personMedIDent(ident: Ident) = Person(1, UUID.randomUUID(), listOf(ident))

 }