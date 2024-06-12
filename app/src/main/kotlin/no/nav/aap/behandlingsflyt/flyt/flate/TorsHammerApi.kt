package no.nav.aap.behandlingsflyt.flyt.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.kontrakt.aktivitet.TorsHammerDto
import no.nav.aap.behandlingsflyt.prosessering.BREVKODE
import no.nav.aap.behandlingsflyt.prosessering.HendelseMottattHåndteringOppgaveUtfører
import no.nav.aap.behandlingsflyt.prosessering.JOURNALPOST_ID
import no.nav.aap.behandlingsflyt.prosessering.MOTTATT_TIDSPUNKT
import no.nav.aap.behandlingsflyt.prosessering.PERIODE
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.behandlingsflyt.server.respondWithStatus
import no.nav.aap.json.DefaultJsonMapper
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import java.time.LocalDateTime
import javax.sql.DataSource

fun NormalOpenAPIRoute.torsHammerApi(dataSource: DataSource) {
    route("/api/hammer") {
        route("/send").post<Unit, String, TorsHammerDto> { _, dto ->
            dataSource.transaction { connection ->
                val sakService = SakService(connection)

                val sak = sakService.hent(Saksnummer(dto.saksnummer))

                val flytJobbRepository = FlytJobbRepository(connection)
                flytJobbRepository.leggTil(
                    JobbInput(HendelseMottattHåndteringOppgaveUtfører)
                        .forSak(sak.id)
                        .medParameter(JOURNALPOST_ID, System.currentTimeMillis().toString()) // TODO: Skal disse arkiveres eller kan vi håndtere disse utenfor
                        .medParameter(BREVKODE, Brevkode.AKTIVITETSKORT.name)
                        .medParameter(PERIODE, "")
                        .medParameter(MOTTATT_TIDSPUNKT, DefaultJsonMapper.toJson(LocalDateTime.now()))
                        .medPayload(DefaultJsonMapper.toJson(dto))
                )
            }
            respondWithStatus(HttpStatusCode.Accepted, "{}")
        }
    }
}
