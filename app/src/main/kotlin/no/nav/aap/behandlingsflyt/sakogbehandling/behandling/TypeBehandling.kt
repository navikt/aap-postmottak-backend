package no.nav.aap.behandlingsflyt.sakogbehandling.behandling

enum class TypeBehandling(identifikator: String) {

    Førstegangsbehandling("ae0034"),
    Revurdering("ae0028"),
    Tilbakekreving(""),
    Klage(""),
    Anke("")
}