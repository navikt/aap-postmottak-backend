package no.nav.aap.postmottak.api.test

import com.papsign.ktor.openapigen.annotations.parameters.PathParam

data class TilknyttedeJournalposterRequest(
    @param:PathParam("Dokumentinfo-ID det skal finnes gjenbruk av")
    val dokumentInfoId: String
)
