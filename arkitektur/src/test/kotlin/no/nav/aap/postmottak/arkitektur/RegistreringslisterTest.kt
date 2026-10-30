package no.nav.aap.postmottak.arkitektur

import no.nav.aap.postmottak.arkitektur.Kildekode.fil
import no.nav.aap.postmottak.arkitektur.Kildekode.klasser
import no.nav.aap.postmottak.arkitektur.Kildekode.objekter
import no.nav.aap.postmottak.arkitektur.Kildekode.produksjon
import no.nav.aap.postmottak.arkitektur.Kildekode.registrerte
import org.junit.jupiter.api.Test

/**
 * De håndvedlikeholdte registreringslistene kompilerer uten endring og feiler først i runtime
 * (IllegalStateException/NoSuchElementException når repositoryet eller gatewayen slås opp) eller ikke i det hele
 * tatt (jobben kjører aldri).
 */
class RegistreringslisterTest {

    private val repositoryImpl = klasser.filter {
        it.name.endsWith("RepositoryImpl") && it.erKonkret() && it.moduleName != "lib-test"
    }
    private val steg = klasser.filter { it.arver("BehandlingSteg") && it.erKonkret() }
    private val informasjonskrav = klasser.filter { it.arver("Informasjonskrav") && it.erKonkret() }
    private val jobber = klasser.filter { it.arver("JobbUtfører") && it.erKonkret() }

    /** Klasser og objekter (noen gatewayer er `object`) som arver Gateway. */
    private val gatewayer =
        (klasser.filter { it.erKonkret() && it.arver("Gateway") }.map { it.name to it.moduleName } +
            objekter.filterNot { it.hasCompanionModifier }.filter { it.arver("Gateway") }.map { it.name to it.moduleName })
            .filter { (_, modul) -> modul == "klienter" || modul == "flyt" }
            .map { (navn, _) -> navn }

    private val flytdefinisjoner by lazy {
        produksjon.files.filter { it.path.contains("/forretningsflyt/behandlingstyper/") }.joinToString("\n") { it.text }
    }

    @Test
    fun `alle RepositoryImpl er registrert i PostgresRepositoryRegistry`() {
        val registrert = registrerte(fil("repository/PostgresRepositoryRegistry.kt"))
        sjekkRegel(
            sjekket = repositoryImpl,
            funn = repositoryImpl.filterNot { it.name in registrert }.map { it.name },
            slikRetterDu = "Legg til .register<Navn>() i repository/.../repository/PostgresRepositoryRegistry.kt.",
        )
    }

    @Test
    fun `alle jobbutførere er registrert i ProsesseringsJobber`() {
        val liste = fil("prosessering/ProsesseringsJobber.kt").text.substringAfter("listOf")
        sjekkRegel(
            sjekket = jobber,
            funn = jobber.filterNot { helOrd(it.name).containsMatchIn(liste) }.map { it.name },
            slikRetterDu = "Legg til jobben i listen i flyt/.../prosessering/ProsesseringsJobber.kt, " +
                "ellers kjenner ikke Motor den.",
        )
    }

    @Test
    fun `alle gateway-implementasjoner er registrert i DefaultGatewayProvider`() {
        val registrert = registrerte(fil("klient/DefaultGatewayProvider.kt"))
        sjekkRegel(
            sjekket = gatewayer,
            funn = gatewayer.filterNot { it in registrert },
            slikRetterDu = "Legg til register<Navn>() i klienter/.../klient/DefaultGatewayProvider.kt.",
        )
    }

    @Test
    fun `alle steg brukes i en flytdefinisjon`() = sjekkRegel(
        sjekket = steg,
        funn = steg.filterNot { helOrd(it.name).containsMatchIn(flytdefinisjoner) }.map { it.name },
        slikRetterDu = "Legg steget inn i riktig flyt i flyt/.../forretningsflyt/behandlingstyper/, eller slett det. " +
            "Rekkefølgen er semantikk: spør før du flytter eller setter inn steg.",
    )

    @Test
    fun `alle informasjonskrav brukes i en flytdefinisjon`() = sjekkRegel(
        sjekket = informasjonskrav,
        funn = informasjonskrav.filterNot { helOrd(it.name).containsMatchIn(flytdefinisjoner) }.map { it.name },
        slikRetterDu = "Legg informasjonskravet inn i kravlisten til steget som eier det i " +
            "flyt/.../forretningsflyt/behandlingstyper/, ellers hentes det aldri.",
    )

    @Test
    fun `alle feature toggles er i bruk`() {
        // FakeUnleash bruker en uttømmende when over PostmottakFeature, så en ny toggle uten fake kompilerer ikke.
        val enumKlasse = klasser.single { it.name == "PostmottakFeature" }
        val toggles = enumKlasse.enumConstants.map { it.name }
        val bruk = produksjon.files.filter { it.path != enumKlasse.path && it.moduleName != "lib-test" }
            .joinToString("\n") { it.text }
        sjekkRegel(
            sjekket = toggles,
            funn = toggles.filterNot { helOrd("PostmottakFeature.$it").containsMatchIn(bruk) }
                .map { "$it er ikke i bruk" },
            kjenteBrudd = setOf(
                // Eksempel-toggle, se kommentaren i FeatureToggle.kt. Kan slettes når en ny toggle legges til.
                "DummyFeature er ikke i bruk",
            ),
            slikRetterDu = "Ta togglen i bruk som PostmottakFeature.<Navn>, eller slett verdien i PostmottakFeature " +
                "(flyt/.../unleash/FeatureToggle.kt) og grenen i lib-test/.../test/FakeUnleash.kt.",
        )
    }

    @Test
    fun `konstantene i AvklaringsbehovKode speiler enumen`() {
        val tekst = fil("kontrakt/avklaringsbehov/AvklaringsbehovKode.kt").text
        val konstanter = Regex("""const val $IDENTIFIKATOR: String = "(\d+)"""").findAll(tekst).map { it.groupValues[1] }.toList()
        sjekkRegel(
            sjekket = konstanter,
            funn = konstanter.filterNot { "`$it`" in tekst.substringAfter("enum class") }.map { "$it finnes ikke i enumen" },
            slikRetterDu = "Legg koden til som `NNNN` i enum class AvklaringsbehovKode i " +
                "kontrakt/.../kontrakt/avklaringsbehov/AvklaringsbehovKode.kt.",
        )
    }
}
