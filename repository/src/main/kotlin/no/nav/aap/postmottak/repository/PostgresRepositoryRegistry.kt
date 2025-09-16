package no.nav.aap.postmottak.repository

import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.motor.FlytJobbRepositoryImpl
import no.nav.aap.postmottak.forretningsflyt.gjenopptak.GjenopptakRepositoryImpl
import no.nav.aap.postmottak.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.postmottak.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.AvklarTemaRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.DigitaliseringsvurderingRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.OverleveringVurderingRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.SaksnummerRepositoryImpl
import no.nav.aap.postmottak.repository.fordeler.InnkommendeJournalpostRepositoryImpl
import no.nav.aap.postmottak.repository.fordeler.ManuellFordelingRepositoryImpl
import no.nav.aap.postmottak.repository.fordeler.RegelRepositoryImpl
import no.nav.aap.postmottak.repository.journalpost.JournalpostRepositoryImpl
import no.nav.aap.postmottak.repository.lås.TaSkriveLåsRepositoryImpl
import no.nav.aap.postmottak.repository.person.PersonRepositoryImpl

val postgresRepositoryRegistry = RepositoryRegistry()
        .register<AvklaringsbehovRepositoryImpl>()
        .register<BehandlingRepositoryImpl>()
        .register<AvklarTemaRepositoryImpl>()
        .register<SaksnummerRepositoryImpl>()
        .register<DigitaliseringsvurderingRepositoryImpl>()
        .register<JournalpostRepositoryImpl>()
        .register<TaSkriveLåsRepositoryImpl>()
        .register<PersonRepositoryImpl>()
        .register<InnkommendeJournalpostRepositoryImpl>()
        .register<RegelRepositoryImpl>()
        .register<OverleveringVurderingRepositoryImpl>()
        .register<FlytJobbRepositoryImpl>()
        .register<GjenopptakRepositoryImpl>()
        .register<ManuellFordelingRepositoryImpl>()
