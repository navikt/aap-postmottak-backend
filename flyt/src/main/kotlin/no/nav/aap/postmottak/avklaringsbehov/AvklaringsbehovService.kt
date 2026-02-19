package no.nav.aap.postmottak.avklaringsbehov

import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Status.AVBRUTT
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Status.AVSLUTTET
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Status.OPPRETTET
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Status.SENDT_TILBAKE_FRA_BESLUTTER
import no.nav.aap.postmottak.kontrakt.steg.StegType


class AvklaringsbehovService(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        avklaringsbehovRepository = repositoryProvider.provide(),
    )

    fun oppdaterAvklaringsbehov(
        definisjon: Definisjon,
        vedtakBehøverVurdering: () -> Boolean,
        erTilstrekkeligVurdert: () -> Boolean,
        kontekst: FlytKontekst
    ) {
        require(definisjon.løsesISteg != StegType.UDEFINERT)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)

        if (vedtakBehøverVurdering()) {
            if (avklaringsbehov == null || !avklaringsbehov.harAvsluttetStatusIHistorikken() || avklaringsbehov.status() == AVBRUTT) {
                /* ønsket tilstand: OPPRETTET */
                when (avklaringsbehov?.status()) {
                    OPPRETTET -> {
                        /* ønsket tilstand er OPPRETTET */
                    }

                    null, AVBRUTT ->
                        avklaringsbehovene.leggTil(
                            definisjon,
                            definisjon.løsesISteg,
                        )

                    SENDT_TILBAKE_FRA_BESLUTTER,
                    AVSLUTTET ->
                        error("Ikke mulig: fikk ${avklaringsbehov.status()}")
                }
            } else if (erTilstrekkeligVurdert()) {
                /* ønsket tilstand: ... */
                when (avklaringsbehov.status()) {
                    OPPRETTET, AVBRUTT ->
                        avklaringsbehovene.internalAvslutt(definisjon)

                    AVSLUTTET,
                    SENDT_TILBAKE_FRA_BESLUTTER,
                        -> {
                        /* uendret status */
                    }
                }
            } else {
                /* ønsket tilstand: OPPRETTET */
                when (avklaringsbehov.status()) {
                    OPPRETTET -> {
                        /* forbli OPPRETTET */

                    }

                    AVSLUTTET,
                    SENDT_TILBAKE_FRA_BESLUTTER,
                    AVBRUTT -> {
                        avklaringsbehovene.leggTil(
                            definisjon,
                            definisjon.løsesISteg,
                        )
                    }
                }
            }
        } else /* vedtaket behøver ikke vurdering */ {
            /* ønsket tilstand: ikke eksistere (null) eller AVBRUTT. */
            when (avklaringsbehov?.status()) {
                null,
                AVBRUTT -> {
                    /* allerede ønsket tilstand */
                }

                OPPRETTET,
                AVSLUTTET,
                SENDT_TILBAKE_FRA_BESLUTTER,
                    -> {
                    avklaringsbehovene.internalAvbryt(definisjon)
                }
            }
        }
    }
}