package no.nav.aap.postmottak.api.drift

import no.nav.aap.fordeler.arena.AvklarFordelingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering.DigitaliseringsvurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.overlever.OverleveringVurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.AvklarTemaRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId

class VurderingerService(
    private val fordelingRepository: AvklarFordelingRepository,
    private val temaRepository: AvklarTemaRepository,
    private val sakRepository: SaksnummerRepository,
    private val digitaliseringRepository: DigitaliseringsvurderingRepository,
    private val overleveringRepository: OverleveringVurderingRepository,
) {
    fun hent(behandlingId: BehandlingId): VurderingerDriftsinfo {
        val tema = temaRepository.hentTemaAvklaring(behandlingId)?.let {
            TemavurderingDriftsinfo(it.skalTilAap, it.tema)
        }

        val sak = sakRepository.hentSakVurdering(behandlingId)?.let {
            SaksvurderingDriftsinfo(it.generellSak, it.opprettetNy)
        }

        val fordeling = fordelingRepository.hentVurderingHvisEksisterer(behandlingId)?.let {
            FordelingsvurderingDriftsinfo(it.system)
        }

        val digitalisering = digitaliseringRepository.hentHvisEksisterer(behandlingId)?.let {
            DigitaliseringsvurderingDriftsinfo(
                kategori = it.kategori,
                digitalisertManueltGjennomPostmottak = it.digitalisertManueltGjennomPostmottak,
            )
        }

        val overlevering = overleveringRepository.hentHvisEksisterer(behandlingId)?.let {
            OverleveringsvurderingDriftsinfo(it.skalOverleveresTilKelvin)
        }

        return VurderingerDriftsinfo(
            tema = tema,
            sak = sak,
            fordeling = fordeling,
            digitalisering = digitalisering,
            overlevering = overlevering,
        )
    }

}
