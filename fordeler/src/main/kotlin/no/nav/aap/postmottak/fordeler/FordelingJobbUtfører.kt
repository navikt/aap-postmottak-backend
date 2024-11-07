package no.nav.aap.postmottak.fordeler

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.postmottak.fordeler.regler.RegelInput
import javax.management.loading.ClassLoaderRepository

class FordelingJobbUtfører(
): JobbUtfører {

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return FordelingJobbUtfører()
        }

        override fun type() = "fordel.innkommende"

        override fun navn() = "Prosesser fordeling"

        override fun beskrivelse() = "Vurderer mottaker av innkommende journalpost"

    }

    override fun utfør(input: JobbInput) {
        val skalTilKelvin = FordelerRegelService().skalTilKelvin(
            RegelInput(
                journalpost.journalpostId.referanse,
                journalpost.person.aktivIdent().identifikator
            )
        )

    }


}