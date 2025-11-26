package no.nav.aap.postmottak.mottak.kafka

import kotlin.time.Duration

class NoopStream : Stream {
    override fun ready() = true

    override fun live() = true

    override fun close() {}

    override fun start() {}

    override fun close(timeout: Duration) = true

}