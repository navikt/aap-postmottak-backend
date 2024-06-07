package no.nav.aap.auth

import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import no.nav.aap.httpclient.tokenprovider.OidcToken


fun ApplicationCall.bruker(): Bruker {
    val navIdent = principal<JWTPrincipal>()?.getClaim("NAVident", String::class)
    if (navIdent == null) {
        error("NAVident mangler i AzureAD claims")
    }
    return Bruker(navIdent)
}

fun ApplicationCall.token(): OidcToken {
    val token: String? = (principal<JWTPrincipal>()?.payload as DecodedJWT).token
    if (token == null) {
        error("token mangler for OBO hendelse")
    }
    return OidcToken(token)
}
