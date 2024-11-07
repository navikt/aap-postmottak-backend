package no.nav.aap.postmottak.fordeler

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.fordeler.regler.RegelInput
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.saf.graphql.SafGraphqlClient
import java.time.LocalDateTime

const val JOURNALPOST_ID_KEY = "journalpostId"
const val MOTTATT_TID_KEY = "mottattTid"

class FordelingJobbUtfører(
    val safClient: SafGraphqlClient,
    val journalpostService: JournalpostService,
    val regelService: FordelerRegelService
): JobbUtfører {


    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return FordelingJobbUtfører(
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
        val journalpostId = JournalpostId(input.parameter(JOURNALPOST_ID_KEY).toLong())
        val mottattTid = LocalDateTime.parse(input.parameter(MOTTATT_TID_KEY))

        val journalpost = journalpostService.hentjournalpost(journalpostId)

        regelService.skalTilKelvin(
            RegelInput(journalpostId.referanse,
                journalpost.person.aktivIdent().identifikator)
        )

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