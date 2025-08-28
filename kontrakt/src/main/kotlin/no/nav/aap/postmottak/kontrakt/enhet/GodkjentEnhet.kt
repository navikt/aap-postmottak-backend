package no.nav.aap.postmottak.kontrakt.enhet


// OBS! Legger du til en enhet i denne lista, vil den begynne å motta saker i Kelvin!
enum class GodkjentEnhet(val enhetNr: String) {
    // Vest-Viken
    NAV_BÆRUM("0219"),
    NAV_ASKER("0220"),
    NAV_JEVNAKER("0532"),
    NAV_DRAMMEN("0602"),
    NAV_KONGSBERG("0604"),
    NAV_HOLE("0612"),
    NAV_HALLINGDAL("0617"),
    NAV_MIDTBUSKERUD("0623"),
    NAV_ØVRE_EIKER("0624"),
    NAV_LIER("0626"),
    NAV_NUMEDAL("0632"),
    NAV_RINGERIKE("0605"),
    EGEN_ANSATT_VEST_VIKEN("0683"),

    // Innlandet
    SYFA_INNLANDET("0491"),
    NAV_KONGSVINGER("0402"),
    NAV_HAMAR("0403"),
    NAV_RINGSAKER("0412"),
    NAV_LØTEN("0415"),
    NAV_STANGE("0417"),
    NAV_ODAL("0419"),
    NAV_EIDSKOG("0420"),
    NAV_SOLØR("0425"),
    NAV_ELVERUM("0427"),
    NAV_TRYSIL("0428"),
    NAV_ÅMOT("0429"),
    NAV_STORELVDAL("0430"),
    NAV_ENGERDAL("0434"),
    NAV_NORDØSTERDAL("0437"),
    NAV_LILLEHAMMER_GAUSDAL("0501"),
    NAV_GJØVIK("0502"),
    NAV_LESJA_DOVRE("0511"),
    NAV_LOM_SKJÅK("0513"),
    NAV_VÅGÅ("0515"),
    NAV_MIDTGUDBRANDSDAL("0516"),
    NAV_SEL("0517"),
    NAV_ØYER("0521"),
    NAV_ØSTRE_TOTEN("0528"),
    NAV_VESTRE_TOTEN("0529"),
    NAV_HADELAND("0534"),
    NAV_LAND("0538"),
    NAV_VALDRES("0542"),
    EGNE_ANSATTE_INNLANDET("0483")
}