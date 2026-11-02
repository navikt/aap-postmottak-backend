package no.nav.aap.postmottak.joarkavstemmer

import java.time.Instant
import java.time.LocalDate

data class Avstemmingsrapport(
    val startetTidspunkt: Instant,
    val fullførtTidspunkt: Instant,
    val resultater: List<JournalpostAvstemmingsresultat>,
)

enum class Avstemmingsutfall {
    UAVSTEMT_UKJENT,
    UAVSTEMT_KELVIN,
    GOSYS_OPPGAVE_OPPRETTET,
    FORDELINGSOPPGAVE_OPPRETTET,
    ALLEREDE_HAR_OPPGAVE,
    IKKE_NOK_INFORMASJON,
}

data class JournalpostAvstemmingsresultat(
    val journalpostId: Long,
    val utfall: Avstemmingsutfall,
    val kanal: String?,
    val datoOpprettet: LocalDate?,
) {
    companion object {
        fun fra(resultat: AvstemmingsResultat): JournalpostAvstemmingsresultat {
            val utfall = when (resultat) {
                is AvstemmingsResultat.UavstemtUkjent -> Avstemmingsutfall.UAVSTEMT_UKJENT
                is AvstemmingsResultat.UavstemtKelvin -> Avstemmingsutfall.UAVSTEMT_KELVIN
                is AvstemmingsResultat.GosysOppgaveOpprettet -> Avstemmingsutfall.GOSYS_OPPGAVE_OPPRETTET
                is AvstemmingsResultat.FordelingsoppgaveOpprettet -> Avstemmingsutfall.FORDELINGSOPPGAVE_OPPRETTET
                is AvstemmingsResultat.AlleredeHarOppgave -> Avstemmingsutfall.ALLEREDE_HAR_OPPGAVE
                is AvstemmingsResultat.IkkeNokInformasjon -> Avstemmingsutfall.IKKE_NOK_INFORMASJON
            }
            val kanal = when (resultat) {
                is AvstemmingsResultat.UavstemtUkjent -> resultat.kanal
                is AvstemmingsResultat.UavstemtKelvin -> resultat.kanal
                else -> null
            }
            val datoOpprettet = when (resultat) {
                is AvstemmingsResultat.UavstemtUkjent -> resultat.datoOpprettet
                is AvstemmingsResultat.UavstemtKelvin -> resultat.datoOpprettet
                else -> null
            }
            return JournalpostAvstemmingsresultat(resultat.journalpostId.referanse, utfall, kanal, datoOpprettet)
        }
    }
}
