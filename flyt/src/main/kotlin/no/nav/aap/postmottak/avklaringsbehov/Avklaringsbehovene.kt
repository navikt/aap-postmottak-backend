package no.nav.aap.postmottak.avklaringsbehov

import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.postmottak.SYSTEMBRUKER
import no.nav.aap.postmottak.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.postmottak.flyt.utledType
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandling
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.steg.StegType
import org.slf4j.LoggerFactory
import java.time.LocalDate

class Avklaringsbehovene(
    private val repository: AvklaringsbehovRepository,
    private val behandlingId: BehandlingId
) : AvklaringsbehoveneDecorator {
    private val log = LoggerFactory.getLogger(javaClass)
    private val avklaringsbehovene: List<Avklaringsbehov>
        get() = repository.hent(behandlingId)

    fun ingenEndring(avklaringsbehov: Avklaringsbehov, bruker: String) {
        løsAvklaringsbehov(
            avklaringsbehov.definisjon,
            "Ingen endring fra forrige vurdering",
            bruker
        )
    }

    fun løsAvklaringsbehov(
        definisjon: Definisjon,
        begrunnelse: String,
        endretAv: String,
        kreverToTrinn: Boolean? = null
    ) {
        val avklaringsbehov = alle().single { it.definisjon == definisjon }
        if (kreverToTrinn == null) {
            avklaringsbehov.løs(begrunnelse, endretAv = endretAv)
        } else {
            avklaringsbehov.løs(begrunnelse = begrunnelse, endretAv = endretAv, kreverToTrinn = kreverToTrinn)
            repository.kreverToTrinn(avklaringsbehov.id, kreverToTrinn)
        }
        repository.endre(avklaringsbehov.id, avklaringsbehov.historikk.last())
    }

    fun leggTilFrivilligHvisMangler(
        definisjon: Definisjon,
        bruker: Bruker
    ) {
        if (definisjon.erFrivillig()) {
            if (hentBehovForDefinisjon(definisjon) == null) {
                // Legger til frivillig behov
                leggTil(definisjoner = listOf(definisjon), stegType = definisjon.løsesISteg, bruker = bruker)
            }
        }
    }

    /**
     * Legger til nye avklaringsbehov.
     *
     * NB! Dersom avklaringsbehovet finnes fra før og er åpent så ignorerer vi det nye behovet, mens dersom det er avsluttet eller avbrutt så reåpner vi det.
     */
    fun leggTil(
        definisjoner: List<Definisjon>,
        stegType: StegType,
        frist: LocalDate? = null,
        begrunnelse: String = "",
        grunn: ÅrsakTilSettPåVent? = null,
        bruker: Bruker = SYSTEMBRUKER
    ) {
        log.info("Legger til avklaringsbehov :: {} - {}", definisjoner, stegType)
        definisjoner.forEach { definisjon ->
            val avklaringsbehov = hentBehovForDefinisjon(definisjon)
            if (avklaringsbehov != null) {
                if (avklaringsbehov.erAvsluttet()) {
                    avklaringsbehov.reåpne(utledFrist(definisjon, frist), begrunnelse, grunn, bruker)
                    if (avklaringsbehov.erVentepunkt()) {
                        // TODO: Vurdere om funnet steg bør ligge på endringen...
                        repository.endreVentepunkt(avklaringsbehov.id, avklaringsbehov.historikk.last(), stegType)
                    } else {
                        repository.endre(avklaringsbehov.id, avklaringsbehov.historikk.last())
                    }
                } else {
                    log.info("Forsøkte å legge til et avklaringsbehov som allerede eksisterte")
                }
            } else {
                repository.opprett(
                    behandlingId = behandlingId,
                    definisjon = definisjon,
                    funnetISteg = stegType,
                    frist = utledFrist(definisjon, frist),
                    begrunnelse = begrunnelse,
                    grunn = grunn,
                    endretAv = bruker.ident
                )
            }
        }
    }

    private fun utledFrist(
        definisjon: Definisjon,
        frist: LocalDate?
    ): LocalDate? {
        if (definisjon.erVentebehov()) {
            return definisjon.utledFrist(frist)
        }
        return null
    }

    override fun alle(): List<Avklaringsbehov> {
        return avklaringsbehovene
    }

    override fun alleEkskludertVentebehov(): List<Avklaringsbehov> {
        return avklaringsbehovene.filterNot { it.definisjon.erVentebehov() }
    }

    fun åpne(): List<Avklaringsbehov> {
        return alle().filter { it.erÅpent() }.toList()
    }

    override fun hentBehovForDefinisjon(definisjon: Definisjon): Avklaringsbehov? {
        return alle().singleOrNull { it.definisjon == definisjon }
    }

    fun harVærtSendtTilbakeFraBeslutterTidligere(): Boolean {
        return alle().any { avklaringsbehov -> avklaringsbehov.harVærtSendtTilbakeFraBeslutterTidligere() }
    }

    fun validateTilstand(
        behandling: Behandling,
        avklaringsbehov: Definisjon? = null
    ) {
        ValiderBehandlingTilstand.validerTilstandBehandling(
            behandling = behandling,
            avklaringsbehov = avklaringsbehov,
            eksisterenedeAvklaringsbehov = avklaringsbehovene
        )
    }

    fun validerPlassering(behandling: Behandling) {
        val nesteSteg = behandling.aktivtSteg()
        val behandlingFlyt = utledType(behandling.typeBehandling).flyt()
        behandlingFlyt.forberedFlyt(nesteSteg)
        val uhåndterteBehov = alle().filter { it.erÅpent() }
            .filter { definisjon ->
                behandlingFlyt.erStegFør(
                    definisjon.løsesISteg(),
                    nesteSteg
                )
            }
        if (uhåndterteBehov.isNotEmpty()) {
            throw IllegalStateException("Har uhåndterte behov som skulle vært håndtert før nåværende steg = '$nesteSteg'. Behov: ${uhåndterteBehov.map { it.definisjon }}")
        }
    }

    fun hentVentepunkterMedUtløptFrist(): List<Avklaringsbehov> {
        return alle().filter { it.erVentepunkt() && it.erÅpent() && it.fristUtløpt() }
    }

    fun hentVentepunkter(): List<Avklaringsbehov> {
        return alle().filter { it.erVentepunkt() && it.erÅpent() }
    }

    override fun erSattPåVent(): Boolean {
        return alle().any { avklaringsbehov -> avklaringsbehov.erVentepunkt() && avklaringsbehov.erÅpent() }
    }

    override fun toString(): String {
        return "Behov[${avklaringsbehovene.joinToString { it.toString() }}. For ID $behandlingId.]"
    }
}