package no.nav.aap.postmottak.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepositoryImpl
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.KategoriVurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.KategorivurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering.Struktureringsvurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering.StruktureringsvurderingRepository
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FantAvklaringsbehov
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.postmottak.sakogbehandling.journalpost.Journalpost
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder

class DigitaliserDokumentSteg(
    private val struktureringsvurderingRepository: StruktureringsvurderingRepository,
    private val journalpostRepository: JournalpostRepository,
    private val kategorivurderingRepository: KategorivurderingRepository
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return DigitaliserDokumentSteg(
                StruktureringsvurderingRepository(connection),
                JournalpostRepositoryImpl(connection),
                KategorivurderingRepository(connection)
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

        return if (skalDigitaliseres(journalpost, struktureringsvurdering, kategorivurdering)) FantAvklaringsbehov(
            Definisjon.DIGITALISER_DOKUMENT
        )
        else Fullført
    }

    private fun skalDigitaliseres(
        journalpost: Journalpost,
        struktureringsvurdering: Struktureringsvurdering?,
        kategorivurdering: KategoriVurdering?
    ): Boolean {
        return !journalpost.erDigitalSøknad()
                && !journalpost.erDigitalLegeerklæring()        
                && struktureringsvurdering == null 
                && kategorivurdering?.avklaring?.kanStruktureres()!!
    }

    private fun InnsendingType.kanStruktureres(): Boolean {
        return this in listOf(InnsendingType.SØKNAD, InnsendingType.PLIKTKORT, InnsendingType.AKTIVITETSKORT)
    }
}