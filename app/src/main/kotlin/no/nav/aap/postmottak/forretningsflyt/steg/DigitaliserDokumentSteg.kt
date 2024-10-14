package no.nav.aap.postmottak.forretningsflyt.steg

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepositoryImpl
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering.StruktureringsvurderingRepository
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.klient.joark.Journalpost
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder

class DigitaliserDokumentSteg(
    private val struktureringsvurderingRepository: StruktureringsvurderingRepository,
    private val journalpostRepository: JournalpostRepository
) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return DigitaliserDokumentSteg(StruktureringsvurderingRepository(connection), JournalpostRepositoryImpl(connection))
        }

        override fun type(): StegType {
            return StegType.DIGITALISER_DOKUMENT
        }

    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {

        val struktureringsvurdering = struktureringsvurderingRepository.hentStruktureringsavklaring(kontekst.behandlingId)
        val journalpost = journalpostRepository.hentHvisEksisterer(kontekst.behandlingId)

        require(journalpost is Journalpost.MedIdent)

        return if (!journalpost.kanBehandlesAutomatisk() && struktureringsvurdering == null) StegResultat(
            listOf(
                Definisjon.DIGITALISER_DOKUMENT
            )
        )
        else StegResultat()
    }
}