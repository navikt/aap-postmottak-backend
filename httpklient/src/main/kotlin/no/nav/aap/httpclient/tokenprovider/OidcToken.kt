package no.nav.aap.httpclient.tokenprovider

import com.auth0.jwt.JWT
import java.time.Instant
import java.util.*

class OidcToken(accessToken: String) {

    private val accessToken = JWT.decode(accessToken)

    fun token(): String {
        return accessToken.token
    }

    fun expires(): Date {
        return accessToken.expiresAt
    }

    fun isNotExpired(): Boolean {
        return accessToken.expiresAt.after(Date.from(Instant.now().minusSeconds(30)))
    }
}