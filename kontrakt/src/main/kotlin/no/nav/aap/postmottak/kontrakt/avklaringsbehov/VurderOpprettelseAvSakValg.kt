package no.nav.aap.postmottak.kontrakt.avklaringsbehov

/**
 * Saksbehandlers valg i det manuelle avklaringsbehovet [Definisjon.VURDER_OPPRETTELSE_AV_SAK]:
 * hvor søknaden skal behandles videre.
 *
 * - [ARENA]: bruker har fortsatt gjenværende rettigheter på eksisterende Arena-sak.
 * - [KELVIN]: søknaden skal vurderes som ny sak i Kelvin.
 * - [BEGGE]: både gjenoppta/gjeninntre i Arena-sak og starte ny sak i Kelvin.
 *
 * Merk: [BEGGE] er foreløpig ikke valgbart i frontend og har ingen ferdig løsning i backend enda.
 */
public enum class VurderOpprettelseAvSakValg {
    ARENA,
    KELVIN,
    BEGGE,
}

