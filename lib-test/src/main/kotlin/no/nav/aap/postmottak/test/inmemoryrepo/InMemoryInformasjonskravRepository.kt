package no.nav.aap.postmottak.test.inmemoryrepo

import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravInput
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object InMemoryInformasjonskravRepository : InformasjonskravRepository {
    private val oppdateringer = ConcurrentHashMap<JournalpostId, CopyOnWriteArrayList<InformasjonskravOppdatert>>()

    override fun hentOppdateringer(
        journalpostId: JournalpostId,
        informasjonskrav: List<InformasjonskravNavn>,
    ): List<InformasjonskravOppdatert> =
        oppdateringer[journalpostId].orEmpty()
            .filter { it.navn in informasjonskrav }
            .groupBy { it.navn }
            .values
            .mapNotNull { it.maxByOrNull(InformasjonskravOppdatert::oppdatert) }

    override fun registrerOppdateringer(
        journalpostId: JournalpostId,
        behandlingId: BehandlingId,
        informasjonskrav: Map<InformasjonskravNavn, InformasjonskravInput>,
        oppdatert: Instant,
    ) {
        val lagrede = oppdateringer.computeIfAbsent(journalpostId) { CopyOnWriteArrayList() }
        informasjonskrav.forEach { (navn, input) ->
            lagrede += InformasjonskravOppdatert(
                behandlingId = behandlingId,
                navn = navn,
                oppdatert = oppdatert,
                forrigeInput = DefaultJsonMapper.toJson(input),
            )
        }
    }
}
