package no.nav.aap.fordeler

/**
 * Utfallet av den maskinelle fordelingsvurderingen for en innkommende journalpost.
 *
 * - [ARENA]: journalposten videresendes til Arena.
 * - [KELVIN]: journalposten behandles automatisk i Kelvin.
 * - [MANUELL]: journalposten skal i utgangspunktet til Kelvin, men en saksbehandler må først vurdere
 *   om den heller skal fortsette i Arena (avklaringsbehovet VURDER_OPPRETTELSE_AV_SAK).
 */
enum class Fordelingsutfall {
    ARENA,
    KELVIN,
    MANUELL,
}

