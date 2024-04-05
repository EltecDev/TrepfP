package mx.eltec.oxxomonitor.Sync

import java.sql.DriverManager
import java.sql.SQLException

class DatabaseHandlerFinal(
    private val URL: String
    , private val USER: String
    , private val PASS: String
) {
    fun executeQuery(query: String): List<Map<String, Any>> {
        val result = mutableListOf<Map<String, Any>>()

        try {
            DriverManager.getConnection(
                URL,
                USER,
                PASS
            ).use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery(query).use { resultSet ->
                        while (resultSet.next()) {
                            val row = mutableMapOf<String, Any>()
                            val metaData = resultSet.metaData
                            for (i in 1..metaData.columnCount) {
                                val columnName = metaData.getColumnName(i)
                                val columnValue = resultSet.getObject(i)
                                // Verifica si columnValue es nulo y, si es así, asigna un valor predeterminado (por ejemplo, "")
                                row[columnName] = columnValue ?: ""
                            }
                            result.add(row)
                        }
                    }
                }
            }
        } catch (exc: SQLException) {
            result.add(mapOf(
                "Exception" to exc.message.toString()
            ))
            exc.printStackTrace()
        }

        return result
    }
}

