package no.nav.aap.verdityper.sakogbehandling

/**
 * Representerer databaseId for en behandling - er ikke ment å dele utenfor domenet.
 */
class BehandlingId(private var id: Long) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BehandlingId

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return id.toString()
    }

    fun toLong(): Long {
        return id
    }
}
