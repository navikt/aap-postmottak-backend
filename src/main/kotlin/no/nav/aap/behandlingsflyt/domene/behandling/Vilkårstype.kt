package no.nav.aap.behandlingsflyt.domene.behandling

enum class Vilkårstype(
    val kode: String,
    val gruppe: FunksjonellGruppe,
    val avslagsårsaker: List<Avslagsårsak>,
    val hjemmel: String
) {
    ALDERSVILKÅRET(
        kode = "AAP-4",
        gruppe = FunksjonellGruppe.INNGANGSVILKÅR,
        avslagsårsaker = listOf(
            Avslagsårsak.BRUKER_OVER_67,
            Avslagsårsak.BRUKER_UNDER_18,
            Avslagsårsak.MANGLENDE_DOKUMENTASJON
        ),
        hjemmel = "§ 11-4"
    ),
    SYKDOMSVILKÅRET(
        kode = "AAP-5",
        gruppe = FunksjonellGruppe.INNGANGSVILKÅR,
        avslagsårsaker = listOf(
            Avslagsårsak.MANGLENDE_DOKUMENTASJON
        ),
        hjemmel = "§ 11-5"
    ),
    GRUNNLAGET(
        kode = "AAP-20",
        gruppe = FunksjonellGruppe.BEREGNING,
        avslagsårsaker = listOf(
            Avslagsårsak.MANGLENDE_DOKUMENTASJON
        ),
        hjemmel = "§ 11-19"
    )

}

enum class FunksjonellGruppe {
    INNGANGSVILKÅR,
    BEREGNING,
    UTTAK,
    TILKJENT_YTELSE
}