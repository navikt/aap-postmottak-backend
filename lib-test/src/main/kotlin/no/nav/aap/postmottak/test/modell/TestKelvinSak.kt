package no.nav.aap.postmottak.test.modell

import no.nav.aap.behandlingsflyt.kontrakt.statistikk.ResultatKode
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate
import kotlin.random.Random

data class TestKelvinSak(
    val saksnummer: String = Random.nextInt(10_000, 20_000).toString(),
    val periode: Periode = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2022, 1, 31)),
    val resultat: ResultatKode? = null
)