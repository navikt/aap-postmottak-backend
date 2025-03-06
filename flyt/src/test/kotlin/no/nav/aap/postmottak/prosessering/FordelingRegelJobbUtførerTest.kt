package no.nav.aap.postmottak.prosessering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.fordeler.FordelerRegelService
import no.nav.aap.fordeler.InnkommendeJournalpostStatus
import no.nav.aap.fordeler.Regelresultat
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.Tema
import no.nav.aap.postmottak.gateway.Bruker
import no.nav.aap.postmottak.gateway.BrukerIdType
import no.nav.aap.postmottak.gateway.GosysOppgaveGateway
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.gateway.SafDokumentInfo
import no.nav.aap.postmottak.gateway.SafDokumentvariant
import no.nav.aap.postmottak.gateway.SafJournalpost
import no.nav.aap.postmottak.gateway.SafVariantformat
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.repository.fordeler.InnkommendeJournalpostRepositoryImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FordelingRegelJobbUtførerTest {

    val flytJobbRepository: FlytJobbRepository = mockk(relaxed = true)
    val innkommendeJournalpostRepository = mockk<InnkommendeJournalpostRepositoryImpl>(relaxed = true)
    val regelService = mockk<FordelerRegelService>(relaxed = true)
    val journalpostService = mockk<JournalpostService>(relaxed = true)
    val gosysOppgaveGateway = mockk<GosysOppgaveGateway>(relaxed = true)

    val fordelingRegelJobbUtfører = FordelingRegelJobbUtfører(
        flytJobbRepository,
        journalpostService,
        regelService = regelService,
        innkommendeJournalpostRepository = innkommendeJournalpostRepository,
        gosysOppgaveGateway,
    )

    @Test
    fun `Vi kan sette og hente parametere for jobben`() {
        val journalpostId = JournalpostId(1)
        val input = JobbInput(FordelingRegelJobbUtfører)
            .forSak(journalpostId.referanse)
            .medJournalpostId(journalpostId)

        val actualJournalpostId = input.getJournalpostId()

        assertThat(actualJournalpostId).isEqualTo(actualJournalpostId)
    }

    @Test
    fun `når jobben er utført finnes det et regel resultat for journalposten`() {
        val journalpostId = JournalpostId(1L)
        
        val journalpost = SafJournalpost(
            journalpostId = journalpostId.referanse,
            bruker = Bruker(
                id = "fnr",
                type = BrukerIdType.FNR,
            ),
            dokumenter = listOf(
                SafDokumentInfo(
                    dokumentInfoId = "1",
                    brevkode = "NAV 11-13.05",
                    tittel = "tittel",
                    dokumentvarianter = listOf(
                        SafDokumentvariant(variantformat = SafVariantformat.ORIGINAL, filtype = "json")
                    )
                )
            ),
            journalstatus = Journalstatus.MOTTATT,
            tema = Tema.AAP.name,
            relevanteDatoer = emptyList()
        )

        val regelResultat = Regelresultat(mapOf("yolo" to true))

        every { journalpostService.hentSafJournalpost(journalpostId) } returns journalpost
        every { regelService.evaluer(any()) } returns regelResultat

        fordelingRegelJobbUtfører.utfør(
            JobbInput(FordelingRegelJobbUtfører)
                .forSak(journalpostId.referanse)
                .medJournalpostId(journalpostId)
        )

        verify {
            innkommendeJournalpostRepository.lagre(withArg {
                assertThat(it.journalpostId).isEqualTo(journalpostId)
                assertThat(it.regelresultat).isEqualTo(regelResultat)
            })
        }

        verify {
            flytJobbRepository.leggTil(withArg {
                assertThat(it.getJournalpostId()).isEqualTo(journalpostId)
            })
        }
    }
    
    @Test
    fun `Skal returnere tidlig dersom journalposten har blitt evaluert før`() {
        val journalpostId = JournalpostId(1L)
        every { innkommendeJournalpostRepository.eksisterer(journalpostId) } returns true

        fordelingRegelJobbUtfører.utfør(
            JobbInput(FordelingRegelJobbUtfører)
                .forSak(journalpostId.referanse)
                .medJournalpostId(journalpostId)
        )

        verify(exactly = 0) { regelService.evaluer(any()) }
    }

    @Test
    fun `Fordelingsoppgave skal opprettes for journalposter med orgnummer`() {
        val journalpostId = JournalpostId(1L)
        val journalpost = SafJournalpost(
            journalpostId = 1L,
            bruker = Bruker(
                id = "orgnr",
                type = BrukerIdType.ORGNR,
            ),
            dokumenter = listOf(
                SafDokumentInfo(
                    dokumentInfoId = "1",
                    brevkode = "NAV 11-13.05",
                    tittel = "tittel",
                    dokumentvarianter = listOf(
                        SafDokumentvariant(variantformat = SafVariantformat.ORIGINAL, filtype = "json")
                    )
                )
            ),
            journalstatus = Journalstatus.MOTTATT,
            tema = Tema.AAP.name,
            relevanteDatoer = emptyList()
        )

        every { journalpostService.hentSafJournalpost(journalpostId) } returns journalpost

        fordelingRegelJobbUtfører.utfør(
            JobbInput(FordelingRegelJobbUtfører)
                .forSak(journalpostId.referanse)
                .medJournalpostId(journalpostId)
        )

        verify(exactly = 1) {
            gosysOppgaveGateway.opprettFordelingsOppgaveHvisIkkeEksisterer(
                journalpostId = journalpostId,
                personIdent = null,
                orgnr = "orgnr",
                beskrivelse = "tittel. Forventet ikke å motta journalpost fra organisasjon på tema AAP"
            )
        }

        verify {
            innkommendeJournalpostRepository.lagre(withArg {
                assertThat(it.journalpostId).isEqualTo(journalpostId)
                assertThat(it.status).isEqualTo(InnkommendeJournalpostStatus.GOSYS_FDR)
                assertThat(it.regelresultat).isEqualTo(null)
            })
        }

        verify(exactly = 0) {
            flytJobbRepository.leggTil(any())
        }
    }
}