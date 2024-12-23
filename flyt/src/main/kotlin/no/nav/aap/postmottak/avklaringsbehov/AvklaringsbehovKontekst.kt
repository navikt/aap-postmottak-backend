package no.nav.aap.postmottak.avklaringsbehov

import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst

class AvklaringsbehovKontekst(val bruker: Bruker, val kontekst: FlytKontekst)