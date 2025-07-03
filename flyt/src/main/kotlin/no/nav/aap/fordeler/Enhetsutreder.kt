package no.nav.aap.fordeler

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.postmottak.gateway.EgenAnsattGateway
import no.nav.aap.postmottak.gateway.GeografiskTilknytning
import no.nav.aap.postmottak.gateway.GeografiskTilknytningType
import no.nav.aap.postmottak.gateway.Oppgavetype
import no.nav.aap.postmottak.gateway.PersondataGateway
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Behandlingstema
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import org.slf4j.LoggerFactory

class Enhetsutreder(
    private val norgKlient: NorgGateway,
    private val pdlKlient: PersondataGateway,
    private val nomKlient: EgenAnsattGateway,
    private val veilarbarenaKlient: VeilarbarenaGateway,
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        fun konstruer(): Enhetsutreder {
            val norgKlient = GatewayProvider.provide(NorgGateway::class)
            val pdlKlient = GatewayProvider.provide(PersondataGateway::class)
            val nomKlient = GatewayProvider.provide(EgenAnsattGateway::class)
            val veilarbarenaKlient = GatewayProvider.provide(VeilarbarenaGateway::class)
            return Enhetsutreder(norgKlient, pdlKlient, nomKlient, veilarbarenaKlient)
        }
    }

    fun finnJournalføringsenhet(journalpost: Journalpost): NavEnhet? {
        val journalførendeEnhet = journalpost.journalførendeEnhet
        if (journalførendeEnhet != null && journalførendeEnhet != "9999" && erNavEnhetAktiv(journalførendeEnhet)) {
            log.info("Journalpost ${journalpost.journalpostId} har allerede journalførende enhet ($journalførendeEnhet)")
            return journalførendeEnhet
        }
        val personAttributter = finnPersonAttributter(journalpost.person)

        return norgKlient.finnArbeidsfordelingsEnhet(
            geografiskTilknytning = personAttributter.geografiskTilknytning,
            diskresjonskode = personAttributter.diskresjonskode,
            erNavansatt = personAttributter.erNavansatt,
            behandlingstema = finnBehandlingstema(journalpost),
            behandlingstype = finnBehandlingstype(journalpost),
            oppgavetype = Oppgavetype.JOURNALFØRING
        ).also {
            log.info("Fant enhet $it for journalpost ${journalpost.journalpostId}")
        }
    }

    fun finnEnhetMedOppfølgingskontor(person: Person): EnhetMedOppfølgingsKontor {
        val personAttributter = finnPersonAttributter(person)

        val norgEnhet = norgKlient.finnArbeidsfordelingsEnhet(
            geografiskTilknytning = personAttributter.geografiskTilknytning,
            diskresjonskode = personAttributter.diskresjonskode,
            erNavansatt = personAttributter.erNavansatt,
            behandlingstema = Behandlingstema.AAP.kode
        )

        val oppfølgingsenhet =
            if (personAttributter.diskresjonskode != Diskresjonskode.SPSF)
                veilarbarenaKlient.hentOppfølgingsenhet(person.aktivIdent().identifikator)
            else null

        return EnhetMedOppfølgingsKontor(norgEnhet, oppfølgingsenhet)
    }

    private fun finnPersonAttributter(person: Person): PersonAttributter {
        val adressebeskyttelseOgGeoTilknytning = pdlKlient.hentAdressebeskyttelseOgGeolokasjon(person.aktivIdent())

        val geografiskTilknytning =
            mapGeografiskTilknytningTilKode(adressebeskyttelseOgGeoTilknytning.geografiskTilknytning)
        val diskresjonskode =
            adressebeskyttelseOgGeoTilknytning.adressebeskyttelse.firstOrNull()?.gradering?.tilDiskresjonskode()
                ?: Diskresjonskode.ANY
        val erNavansatt = nomKlient.erEgenAnsatt(person.aktivIdent())

        return PersonAttributter(geografiskTilknytning, diskresjonskode, erNavansatt)
    }

    // TODO: Sjekk om vi må hente behandlingstema fra kodeverk
    private fun finnBehandlingstema(journalpost: Journalpost): String {
        return journalpost.behandlingstema ?: Behandlingstema.AAP.kode
    }

    // TODO: Sjekk om vi må hente behandlingstype fra kodeverk
    private fun finnBehandlingstype(journalpost: Journalpost): String? {
        return Brevkoder.fraKode(journalpost.hoveddokumentbrevkode).behandlingstype
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

data class EnhetMedOppfølgingsKontor(
    val norgEnhet: NavEnhet?,
    val oppfølgingsenhet: NavEnhet?
)

data class PersonAttributter(
    val geografiskTilknytning: String?,
    val diskresjonskode: Diskresjonskode,
    val erNavansatt: Boolean
)