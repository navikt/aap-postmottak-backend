package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.Dokument
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlIdentGateway
import no.nav.aap.behandlingsflyt.server.prosessering.ProssesserDokumentJobbUtfører
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.Ident
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling
import java.time.LocalDate

class MottaDokumentService {


    fun mottaDokument(connection: DBConnection, journalpostId: String, dokumenter: Dokument) {
        // TODO OPPRETT Dokumentbehandling
        // PLACEHOLDER FOR TESTING
        val sag = PersonOgSakService(connection, PdlIdentGateway)
            .finnEllerOpprett(Ident("12345467"), Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2025, 1, 1)))
        val behandling = BehandlingRepositoryImpl(connection).opprettBehandling(sag.id, TypeBehandling.DokumentHåndtering)
        FlytJobbRepository(connection).leggTil(
            JobbInput(ProssesserDokumentJobbUtfører)
            .forBehandling(sag.id, behandling.id).medCallId())
    }



}