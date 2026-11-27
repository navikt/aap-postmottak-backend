package no.nav.aap.postmottak.avklaringsbehov.løser

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.løsning.AvklarUnder18SøknadLøsning
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.under18.Under18Repository
import no.nav.aap.postmottak.under18.Under18Aldersgruppe
import no.nav.aap.postmottak.under18.krevAldersgrunnlag
import no.nav.aap.postmottak.under18.Under18Søknadsavklaring
import no.nav.aap.postmottak.under18.Under18Søknadssituasjon

class AvklarUnder18SøknadLøser(
    private val under18Repository: Under18Repository,
) : AvklaringsbehovsLøser<AvklarUnder18SøknadLøsning> {

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarUnder18SøknadLøsning): LøsningsResultat {
        val id = kontekst.kontekst.behandlingId
        val alderssvar = løsning.fylt17ÅrOg9Måneder
        if (løsning.situasjon == null && (alderssvar == null ||
                under18Repository.krevAldersgrunnlag(id).aldersgruppe == Under18Aldersgruppe.FYLT_18)) {
            throw UgyldigForespørselException("Oppgi hvilken søknadssituasjon som er avklart.")
        }
        if (alderssvar != null && løsning.situasjon != null &&
            under18Repository.krevAldersgrunnlag(id).aldersgruppe != Under18Aldersgruppe.FYLT_18 &&
            (alderssvar || løsning.situasjon != Under18Søknadssituasjon.SØKER_FOR_UNG)) {
                throw UgyldigForespørselException("Søknadsavklaringen stemmer ikke med alderssvaret.")
        }
        val avklaring = Under18Søknadsavklaring(
            løsning.situasjon, løsning.søkerVilFortsette,
            alderssvar ?: under18Repository.hentSøknadsavklaring(id)?.fylt17ÅrOg9Måneder,
        )
        under18Repository.lagreSøknadsavklaring(id, avklaring)

        return LøsningsResultat(
            when {
                avklaring.situasjon == null -> if (alderssvar == true) "Søker er vurdert som fylt 17 år og 9 måneder"
                    else "Søker er vurdert som under 17 år og 9 måneder"
                løsning.situasjon == Under18Søknadssituasjon.SØKER_FOR_UNG -> "Henleggelse bekreftet: søker er for ung"
                avklaring.søkerVilFortsette == true -> "Søker har fylt 18 år og vil fortsette søknaden"
                else -> "Søker har fylt 18 år og vil ikke fortsette søknaden"
            }
        )
    }

    override fun forBehov(): Definisjon = Definisjon.AVKLAR_UNDER18_SØKNAD

    companion object : LøserKonstruktør<AvklarUnder18SøknadLøsning> {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): AvklaringsbehovsLøser<AvklarUnder18SøknadLøsning> = AvklarUnder18SøknadLøser(repositoryProvider.provide())
    }
}
