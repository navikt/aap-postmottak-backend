package no.nav.aap.postmottak.mottak.kafka

interface Stream {
    fun ready(): Boolean
    fun live(): Boolean
    fun close()
    fun start()
}