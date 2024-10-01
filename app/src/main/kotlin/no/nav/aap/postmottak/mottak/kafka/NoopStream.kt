package no.nav.aap.postmottak.mottak.kafka

class NoopStream : Stream {
    override fun ready() = true

    override fun live() = true

    override fun close() {}

    override fun start() {}

}