package no.nav.aap.postmottak.avklaringsbehov

import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Status
import no.nav.aap.postmottak.kontrakt.steg.StegType

class AvklaringsbehovService() {
    /** Oppdater tilstanden på avklaringsbehovet [definisjon], slik at kvalitetssikring,
     * to-trinnskontroll og tilbakeflyt blir riktig.
     *
     * For at kvalitetssikring og totrinnskontroll vises for riktig steg, så er det
     * viktig at avklaringsbehovet har rett status. Ved å bruke denne funksjonen
     * ivaretar man det.
     *
     * For at flyten skal bli riktig hvis man beveger seg fram og tilbake i flyten,
     * så er det viktig at et steg rydder opp etter seg når det viser seg at steget
     * ikke er relevant allikevel. Denne funksjonen hjelper også med det.
     */
    fun oppdaterAvklaringsbehov(
        avklaringsbehovene: Avklaringsbehovene,
        definisjon: Definisjon,

        /** Skal vedtaket inneholde en menneskelig vurdering av [definisjon]?
         *
         * Det er viktig å svare på det mer generelle spørsmålet *om vedtaket*
         * skal inneholde en menneskelig vurdering. Ikke om nå-tilstanden av behandlingen
         * har behov for en menneskelig vurdering. Grunnen er at det vil være behov for totrinnskontroll hvis vedtaket inneholder
         * en menneskelig vurdering, selv om siste gjennomkjøring av steget
         * ikke løftet avklaringsbehovet.
         *
         * En egenskap denne funksjonen må ha:
         * Hvis `vedtakBehøverVurdering() == true` og noen løser
         * (avklaringsbehovet)[definisjon], så er fortsatt `vedtakBehøverVurdering() == true`.
         *
         * @return Skal returnere `true` hvis behandlingen kommer til å inneholde
         * en menneskelig vurdering av [definisjon].
         */
        vedtakBehøverVurdering: () -> Boolean,
        erTilstrekkeligVurdert: () -> Boolean

    ) {
        require(definisjon.løsesISteg != StegType.UDEFINERT)
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)

        if (vedtakBehøverVurdering()) {
            if (avklaringsbehov == null || !avklaringsbehov.harAvsluttetStatusIHistorikken() || avklaringsbehov.status() == Status.AVBRUTT) {
                /* ønsket tilstand: OPPRETTET */
                when (avklaringsbehov?.status()) {
                    Status.OPPRETTET -> {
                        /* ønsket tilstand er OPPRETTET */
                    }

                    null, Status.AVBRUTT ->
                        avklaringsbehovene.leggTil(
                            listOf(definisjon),
                            definisjon.løsesISteg,
                        )

                    Status.AVSLUTTET ->
                        error("Ikke mulig: fikk ${avklaringsbehov.status()}")

                    Status.SENDT_TILBAKE_FRA_BESLUTTER -> TODO()
                }
            } else if (erTilstrekkeligVurdert()) {
                /* ønsket tilstand: ... */
                when (avklaringsbehov.status()) {
                    Status.OPPRETTET, Status.AVBRUTT ->
                        avklaringsbehovene.internalAvslutt(definisjon)

                    Status.AVSLUTTET,
                    Status.SENDT_TILBAKE_FRA_BESLUTTER,
                        -> {
                        /* uendret status */
                    }
                }
            } else {
                /* ønsket tilstand: OPPRETTET */
                when (avklaringsbehov.status()) {
                    Status.OPPRETTET -> {
                        /* forbli OPPRETTET */
                    }

                    Status.AVSLUTTET,
                    Status.SENDT_TILBAKE_FRA_BESLUTTER,
                    Status.AVBRUTT -> {
                        avklaringsbehovene.leggTil(
                            listOf(definisjon),
                            definisjon.løsesISteg,
                        )
                    }
                }
            }
        } else /* vedtaket behøver ikke vurdering */ {
            /* ønsket tilstand: ikke eksistere (null) eller AVBRUTT. */
            when (avklaringsbehov?.status()) {
                null,
                Status.AVBRUTT -> {
                    /* allerede ønsket tilstand */
                }

                Status.OPPRETTET,
                Status.AVSLUTTET,
                Status.SENDT_TILBAKE_FRA_BESLUTTER,
                    -> {
                    avklaringsbehovene.internalAvbryt(definisjon)
                }
            }
        }
    }
}