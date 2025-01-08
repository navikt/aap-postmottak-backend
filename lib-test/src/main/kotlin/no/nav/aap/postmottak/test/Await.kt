package no.nav.aap.postmottak.test

fun <T> await(maxWait: Long = 10000, block: () -> T): T {
    val currentTime = System.currentTimeMillis()
    while (System.currentTimeMillis() - currentTime <= maxWait) {
        try {
            return block()
        } catch (_: Throwable) {
        }
        Thread.sleep(100)
    }
    return block()
}