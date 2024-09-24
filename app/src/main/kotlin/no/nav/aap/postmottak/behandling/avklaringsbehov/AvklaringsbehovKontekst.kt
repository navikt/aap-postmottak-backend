package no.nav.aap.postmottak.behandling.avklaringsbehov

import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.verdityper.flyt.FlytKontekst

class AvklaringsbehovKontekst(val bruker: Bruker, val kontekst: FlytKontekst)