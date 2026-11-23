package no.nav.aap.postmottak.faktagrunnlag

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.steg.StegType
import java.time.Instant

class InformasjonskravGrunnlagImpl(
    private val repositoryProvider: RepositoryProvider,
    private val gatewayProvider: GatewayProvider
) : InformasjonskravGrunnlag {
    private val informasjonskravRepository = repositoryProvider.provide<InformasjonskravRepository>()

    override fun oppdaterFaktagrunnlagForKravliste(
        kravkonstruktører: List<Pair<StegType, Informasjonskravkonstruktør>>,
        kontekst: FlytKontekst
    ): List<Informasjonskravkonstruktør> {
        val oppdateringer = kravkonstruktører.map { (_, konstruktør) ->
            val (input, endret) = oppdaterInformasjonskrav(
                konstruktør.konstruer(repositoryProvider, gatewayProvider),
                kontekst,
            )
            Triple(konstruktør, input, endret)
        }

        informasjonskravRepository.registrerOppdateringer(
            journalpostId = kontekst.journalpostId,
            behandlingId = kontekst.behandlingId,
            informasjonskrav = oppdateringer.associate { (konstruktør, input) -> konstruktør.navn to input },
            oppdatert = Instant.now(),
        )
        return oppdateringer.filter { it.third == Informasjonskrav.Endret.ENDRET }.map { it.first }
    }

    private fun <Input : InformasjonskravInput, Registerdata : InformasjonskravRegisterdata> oppdaterInformasjonskrav(
        informasjonskrav: Informasjonskrav<Input, Registerdata>,
        kontekst: FlytKontekst,
    ): Pair<InformasjonskravInput, Informasjonskrav.Endret> {
        val input = informasjonskrav.klargjør(kontekst)
        val registerdata = informasjonskrav.hentData(input)
        return input to informasjonskrav.oppdater(input, registerdata, kontekst)
    }
}
