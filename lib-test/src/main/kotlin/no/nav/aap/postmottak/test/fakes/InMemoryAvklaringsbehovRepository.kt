package no.nav.aap.postmottak.test.fakes

import no.nav.aap.postmottak.avklaringsbehov.Avklaringsbehov
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.postmottak.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.postmottak.avklaringsbehov.Endring
import no.nav.aap.postmottak.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Status
import no.nav.aap.postmottak.kontrakt.steg.StegType
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong

object InMemoryAvklaringsbehovRepository : AvklaringsbehovRepository {

    private val idSeq = AtomicLong(10000)
    private val memory = HashMap<BehandlingId, AvklaringsbehovHolder>()
    private val lock = Any()

    override fun hentAvklaringsbehovene(behandlingId: BehandlingId): Avklaringsbehovene {
        return Avklaringsbehovene(this, behandlingId)
    }

    override fun hent(behandlingId: BehandlingId): List<Avklaringsbehov> {
        synchronized(lock) {
            ensureDefault(behandlingId)
            return memory.getValue(behandlingId).avklaringsbehovene
        }
    }

    override fun opprett(
        behandlingId: BehandlingId,
        definisjon: Definisjon,
        funnetISteg: StegType,
        frist: LocalDate?,
        begrunnelse: String,
        grunn: ÅrsakTilSettPåVent?,
        endretAv: String
    ) {
        synchronized(lock) {
            ensureDefault(behandlingId)
            val avklaringsbehov = memory.getValue(behandlingId)

            val eksisterendeBehov = avklaringsbehov.hentBehov(definisjon)
            eksisterendeBehov?.historikk?.add(
                Endring(
                    status = Status.OPPRETTET,
                    begrunnelse = begrunnelse,
                    grunn = grunn,
                    endretAv = endretAv,
                    frist = frist
                )
            )
                ?: avklaringsbehov.leggTilBehov(
                    definisjon,
                    funnetISteg,
                    frist,
                    begrunnelse,
                    grunn,
                    endretAv,
                )
        }
    }

    private fun ensureDefault(behandlingId: BehandlingId) {
        if (memory[behandlingId] == null) {
            memory[behandlingId] = AvklaringsbehovHolder(mutableListOf())
        }
    }

    override fun endre(
        avklaringsbehovId: Long,
        endring: Endring
    ) {
        endreAvklaringsbehov(avklaringsbehovId, endring)
    }

    override fun endreVentepunkt(
        avklaringsbehovId: Long,
        endring: Endring,
        funnetISteg: StegType
    ) {
        oppdaterFunnetISteg(avklaringsbehovId, funnetISteg)
        endreAvklaringsbehov(avklaringsbehovId, endring)
    }

    fun clearMemory() {
        memory.clear()
    }

    private fun endreAvklaringsbehov(
        avklaringsbehovId: Long,
        endring: Endring
    ) {
        memory.values.flatMap { it.avklaringsbehovene }
            .single { it.id == avklaringsbehovId }
            .historikk.add(endring)
    }

    private fun oppdaterFunnetISteg(avklaringsbehovId: Long, funnetISteg: StegType) {
        val holder = memory.values
            .single { avklaringsbehovHolder ->
                avklaringsbehovHolder.avklaringsbehovene
                    .find { it.id == avklaringsbehovId } != null
            }

        val avklaringsbehov = holder.avklaringsbehovene
            .single { it.id == avklaringsbehovId }


        holder.avklaringsbehovene.removeIf { it.id == avklaringsbehovId }
        holder.avklaringsbehovene.add(
            Avklaringsbehov(
                avklaringsbehovId,
                avklaringsbehov.definisjon,
                avklaringsbehov.historikk,
                funnetISteg,
            )
        )
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
    }

    override fun slett(behandlingId: BehandlingId) {
    }

    private class AvklaringsbehovHolder(val avklaringsbehovene: MutableList<Avklaringsbehov>) {
        fun hentBehov(definisjon: Definisjon): Avklaringsbehov? {
            return avklaringsbehovene.singleOrNull { avklaringsbehov -> avklaringsbehov.definisjon == definisjon }
        }

        fun leggTilBehov(
            definisjon: Definisjon,
            funnetISteg: StegType,
            frist: LocalDate?,
            begrunnelse: String,
            venteÅrsak: ÅrsakTilSettPåVent?,
            endretAv: String,
        ) {
            val avklaringsbehov = Avklaringsbehov(
                idSeq.andIncrement, definisjon,
                mutableListOf(
                    Endring(
                        status = Status.OPPRETTET,
                        begrunnelse = begrunnelse,
                        grunn = venteÅrsak,
                        endretAv = endretAv,
                        frist = frist,
                    )
                ),
                funnetISteg = funnetISteg,
            )
            avklaringsbehovene.add(avklaringsbehov)
        }
    }
}

