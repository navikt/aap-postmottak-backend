package no.nav.aap.postmottak.mottak.kafka

import kotlin.time.Duration

interface Stream {
    fun ready(): Boolean
    fun live(): Boolean
    fun close()
    fun start()
    fun close(timeout: Duration): Boolean
}