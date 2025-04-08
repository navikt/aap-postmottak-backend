package no.nav.aap.postmottak.forretningsflyt.steg.dokumentflyt

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering.Digitaliseringsvurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering.DigitaliseringsvurderingRepository
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FantAvklaringsbehov
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.gateway.DokumentGateway
import no.nav.aap.postmottak.gateway.DokumentTilMeldingParser
import no.nav.aap.postmottak.gateway.serialiser
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.steg.StegType


class DigitaliserDokumentSteg(
    private val digitaliseringsvurderingRepository: DigitaliseringsvurderingRepository,
    private val journalpostRepository: JournalpostRepository,
    private val dokumentGateway: DokumentGateway
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            return DigitaliserDokumentSteg(
                repositoryProvider.provide(DigitaliseringsvurderingRepository::class),
                repositoryProvider.provide(JournalpostRepository::class),
                GatewayProvider.provide(DokumentGateway::class)
            )
        }

        override fun type(): StegType {
            return StegType.DIGITALISER_DOKUMENT
        }

    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val struktureringsvurdering =
            digitaliseringsvurderingRepository.hentHvisEksisterer(kontekst.behandlingId)
        val journalpost = journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)
        requireNotNull(journalpost)


        if (journalpost.erDigitalSøknad() || journalpost.erDigitalLegeerklæring() || journalpost.erDigitaltMeldekort() || journalpost.erDigitalKlage()) {
            val dokument =
                if (journalpost.erDigitalSøknad() || journalpost.erDigitaltMeldekort()) hentOriginalDokumentFraSaf(
                    journalpost
                ) else null
            val innsending = getInnsendingForBrevkode(journalpost.hoveddokumentbrevkode)
            val validertDokument =
                DokumentTilMeldingParser.parseTilMelding(dokument, innsending)?.serialiser()
            digitaliseringsvurderingRepository.lagre(
                kontekst.behandlingId, Digitaliseringsvurdering(innsending, validertDokument, null)
            )

            return Fullført
        }

        return if (struktureringsvurdering == null) {
            FantAvklaringsbehov(Definisjon.DIGITALISER_DOKUMENT)
        } else Fullført
    }

    private fun hentOriginalDokumentFraSaf(journalpost: Journalpost): ByteArray {
        val strukturertDokument = journalpost.finnOriginal()
        requireNotNull(strukturertDokument) { "Finner ikke strukturert dokument" }
        return dokumentGateway.hentDokument(
            journalpost.journalpostId,
            strukturertDokument.dokumentInfoId
        ).dokument.readBytes()
    }

    private fun getInnsendingForBrevkode(brevkode: String): InnsendingType {
        val brevkodeTilInnsendingMap = mapOf(
            Brevkoder.SØKNAD to InnsendingType.SØKNAD,
            Brevkoder.LEGEERKLÆRING to InnsendingType.LEGEERKLÆRING,
            Brevkoder.MELDEKORT to InnsendingType.MELDEKORT,
            Brevkoder.MELDEKORT_KORRIGERING to InnsendingType.MELDEKORT,
            Brevkoder.KLAGE to InnsendingType.KLAGE
        )

        return brevkodeTilInnsendingMap[Brevkoder.fraKode(brevkode)]
            ?: throw IllegalStateException("Kan ikke automatisk behanlde journalposter av type $brevkode")
    }

}