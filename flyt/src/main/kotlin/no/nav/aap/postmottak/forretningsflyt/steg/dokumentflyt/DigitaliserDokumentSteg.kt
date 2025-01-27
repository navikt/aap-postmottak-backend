package no.nav.aap.postmottak.forretningsflyt.steg.dokumentflyt

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.KategoriVurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering.StruktureringsvurderingRepository
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FantAvklaringsbehov
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.gateway.DokumentGateway
import no.nav.aap.postmottak.gateway.DokumentTilMeldingParser
import no.nav.aap.postmottak.gateway.serialiser
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost

class DigitaliserDokumentSteg(
    private val struktureringsvurderingRepository: StruktureringsvurderingRepository,
    private val journalpostRepository: JournalpostRepository,
    private val kategorivurderingRepository: KategoriVurderingRepository,
    private val dokumentGateway: DokumentGateway
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            return DigitaliserDokumentSteg(
                repositoryProvider.provide(StruktureringsvurderingRepository::class),
                repositoryProvider.provide(JournalpostRepository::class),
                repositoryProvider.provide(KategoriVurderingRepository::class),
                GatewayProvider.provide(DokumentGateway::class)
            )
        }

        override fun type(): StegType {
            return StegType.DIGITALISER_DOKUMENT
        }

    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val struktureringsvurdering =
            struktureringsvurderingRepository.hentStruktureringsavklaring(kontekst.behandlingId)
        val kategorivurdering = kategorivurderingRepository.hentKategoriAvklaring(kontekst.behandlingId)
        val journalpost = journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)
        requireNotNull(journalpost)
        requireNotNull(kategorivurdering)


        if (journalpost.erDigitalSøknad() || journalpost.erDigitalLegeerklæring()) {
            val dokument = if (journalpost.erDigitalSøknad()) hentOriginalDokumentFraSaf(journalpost) else null
            val validertDokument =
                DokumentTilMeldingParser.parseTilMelding(dokument, kategorivurdering.avklaring)?.serialiser()
            if (validertDokument != null) {
                struktureringsvurderingRepository.lagreStrukturertDokument(kontekst.behandlingId, validertDokument)
            }
            return Fullført
        }

        return if (struktureringsvurdering == null && kategorivurdering.avklaring.kanStruktureres()) {
            FantAvklaringsbehov(
                Definisjon.DIGITALISER_DOKUMENT
            )
        } else Fullført
    }

    private fun InnsendingType.kanStruktureres(): Boolean {
        return this in listOf(InnsendingType.SØKNAD, InnsendingType.PLIKTKORT)
    }

    private fun hentOriginalDokumentFraSaf(journalpost: Journalpost): ByteArray {
        val strukturertDokument = journalpost.finnOriginal()
        requireNotNull(strukturertDokument) { "Finner ikke strukturert dokument" }
        return dokumentGateway.hentDokument(
            journalpost.journalpostId,
            strukturertDokument.dokumentInfoId
        ).dokument.readBytes()
    }
}