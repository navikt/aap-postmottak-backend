package no.nav.aap.postmottak.prosessering

import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.Tema
import no.nav.aap.postmottak.gateway.Bruker
import no.nav.aap.postmottak.gateway.BrukerIdType
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.gateway.SafDokumentInfo
import no.nav.aap.postmottak.gateway.SafDokumentvariant
import no.nav.aap.postmottak.gateway.SafJournalpost
import no.nav.aap.postmottak.gateway.SafVariantformat
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

internal object TestObjekter {

    fun lagTestJournalpost(journalpostId: JournalpostId): SafJournalpost = SafJournalpost(
        journalpostId = journalpostId.referanse,
        bruker = Bruker(
            id = "fnr",
            type = BrukerIdType.FNR,
        ),
        dokumenter = listOf(
            SafDokumentInfo(
                dokumentInfoId = "1",
                brevkode = Brevkoder.SØKNAD.kode,
                tittel = "tittel",
                dokumentvarianter = listOf(
                    SafDokumentvariant(variantformat = SafVariantformat.ORIGINAL, filtype = "json")
                )
            )
        ),
        journalstatus = Journalstatus.MOTTATT,
        tema = Tema.AAP.name,
        relevanteDatoer = emptyList()
    )

}