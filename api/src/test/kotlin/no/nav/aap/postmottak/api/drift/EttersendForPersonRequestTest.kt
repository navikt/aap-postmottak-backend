package no.nav.aap.postmottak.api.drift

import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.*

class EttersendForPersonRequestTest {

    @Test
    fun `validering godkjenner gyldig request`() {
        val request = request()

        val input = request.valider()

        assertThat(input.ident).isEqualTo("12345678910")
        assertThat(input.eksternReferanseId).isEqualTo(UUID.fromString(request.eksternReferanseId))
    }

    @Test
    fun `validering avviser ugyldig base64`() {
        val request = request().copy(pdfBase64 = "ikke-base64")

        assertThatThrownBy { request.valider() }
            .isInstanceOf(UgyldigForespørselException::class.java)
    }

    @Test
    fun `validering avviser innhold som ikke er pdf`() {
        val request = request().copy(pdfBase64 = Base64.getEncoder().encodeToString("ikke-pdf".toByteArray()))

        assertThatThrownBy { request.valider() }
            .isInstanceOf(UgyldigForespørselException::class.java)
    }

    @Test
    fun `validering avviser ugyldig ident`() {
        val request = request().copy(ident = "123")

        assertThatThrownBy { request.valider() }
            .isInstanceOf(UgyldigForespørselException::class.java)
    }

    private fun request() = EttersendForPersonRequest(
        ident = "12345678910",
        filNavn = "filnavn",
        pdfBase64 = Base64.getEncoder().encodeToString("%PDF-1.4\n".toByteArray()),
        eksternReferanseId = UUID.randomUUID().toString()
    )
}
