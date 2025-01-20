package no.nav.aap.fordeler

import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.postmottak.gateway.EgenAnsattGateway
import no.nav.aap.postmottak.gateway.GeografiskTilknytning
import no.nav.aap.postmottak.gateway.GeografiskTilknytningType
import no.nav.aap.postmottak.gateway.PersondataGateway
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import org.slf4j.LoggerFactory

class Enhetsutreder(
    private val norgKlient: NorgGateway,
    private val pdlKlient: PersondataGateway,
    private val nomKlient: EgenAnsattGateway
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        fun konstruer(): Enhetsutreder {
            val norgKlient = GatewayProvider.provide(NorgGateway::class)
            val pdlKlient = GatewayProvider.provide(PersondataGateway::class)
            val nomKlient = GatewayProvider.provide(EgenAnsattGateway::class)
            return Enhetsutreder(norgKlient, pdlKlient, nomKlient)
        }
    }

    fun finnNavenhetForJournalpost(journalpost: Journalpost): NavEnhet? {
        val journalførendeEnhet = journalpost.journalførendeEnhet
        return if (journalførendeEnhet != null && erNavEnhetAktiv(journalførendeEnhet)) journalførendeEnhet
        else finnNavenhetForPerson(journalpost.person)
    }

    fun finnNavenhetForPerson(person: Person): NavEnhet? {
        log.info("Finner enhet for ident ${person.aktivIdent()}")

        val adressebeskyttelseOgGeoTilknytning = pdlKlient.hentAdressebeskyttelseOgGeolokasjon(person.aktivIdent())

        val geografiskTilknytning =
            mapGeografiskTilknytningTilKode(adressebeskyttelseOgGeoTilknytning.geografiskTilknytning)
        val diskresjonskode = adressebeskyttelseOgGeoTilknytning.adressebeskyttelse.firstOrNull()?.tilDiskresjonskode()
            ?: Diskresjonskode.ANY
        val erNavansatt = nomKlient.erEgenAnsatt(person.aktivIdent())

        return norgKlient.finnEnhet(
            geografiskTilknytning = geografiskTilknytning,
            diskresjonskode = diskresjonskode,
            erNavansatt = erNavansatt,
        )
    }

    private fun erNavEnhetAktiv(navEnhet: NavEnhet): Boolean {
        return navEnhet in hentAkriveEnheter()
    }

    private fun hentAkriveEnheter(): List<NavEnhet> {
        return norgKlient.hentAktiveEnheter()
    }

    private fun mapGeografiskTilknytningTilKode(geoTilknytning: GeografiskTilknytning) =
        when (geoTilknytning.gtType) {
            GeografiskTilknytningType.KOMMUNE ->
                geoTilknytning.gtKommune

            GeografiskTilknytningType.BYDEL ->
                geoTilknytning.gtBydel

            GeografiskTilknytningType.UTLAND ->
                geoTilknytning.gtLand

            GeografiskTilknytningType.UDEFINERT ->
                geoTilknytning.gtType.name
        }
}

typealias NavEnhet = String