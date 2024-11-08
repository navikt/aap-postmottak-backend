package no.nav.aap.postmottak.fordeler

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører

class FordelingVideresendJobbUtfører(
    val regelRepository: RegelRepository
): JobbUtfører {


    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return FordelingVideresendJobbUtfører(
                RegelRepository(connection),
            )
        }

        override fun type() = "fordel.videresend"

        override fun navn() = "Prosesser videresending"

        override fun beskrivelse() = "Videresend journalpost"

    }

    override fun utfør(input: JobbInput) {
        val journalpostId = input.getJournalpostId()

    }


}