package no.nav.aap.postmottak.gateway

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.postmottak.JournalføringsType
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.avklaringsbehov.løsning.ForenkletDokument
import no.nav.aap.postmottak.journalføringCounter
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import java.util.*

private const val MASKINELL_JOURNALFØRING_ENHET = "9999"

class JournalføringService(
    private val safGraphqlKlient: JournalpostGateway,
    private val dokarkivGateway: DokarkivGateway,
    private val enhetsregisteretGateway: EnhetsregisteretGateway,
    val prometheus: MeterRegistry = SimpleMeterRegistry(),
) {

    companion object {
        fun konstruer(
            safGraphqlKlient: JournalpostGateway,
            dokarkivGateway: DokarkivGateway,
            enhetsregisteretGateway: EnhetsregisteretGateway,
            prometheus: MeterRegistry = SimpleMeterRegistry(),
        ): JournalføringService {
            return JournalføringService(
                safGraphqlKlient,
                dokarkivGateway,
                enhetsregisteretGateway,
                prometheus
            )
        }
    }

    constructor(gatewayProvider: GatewayProvider) : this(
        safGraphqlKlient = gatewayProvider.provide(JournalpostGateway::class),
        dokarkivGateway = gatewayProvider.provide(),
        enhetsregisteretGateway = gatewayProvider.provide(EnhetsregisteretGateway::class),
        prometheus = PrometheusProvider.prometheus,
    )

    fun endreTema(journalpostId: JournalpostId, tema: String, endretAv: Bruker?) {
        dokarkivGateway.endreTema(journalpostId, tema, endretAv)
    }

    fun førJournalpostPåFagsak(
        journalpostId: JournalpostId,
        ident: Ident,
        fagsakId: String,
        tema: String = "AAP",
        fagsystem: Fagsystem = Fagsystem.KELVIN,
        tittel: String? = null,
        avsenderMottaker: AvsenderMottakerDto? = null,
        dokumenter: List<ForenkletDokument>? = null,
        endretAv: Bruker?,
    ) {
        dokarkivGateway.førJournalpostPåFagsak(
            journalpostId = journalpostId,
            ident = ident,
            fagsakId = fagsakId,
            tema = tema,
            fagsystem = fagsystem,
            tittel = tittel,
            avsenderMottaker = hentMottaker(avsenderMottaker, journalpostId),
            dokumenter = dokumenter,
            endretAv = endretAv
        )
    }

    fun hentMottaker(avsenderMottaker: AvsenderMottakerDto?, journalpostId: JournalpostId): AvsenderMottakerDto? {
        val avsender = avsenderMottaker?.entenKunNavnEllerIdOgType() ?: hentAvsenderMottakerOmNødvendig(
            journalpostId
        )

        return if (avsender?.idType == AvsenderMottakerDto.IdType.ORGNR && avsender.id != null) {
            val orgnr = Organisasjonsnummer(avsender.id)
            val eregRespons = enhetsregisteretGateway.hentOrganisasjon(orgnr)

            if (eregRespons?.fantIkke == true) {
                requireNotNull(avsenderMottaker?.navn) { "Navn på avsender må være satt for utenlandsk ORGNR" }
                AvsenderMottakerDto(
                    id = avsender.id,
                    idType = AvsenderMottakerDto.IdType.UTL_ORG,
                    navn = avsenderMottaker.navn
                )
            } else {
                avsender
            }
        } else avsender
    }

    fun førJournalpostPåGenerellSak(
        journalpost: Journalpost,
        tema: String = "AAP",
        tittel: String? = null,
        avsenderMottaker: AvsenderMottakerDto? = null,
        dokumenter: List<ForenkletDokument>? = null,
        endretAv: Bruker?,
    ) {
        dokarkivGateway.førJournalpostPåGenerellSak(
            journalpost = journalpost,
            tema = tema,
            tittel = tittel,
            avsenderMottaker = avsenderMottaker?.entenKunNavnEllerIdOgType() ?: hentAvsenderMottakerOmNødvendig(
                journalpost.journalpostId
            ),
            dokumenter = dokumenter,
            endretAv = endretAv
        )
    }

    fun kopierJournalpost(journalpost: Journalpost): JournalpostId {
        return dokarkivGateway.kopierJournalpost(journalpost)
    }

    fun opprettJournalpostForPerson(
        ident: Ident,
        pdf: ByteArray,
        filNavn: String,
        eksternReferanseId: UUID?,
    ): JournalpostId {
        return dokarkivGateway.opprettJournalpostForPerson(
            ident = ident,
            pdf = pdf,
            filNavn = filNavn,
            eksternReferanseId = eksternReferanseId
        )
    }

    fun ferdigstillJournalpostMaskinelt(
        journalpostId: JournalpostId,
        journalførtAv: Bruker?,
    ) {
        ferdigstillJournalpost(journalpostId, MASKINELL_JOURNALFØRING_ENHET, journalførtAv)
    }

    fun ferdigstillJournalpost(
        journalpostId: JournalpostId,
        journalfoerendeEnhet: String,
        journalførtAv: Bruker?,
    ) {
        dokarkivGateway.ferdigstillJournalpost(journalpostId, journalfoerendeEnhet, journalførtAv)
        prometheus.journalføringCounter(type = JournalføringsType.automatisk).increment()
    }


    private fun hentAvsenderMottakerOmNødvendig(journalpostId: JournalpostId): AvsenderMottakerDto? {
        val safJournalpost = safGraphqlKlient.hentJournalpost(journalpostId)
        val avsenderMottaker = safJournalpost.avsenderMottaker
        val bruker = safJournalpost.bruker!!
        return if (avsenderMottaker?.id == null && bruker.type in listOf(BrukerIdType.FNR, BrukerIdType.ORGNR)) {
            AvsenderMottakerDto(
                id = safJournalpost.bruker.id,
                idType = AvsenderMottakerDto.IdType.valueOf(bruker.type?.name!!),
            )
        } else {
            null
        }
    }
}

data class AvsenderMottakerDto(
    val id: String?,
    val idType: IdType?,
    val navn: String? = null,
) {
    /**
     * Navn må være satt ELLER både id og idType må være satt
     * Helst bør alle tre være satt
     * Se https://confluence.adeo.no/spaces/BOA/pages/313346834/oppdaterJournalpost
     **/
    fun erGyldig(): Boolean = navn != null || (id != null && idType != null)

    /**
     * Dokarkiv aksepterer kun disse ved oppdatering/ferdigstilling av journalpost
     **/
    enum class IdType {
        FNR, ORGNR, HPRNR, UTL_ORG,
    }

    fun entenKunNavnEllerIdOgType(): AvsenderMottakerDto {
        return if (id != null && idType != null) {
            this.copy(navn = null)
        } else {
            this.copy(id = null, idType = null)
        }
    }
}

enum class Fagsystem {
    KELVIN,
    AO01, // Arena
    FS22 // Generell sak
}

data class JournalpostSak(
    val sakstype: Sakstype = Sakstype.FAGSAK,
    val fagsakId: String? = null,
    val fagsaksystem: Fagsystem? = Fagsystem.KELVIN
)