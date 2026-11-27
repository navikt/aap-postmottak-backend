package no.nav.aap.postmottak.test.inmemoryrepo

import java.util.concurrent.atomic.AtomicLong

object InMemoryIdSequence {
    private val behandlingIds = AtomicLong(1_000_000)
    private val personIds = AtomicLong(1_000_000)

    fun nesteBehandlingId() = behandlingIds.incrementAndGet()
    fun nestePersonId() = personIds.incrementAndGet()
}
