package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Status
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.adapter.UbehandletPliktkort
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.kontrakt.pliktkort.Pliktkort
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.kontrakt.søknad.Søknad
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.søknad.adapter.UbehandletSøknad
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.SakId
import java.time.LocalDateTime

class MottaDokumentService(
    private val mottattDokumentRepository: MottattDokumentRepository,
) {

    fun håndterMottattDokument(
        journalpostId: JournalpostId,
        sakId: SakId,
        mottattTidspunkt: LocalDateTime,
        brevkode: Brevkode,
        strukturertDokument: StrukturertDokument<*>
    ) {
        mottattDokumentRepository.lagre(
            MottattDokument(
                journalpostId = journalpostId,
                sakId = sakId,
                mottattTidspunkt = mottattTidspunkt,
                type = brevkode,
                status = Status.MOTTATT,
                behandlingId = null,
                strukturertDokument = strukturertDokument
            )
        )
    }

    fun pliktkortSomIkkeErBehandlet(sakId: SakId): Set<UbehandletPliktkort> {
        val ubehandledePliktkort =
            mottattDokumentRepository.hentUbehandledeDokumenterAvType(sakId, Brevkode.PLIKTKORT)

        return ubehandledePliktkort.map {
            UbehandletPliktkort(
                it.journalpostId,
                (it.strukturerteData<Pliktkort>() as StrukturertDokument<Pliktkort>).data.timerArbeidPerPeriode
            )
        }.toSet()
    }

    fun søknaderSomIkkeHarBlittBehandlet(sakId: SakId): Set<UbehandletSøknad> {
        val ubehandledeSøknader =
            mottattDokumentRepository.hentUbehandledeDokumenterAvType(sakId, Brevkode.SØKNAD)

        return ubehandledeSøknader.map { mapSøknad(it.journalpostId, it.strukturerteData()) }.toSet()
    }

    private fun mapSøknad(journalpostId: JournalpostId, strukturerteData: StrukturertDokument<Søknad>?): UbehandletSøknad {
        val søknad = requireNotNull(strukturerteData).data
        return UbehandletSøknad(journalpostId, søknad.periode, søknad.student)
    }

    fun knyttTilBehandling(sakId: SakId, behandlingId: BehandlingId, journalpostId: JournalpostId) {
        mottattDokumentRepository.oppdaterStatus(journalpostId, behandlingId, sakId, Status.BEHANDLET)
    }
}