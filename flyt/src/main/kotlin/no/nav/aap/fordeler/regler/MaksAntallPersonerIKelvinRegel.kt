package no.nav.aap.fordeler.regler

import no.nav.aap.fordeler.RegelRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(MaksAntallPersonerIKelvinRegel::class.java)

class MaksAntallPersonerIKelvinRegel(private val maksAntallPersoner: Int) : Regel<MaksAntallPersonerIKelvinRegelInput> {
    companion object : RegelFactory<MaksAntallPersonerIKelvinRegelInput> {
        override val erAktiv = miljøConfig(prod = false, dev = false)
        override fun medDataInnhenting(
            repositoryProvider: RepositoryProvider?,
            gatewayProvider: GatewayProvider?
        ): RegelMedInputgenerator<MaksAntallPersonerIKelvinRegelInput> {
            val maksAntallPersoner = 46
            requireNotNull(repositoryProvider)
            return RegelMedInputgenerator(
                MaksAntallPersonerIKelvinRegel(maksAntallPersoner),
                MaksAntallPersonerIKelvinRegelInputGenerator(repositoryProvider)
            )
        }
    }

    override fun regelNavn(): String {
        return this::class.simpleName!!
    }

    override fun vurder(input: MaksAntallPersonerIKelvinRegelInput): Boolean {
        log.info("Maks: $maksAntallPersoner \n Antall i Kelvin: ${input.personerMedJournalpostVideresendtTilKelvin.size}")
        return input.personerMedJournalpostVideresendtTilKelvin.size < maksAntallPersoner
    }
}

class MaksAntallPersonerIKelvinRegelInputGenerator(private val repositoryProvider: RepositoryProvider) :
    InputGenerator<MaksAntallPersonerIKelvinRegelInput> {
    override fun generer(input: RegelInput): MaksAntallPersonerIKelvinRegelInput {
        val personerMedJournalpostVideresendtTilKelvin = repositoryProvider.provide<RegelRepository>()
            .hentPersonerMedJournalpostVideresendtTilKelvin()
        return MaksAntallPersonerIKelvinRegelInput(
            personerMedJournalpostVideresendtTilKelvin
        )
    }
}

data class MaksAntallPersonerIKelvinRegelInput(
    val personerMedJournalpostVideresendtTilKelvin: List<Person>
)