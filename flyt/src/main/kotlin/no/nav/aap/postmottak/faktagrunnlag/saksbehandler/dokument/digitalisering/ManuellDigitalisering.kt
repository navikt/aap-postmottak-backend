package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksvurdering
import no.nav.aap.postmottak.gateway.DokumentTilMeldingParser
import no.nav.aap.postmottak.gateway.serialiser
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import org.slf4j.LoggerFactory
import java.time.LocalDate

object ManuellDigitalisering {
    private val log = LoggerFactory.getLogger(ManuellDigitalisering::class.java)

    fun lagVurdering(
        journalpost: Journalpost,
        kategori: InnsendingType,
        strukturertDokument: String?,
        søknadsdato: LocalDate?,
        saksvurdering: Saksvurdering?
    ): Digitaliseringsvurdering {
        require((søknadsdato == null) xor (kategori == InnsendingType.SØKNAD || kategori == InnsendingType.MELDEKORT)) {
            "Søknadsdato skal kun settes for søknader eller meldekort"
        }
        require(søknadsdato == null || !søknadsdato.isAfter(journalpost.mottattDato)) {
            "Søknadsdato kan ikke være etter registrert dato. Søknadsdato: $søknadsdato. Mottatt-dato: ${journalpost.mottattDato}"
        }
        if (kategori == InnsendingType.KLAGE) {
            checkNotNull(saksvurdering) { "Saksvurdering mangler ved digitalisering av klage" }
            require(!saksvurdering.opprettetNy) {
                "Klage skal knyttes mot eksisterende sak"
            }
        }

        log.info("Parser dokument med journalpostId ${journalpost.journalpostId}.")
        val dokument = DokumentTilMeldingParser.parseTilMelding(strukturertDokument, kategori)

        return Digitaliseringsvurdering(
            kategori = kategori,
            strukturertDokument = dokument?.serialiser(),
            søknadsdato = søknadsdato,
            digitalisertManueltGjennomPostmottak = true
        )
    }
}
