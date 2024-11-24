package no.nav.aap.postmottak.fordeler

import no.nav.aap.postmottak.klient.gosysoppgave.NavEnhet
import no.nav.aap.postmottak.klient.nom.NomKlient
import no.nav.aap.postmottak.klient.norg.Diskresjonskode
import no.nav.aap.postmottak.klient.norg.NorgKlient
import no.nav.aap.postmottak.klient.pdl.Adressebeskyttelseskode
import no.nav.aap.postmottak.klient.pdl.GeografiskTilknytning
import no.nav.aap.postmottak.klient.pdl.GeografiskTilknytningType
import no.nav.aap.postmottak.klient.pdl.PdlGraphQLClient
import no.nav.aap.postmottak.sakogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.sakogbehandling.journalpost.Person

class Enhetsutreder(private val norgKlient: NorgKlient, private val pdlKlient: PdlGraphQLClient, private val nomKlient: NomKlient) {

    fun finnNavenhetForJournalpost(journalpost: Journalpost): NavEnhet {
        val journalførendeEnhet = journalpost.journalførendeEnhet
        return if( journalførendeEnhet != null && erNavnenhetAktiv(journalførendeEnhet)) journalførendeEnhet
            else finnNavenhetForPerson(journalpost.person)
    }

    fun finnNavenhetForPerson(person: Person): NavEnhet {
        val adressebeskyttelseOgGeoTilknytning = pdlKlient.hentAdressebeskyttelseOgGeolokasjon(person.aktivIdent())

        val geografiskTilknytning = adressebeskyttelseOgGeoTilknytning.hentGeografiskTilknytning?.let { mapGeografiskTilknytningTilKode(it)} ?: error("Geografisk tilknytning mangler")
        val diskresjonskode = mapDiskresjonskode(adressebeskyttelseOgGeoTilknytning.hentPerson?.adressebeskyttelse)
        val erNavansatt = nomKlient.erEgenansatt(person.aktivIdent())

        return norgKlient.finnEnhet(
            geografiskTilknyttning = geografiskTilknytning,
            diskresjonskode = diskresjonskode,
            erNavansatt = erNavansatt,
        )
    }

    private fun erNavnenhetAktiv(navEnhet: NavEnhet): Boolean {
        return navEnhet in hentAkriveEnheter()
    }

    private fun hentAkriveEnheter(): List<NavEnhet> {
        return norgKlient.hentAktiveEnheter()
    }

    fun mapGeografiskTilknytningTilKode(geoTilknytning: GeografiskTilknytning) =
        when (geoTilknytning.gtType) {
            GeografiskTilknytningType.KOMMUNE ->
                geoTilknytning.gtKommune
            GeografiskTilknytningType.BYDEL ->
                geoTilknytning.gtBydel
            GeografiskTilknytningType.UTLDAND ->
                geoTilknytning.gtLand
            GeografiskTilknytningType.UDEFINERT ->
                geoTilknytning.gtType.name
        }

    fun mapDiskresjonskode(adressebgeskyttelseskoder: List<Adressebeskyttelseskode>?) =
        adressebgeskyttelseskoder?.firstOrNull().let {
            when (it) {
                Adressebeskyttelseskode.FORTROLIG ->
                    Diskresjonskode.SPFO
                Adressebeskyttelseskode.STRENGT_FORTROLIG, Adressebeskyttelseskode.STRENGT_FORTROLIG_UTLAND ->
                    Diskresjonskode.SPSF
                else -> Diskresjonskode.ANY
            }
        }

}