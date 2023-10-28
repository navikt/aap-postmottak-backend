package no.nav.aap.behandlingsflyt.dbstuff

import java.sql.PreparedStatement

class Execute(private val preparedStatement: PreparedStatement) {
    private var resultValidator: (Int) -> Unit = {}

    fun setParams(block: Params.() -> Unit) {
        Params(preparedStatement).block()
    }

    fun setResultValidator(block: (Int) -> Unit) {
        resultValidator = block
    }

    fun execute() {
        val rowsUpdated = preparedStatement.executeUpdate()
        resultValidator(rowsUpdated)
    }

    fun executeReturnKeys(): List<Long> {
        val rowsUpdated = preparedStatement.executeUpdate()
        resultValidator(rowsUpdated)
        return preparedStatement
            .generatedKeys
            .map { it.getLong(1) }
            .toList()
    }
}
