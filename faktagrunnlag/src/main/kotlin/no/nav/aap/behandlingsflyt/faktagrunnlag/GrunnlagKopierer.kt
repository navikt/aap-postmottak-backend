package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.PliktkortRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningRepository
import no.nav.aap.verdityper.sakogbehandling.BehandlingId


interface Kopierbar {
    fun kopierTilAnnenBehandling(fraBehandlingId: BehandlingId, tilBehandlingId: BehandlingId)
}


/**
 * Har som ansvar å sette i stand en behandling etter opprettelse
 *
 * Knytter alle opplysninger fra forrige til den nye i en immutable state
 */

class GrunnlagKopierer(connection: DBConnection) {

    private val repositories = listOf(
        VilkårsresultatRepository(connection),
        PersonopplysningRepository(connection),
        YrkesskadeRepository(connection),
        SykdomRepository(connection),
        StudentRepository(connection),
        BistandRepository(connection),
        MeldepliktRepository(connection),
        SykepengerErstatningRepository(connection),
        ArbeidsevneRepository(connection),
        UføreRepository(connection),
        PliktkortRepository(connection),
        UnderveisRepository(connection),
        BarnRepository(connection),
        BarnetilleggRepository(connection),
        BarnVurderingRepository(connection),
        BeregningsgrunnlagRepository(connection),
        BeregningVurderingRepository(connection),
        InstitusjonsoppholdRepository(connection)
    )


    fun overfør(fraBehandlingId: BehandlingId, tilBehandlingId: BehandlingId) {
        require(fraBehandlingId != tilBehandlingId)

        repositories.forEach{ it.kopierTilAnnenBehandling(fraBehandlingId, tilBehandlingId)}
    }
}
