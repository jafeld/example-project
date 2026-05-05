package org.example.database

import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant

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

    val startTime = Instant.parse("2026-05-05T10:00:00Z")

    insertEvent(connection, "A", "C", "LINK_DOWN", startTime)
    insertEvent(connection, "B", "C", "NODE_UNREACHABLE", startTime.plusSeconds(1))
    insertEvent(connection, "D", "C", "NODE_UNREACHABLE", startTime.plusSeconds(2))
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

private fun insertEvent(
    connection: Connection,
    node: String,
    target: String?,
    type: String,
    time: Instant
) {
    connection.prepareStatement(
        """
        INSERT INTO events (node_id, target_node_id, event_type, event_time)
        VALUES (?, ?, ?, ?)
        """.trimIndent()
    ).use { statement ->
        statement.setString(1, node)
        statement.setString(2, target)
        statement.setString(3, type)
        statement.setTimestamp(4, Timestamp.from(time))
        statement.executeUpdate()
    }
}