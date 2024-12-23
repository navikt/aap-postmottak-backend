package no.nav.aap.postmottak.avklaringsbehov.løser

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.lookup.repository.RepositoryRegistry
import no.nav.aap.postmottak.repository.faktagrunnlag.AvklarTemaRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.KategorivurderingRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.SaksnummerRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.StruktureringsvurderingRepositoryImpl
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.reflect.full.primaryConstructor

class AvklaringsbehovsLøserTest {
    @BeforeEach
    fun setup() {
        RepositoryRegistry.register<SaksnummerRepositoryImpl>()
            .register<AvklarTemaRepositoryImpl>()
            .register<StruktureringsvurderingRepositoryImpl>()
            .register<KategorivurderingRepositoryImpl>()
    }

    @Test
    fun `alle subtyper skal ha unik verdi`() {
        val utledSubtypes = AvklaringsbehovsLøser::class.sealedSubclasses
        InitTestDatabase.dataSource.transaction { dbConnection ->
            val løsningSubtypes = utledSubtypes.map { it.primaryConstructor!!.call(dbConnection).forBehov() }.toSet()

            Assertions.assertThat(løsningSubtypes).hasSize(utledSubtypes.size)
        }
    }
}