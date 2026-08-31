package no.nav.aap.postmottak.test.modell

import no.nav.aap.postmottak.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.test.FødselsnummerGenerator
import java.time.LocalDate

fun genererIdent(fødselsdato: LocalDate): Ident {
    return Ident(FødselsnummerGenerator.Builder().fodselsdato(fødselsdato).buildAndGenerate())
}

/** Arena-sak med siste vedtak og maksdato, brukt av arenaoppslag-faken til å svare på maksdato-oppslag. */
data class TestArenaVedtak(
    val vedtakId: Int = 99,
    val aktfaseKode: String = "AKT",
    val vedtaktypeKode: String = "O",
    val fra: LocalDate = LocalDate.of(2024, 1, 1),
    val til: LocalDate? = LocalDate.of(2024, 12, 31),
    val maxdatoOrdinaer: LocalDate? = LocalDate.of(2025, 1, 1),
    val maxdatoUnntak: LocalDate? = null,
    val maxdatoAap: LocalDate? = LocalDate.of(2021, 3, 1),
)

data class TestArenaSak(
    val sakId: Int = 1234,
    val saknummer: String = "ABC-123",
    val sakStatus: String = "AKTIV",
    val sakRegistrert: LocalDate = LocalDate.of(2024, 1, 1),
    val sakAvsluttet: LocalDate? = null,
    val sisteVedtak: TestArenaVedtak = TestArenaVedtak(),
    val sisteUtbetalingsdato: LocalDate = LocalDate.of(2024, 5, 10),
)

data class TestPerson(
    val fødselsdato: Fødselsdato = Fødselsdato(LocalDate.now().minusYears(19)),
    val identer: Set<Ident> = setOf(genererIdent(fødselsdato.toLocalDate())),
    val erSkjermet: Boolean = false,
    var uføre: Int? = null,
    val kelvinsaker: List<TestKelvinSak> = emptyList(),
    val harAktivSakIArena: Boolean = false,
    val harHistorikkIArena: Boolean = false,
    val arenaSak: TestArenaSak? = null,
) {
    fun aktivIdent(): Ident = identer.single { it.aktivIdent }
}

class TestPersonBuilder {
    var kelvinSak: TestKelvinSak? = null
    var aktivSakIArena: Boolean = false
    var harHistorikkIArena: Boolean = false
    var skjermet: Boolean = false
    var arenaSak: TestArenaSak? = null
}

object TestPersoner {
    private val fakePersoner: MutableMap<String, TestPerson> = mutableMapOf()

    fun leggTil(person: TestPerson): TestPerson {
        fakePersoner[person.aktivIdent().identifikator] = person
        return person
    }

    fun leggTil(block: TestPersonBuilder.() -> Unit): TestPerson {
        val builder = TestPersonBuilder().apply(block)
        val person = TestPerson(
            kelvinsaker = listOfNotNull(builder.kelvinSak),
            erSkjermet = builder.skjermet,
            harAktivSakIArena = builder.aktivSakIArena,
            harHistorikkIArena = builder.harHistorikkIArena,
            arenaSak = builder.arenaSak,
        )
        fakePersoner[person.aktivIdent().identifikator] = person
        return person
    }

    fun hentPerson(ident: String): TestPerson? {
        return fakePersoner[ident]
    }
}