package org.example.database

import java.sql.Connection

fun seedDemoData(connection: Connection) {
    if (hasNodes(connection)) {
        return
    }

    insertNode(connection, "A", "Core router A", "ROUTER")
    insertNode(connection, "B", "Distribution switch B", "SWITCH")
    insertNode(connection, "C", "Distribution switch C", "SWITCH")
    insertNode(connection, "D", "Access switch D", "SWITCH")

    insertLink(connection, "A", "B")
    insertLink(connection, "A", "C")
    insertLink(connection, "B", "C")
    insertLink(connection, "C", "D")
}

private fun hasNodes(connection: Connection): Boolean {
    connection.createStatement().use { statement ->
        statement.executeQuery("SELECT COUNT(*) FROM nodes").use { resultSet ->
            resultSet.next()
            return resultSet.getInt(1) > 0
        }
    }
}

private fun insertNode(
    connection: Connection,
    id: String,
    name: String,
    type: String
) {
    connection.prepareStatement(
        """
        INSERT INTO nodes (id, name, node_type)
        VALUES (?, ?, ?)
        """.trimIndent()
    ).use { statement ->
        statement.setString(1, id)
        statement.setString(2, name)
        statement.setString(3, type)
        statement.executeUpdate()
    }
}

private fun insertLink(
    connection: Connection,
    source: String,
    target: String
) {
    connection.prepareStatement(
        """
        INSERT INTO links (source_node_id, target_node_id)
        VALUES (?, ?)
        """.trimIndent()
    ).use { statement ->
        statement.setString(1, source)
        statement.setString(2, target)
        statement.executeUpdate()
    }
}