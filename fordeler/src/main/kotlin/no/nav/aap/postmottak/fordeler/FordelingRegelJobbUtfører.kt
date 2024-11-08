package no.nav.aap.postmottak.fordeler

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.saf.graphql.SafGraphqlClient


class FordelingRegelJobbUtfører(
    val safClient: SafGraphqlClient,
    val journalpostService: JournalpostService,
    val regelService: FordelerRegelService
): JobbUtfører {


    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return FordelingRegelJobbUtfører(
                SafGraphqlClient.withClientCredentialsRestClient(),
                JournalpostService.konstruer(connection),
                FordelerRegelService()
            )
        }

        override fun type() = "fordel.innkommende"

        override fun navn() = "Prosesser fordeling"

        override fun beskrivelse() = "Vurderer mottaker av innkommende journalpost"

    }

    override fun utfør(input: JobbInput) {
        val journalpostId = input.getJournalpostId()
        val mottattTid = input.getMottattTid()

        val journalpost = journalpostService.hentjournalpost(journalpostId)

        /*
        regelService.skalTilKelvin(
            RegelInput(journalpostId.referanse,
                journalpost.person.aktivIdent().identifikator)
        )
*/
        /*
        val skalTilKelvin = FordelerRegelService().skalTilKelvin(
            RegelInput(
                journalpost.journalpostId.referanse,
                journalpost.person.aktivIdent().identifikator
            )
        )
*/
    }


}