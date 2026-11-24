package no.nav.aap.postmottak.faktagrunnlag

import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst

interface InformasjonskravInput
interface InformasjonskravRegisterdata

object IngenInput : InformasjonskravInput
object IngenRegisterData : InformasjonskravRegisterdata

interface Informasjonskrav<Input : InformasjonskravInput, Registerdata : InformasjonskravRegisterdata> {
    enum class Endret {
        ENDRET,
        IKKE_ENDRET,
    }

    /** Klargjør input fra databasen, uten kall mot eksterne systemer. */
    fun klargjør(kontekst: FlytKontekst): Input

    /** Hent registerdata uten å lese eller skrive til databasen. */
    fun hentData(input: Input): Registerdata

    /** Oppdater lagrede opplysninger uten kall mot eksterne systemer. */
    fun oppdater(input: Input, registerdata: Registerdata, kontekst: FlytKontekst): Endret
}
