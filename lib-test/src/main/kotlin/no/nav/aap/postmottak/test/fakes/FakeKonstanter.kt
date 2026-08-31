package no.nav.aap.postmottak.test.fakes

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.StudentStatus
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadStudentDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.postmottak.gateway.AvsenderMottaker
import no.nav.aap.postmottak.gateway.AvsenderMottakerIdType
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.AvsenderMottaker as JournalpostAvsenderMottaker
import no.nav.aap.postmottak.gateway.BrukerIdType
import no.nav.aap.postmottak.gateway.JournalpostSak
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Dokument
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Filtype
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Variant
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Variantformat
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.test.modell.TestPerson
import no.nav.aap.postmottak.test.modell.TestPersoner
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

data class TestJournalPost(
    val journalpostId: Long = Random.nextLong(10_000L, 1_000_000_000L),
    // Null betyr at journalposten ikke har noen avsenderMottaker
    val avsenderMottaker: AvsenderMottaker? = AvsenderMottaker(
        id = "21345345210",
        type = AvsenderMottakerIdType.FNR,
        navn = "Test Testesen",
    ),
    // Bruker på journalposten i SAF. Kan avvike fra avsenderMottaker, f.eks. for utenlandske organisasjoner.
    val brukerId: String = "21345345210",
    val brukerType: String = "FNR",
    // Mulige verdier: https://confluence.adeo.no/spaces/BOA/pages/316396024/Tema
    val tema: String = "AAP",
    val kanal: KanalFraKodeverk = KanalFraKodeverk.NAV_NO,
    val status: Journalstatus = Journalstatus.MOTTATT,
    val fagsak: JournalpostSak? = null,
    val brevkode: Brevkoder = Brevkoder.LEGEERKLÆRING,
    val digitalSøknad: SøknadV0? = null,
    // Journalførende enhet på journalposten, f.eks. satt for klage-ettersendinger
    val journalførendeEnhet: String? = null,
    // Om gosys allerede har en åpen oppgave for journalposten
    val harEksisterendeGosysOppgave: Boolean = false,
) {
    fun medFnr(ident: Ident): TestJournalPost =
        this.copy(
            avsenderMottaker = this.avsenderMottaker?.copy(id = ident.identifikator),
            brukerId = ident.identifikator
        )

    fun journalpostId(): JournalpostId {
        return JournalpostId(journalpostId)
    }

    /**
     *  Bruker feltene fra this. Person konstrueres ut fra brukerId.
     *  Som default lages ett dokument basert på brevkode med en digital (JSON) originalvariant,
     *  slik at f.eks. erDigitalLegeerklæring()/erDigitalSøknad() blir true for digitale journalposter.
     */
    fun tilJournalpost(
        dokumenter: List<Dokument> = listOf(standardDokument()),
        tittel: String? = null,
        behandlingstema: String? = null,
        mottattDato: LocalDate = LocalDate.now(),
        mottattTid: LocalDateTime? = LocalDateTime.now(),
    ): Journalpost {
        return Journalpost(
            journalpostId = this.journalpostId(),
            person = Person(1, UUID.randomUUID(), listOf(Ident(this.brukerId))),
            journalførendeEnhet = this.journalførendeEnhet,
            tema = this.tema,
            behandlingstema = behandlingstema,
            tittel = tittel,
            status = this.status,
            mottattDato = mottattDato,
            mottattTid = mottattTid,
            avsenderMottaker = this.avsenderMottaker?.let {
                JournalpostAvsenderMottaker(
                    id = it.id,
                    idType = it.type?.name,
                    navn = it.navn
                )
            },
            dokumenter = dokumenter,
            kanal = this.kanal,
            saksnummer = this.fagsak?.fagsakId,
            fagsystem = this.fagsak?.fagsaksystem?.name
        )
    }

    // Papirkanaler skanner inn dokumentet, så det finnes ingen digital (JSON) originalvariant da.
    private fun standardDokument(): Dokument {
        val erPapir = this.kanal in listOf(
            KanalFraKodeverk.SKAN_IM,
            KanalFraKodeverk.SKAN_PEN,
            KanalFraKodeverk.SKAN_NETS
        )
        return Dokument(
            dokumentInfoId = DokumentInfoId("1"),
            brevkode = this.brevkode.kode,
            tittel = null,
            varianter = listOf(
                Variant(
                    filtype = if (erPapir) Filtype.PDF else Filtype.JSON,
                    variantformat = Variantformat.ORIGINAL
                )
            )
        )
    }
}

class TestJournalPostBuilder {
    var journalpostId: Long? = null

    // Ident på personen journalposten skal knyttes til. Dersom denne (eller person) ikke settes
    // eksplisitt opprettes det automatisk en ny TestPerson (bl.a. slik at NomFake finner en person å svare på).
    var fnr: String? = null

    // Kan brukes i stedet for fnr dersom man trenger å beholde en referanse til TestPerson-objektet.
    var person: TestPerson? = null
    var brukerType = BrukerIdType.FNR
    var tema: String = "AAP"
    var brevkode: Brevkoder = Brevkoder.LEGEERKLÆRING
    var kanal: KanalFraKodeverk = KanalFraKodeverk.NAV_NO
    var status: Journalstatus = Journalstatus.MOTTATT
    var fagsak: JournalpostSak? = null
    var journalførendeEnhet: String? = null
    var harEksisterendeGosysOppgave: Boolean = false
    var digitalSøknad: SøknadV0? = null
    var avsenderMottaker: AvsenderMottaker? = null

    fun digitalSøknad() {
        brevkode = Brevkoder.SØKNAD
        digitalSøknad = SøknadV0(
            student = SøknadStudentDto(erStudent = StudentStatus.Nei),
            yrkesskade = "nei",
            oppgitteBarn = null,
            medlemskap = null,
        )
    }

    fun medUtenlandskOrgnr(orgnr: String = "999999999") {
        avsenderMottaker = null
        fnr = orgnr
        brukerType = BrukerIdType.ORGNR
    }

    fun papirsøknad() {
        kanal = KanalFraKodeverk.SKAN_NETS
        digitalSøknad = null
        brevkode = Brevkoder.SØKNAD
    }
}

object TestJournalposter {
    private val fakeJournalposter: MutableMap<Long, TestJournalPost> = mutableMapOf()

    fun leggTil(): TestJournalPost {
        return leggTil { }
    }

    fun leggTil(block: TestJournalPostBuilder.() -> Unit): TestJournalPost {
        val builder = TestJournalPostBuilder().apply(block)
        val ident = builder.fnr?.let(::Ident) ?: builder.person?.aktivIdent() ?: TestPersoner.leggTil {}.aktivIdent()
        val journalpost = TestJournalPost(
            journalpostId = builder.journalpostId ?: Random.nextLong(10_000L, 1_000_000_000L),
            tema = builder.tema,
            kanal = builder.kanal,
            status = builder.status,
            fagsak = builder.fagsak,
            brevkode = builder.brevkode,
            digitalSøknad = builder.digitalSøknad,
            journalførendeEnhet = builder.journalførendeEnhet,
            harEksisterendeGosysOppgave = builder.harEksisterendeGosysOppgave,
            avsenderMottaker = builder.avsenderMottaker,
            brukerType = builder.brukerType.name
        ).let {
            if (builder.brukerType != BrukerIdType.ORGNR) {
                it.medFnr(ident)
            } else it.copy(brukerId = ident.identifikator)
        }
        fakeJournalposter[journalpost.journalpostId] = journalpost

        return journalpost
    }

    fun digitalSøknad(): TestJournalPost = leggTil { digitalSøknad() }

    fun papirsøknad(): TestJournalPost = leggTil { papirsøknad() }

    fun hentJournalpost(journalpostId: Long): TestJournalPost? {
        return fakeJournalposter[journalpostId]
    }
}
