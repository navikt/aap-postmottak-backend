package no.nav.aap.postmottak.under18

import no.nav.aap.fordeler.InnkommendeJournalpost
import no.nav.aap.fordeler.InnkommendeJournalpostRepository
import no.nav.aap.fordeler.InnkommendeJournalpostStatus
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravGrunnlagImpl
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostInformasjonskrav
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.postmottak.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.postmottak.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.AvklarTemaRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.InformasjonskravRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.SaksnummerRepositoryImpl
import no.nav.aap.postmottak.repository.fordeler.InnkommendeJournalpostRepositoryImpl
import no.nav.aap.postmottak.repository.journalpost.JournalpostRepositoryImpl
import no.nav.aap.postmottak.repository.person.PersonRepositoryImpl
import no.nav.aap.postmottak.test.fakes.IsolertUnder18Repository
import no.nav.aap.postmottak.test.fakes.TestJournalPost

internal class Under18TestOppsett(
    private val dataSource: TestDataSource,
    private val gatewayProvider: GatewayProvider,
) {
    val repositoryRegistry = RepositoryRegistry()
        .register<AvklaringsbehovRepositoryImpl>()
        .register<BehandlingRepositoryImpl>()
        .register<JournalpostRepositoryImpl>()
        .register<PersonRepositoryImpl>()
        .register<InnkommendeJournalpostRepositoryImpl>()
        .register<AvklarTemaRepositoryImpl>()
        .register<SaksnummerRepositoryImpl>()
        .register<InformasjonskravRepositoryImpl>()
        .register<IsolertUnder18Repository>()

    fun opprettBehandling(hovedjournalpost: TestJournalPost): FlytKontekst =
        dataSource.transaction { connection ->
            val provider = repositoryRegistry.provider(connection)
            val behandlingId = provider.provide<BehandlingRepository>()
                .opprettBehandling(hovedjournalpost.journalpostId(), TypeBehandling.Under18)
            val kontekst = FlytKontekst(hovedjournalpost.journalpostId(), behandlingId, TypeBehandling.Under18)

            oppdater(provider, kontekst, JournalpostInformasjonskrav)
            provider.provide<AvklaringsbehovRepository>().opprett(
                behandlingId,
                Definisjon.AVKLAR_UNDER18_SAMTYKKE,
                StegType.AVKLAR_UNDER18_SAMTYKKE,
            )
            connection.execute(
                """
                UPDATE AVKLARINGSBEHOV_ENDRING
                SET opprettet_tid = CURRENT_TIMESTAMP
                WHERE status = 'OPPRETTET'
                    AND avklaringsbehov_id IN (
                        SELECT id FROM AVKLARINGSBEHOV
                        WHERE behandling_id = ? AND definisjon = ?
                    )
                """.trimIndent()
            ) {
                setParams {
                    setLong(1, behandlingId.id)
                    setString(2, Definisjon.AVKLAR_UNDER18_SAMTYKKE.kode.name)
                }
            }
            kontekst
        }

    fun oppdater(kontekst: FlytKontekst, krav: Informasjonskravkonstruktør): Informasjonskrav.Endret =
        dataSource.transaction { connection -> oppdater(repositoryRegistry.provider(connection), kontekst, krav) }

    private fun oppdater(
        provider: RepositoryProvider,
        kontekst: FlytKontekst,
        krav: Informasjonskravkonstruktør,
    ): Informasjonskrav.Endret {
        val endrede = InformasjonskravGrunnlagImpl(provider, gatewayProvider)
            .oppdaterFaktagrunnlagForKravliste(listOf(StegType.AVKLAR_UNDER18 to krav), kontekst)
        return if (krav in endrede) Informasjonskrav.Endret.ENDRET else Informasjonskrav.Endret.IKKE_ENDRET
    }

    fun registrerMottak(vararg journalposter: TestJournalPost) {
        dataSource.transaction { connection ->
            val repository = repositoryRegistry.provider(connection).provide<InnkommendeJournalpostRepository>()
            journalposter.forEach { journalpost ->
                repository.lagre(
                    InnkommendeJournalpost(
                        journalpostId = journalpost.journalpostId(),
                        brevkode = journalpost.brevkode.kode,
                        behandlingstema = null,
                        status = InnkommendeJournalpostStatus.EVALUERT,
                        brukerId = journalpost.brukerId,
                    )
                )
            }
        }
    }
}
