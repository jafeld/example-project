package org.example.database

import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant

fun seedDemoData(connection: Connection) {
    if (hasNodes(connection)) {
        return
    }

    insertNode(connection, id = "A", name = "Core router A", type = "ROUTER")
    insertNode(connection, id = "B", name = "Distribution switch B",  type = "SWITCH")
    insertNode(connection, id = "C", name = "Distribution switch C",  type = "SWITCH")
    insertNode(connection, id = "D", name = "Access switch D",  type = "SWITCH")

    insertLink(connection, source = "A", target = "B")
    insertLink(connection, source = "A", target = "C")
    insertLink(connection, source = "B", target = "C")
    insertLink(connection, source = "C", target = "D")

    val startTime = Instant.parse("2026-05-05T10:00:00Z")

    insertEvent(connection, node = "A", target = "C", type = "LINK_DOWN", time = startTime)
    insertEvent(connection, node = "B", target = "C", type = "NODE_UNREACHABLE", time = startTime.plusSeconds(1))
    insertEvent(connection, node = "D", target = "C", type = "NODE_UNREACHABLE", time = startTime.plusSeconds(2))

    insertEvent(connection, node = "A", target = "B", type = "LINK_DOWN", time = startTime.plusSeconds(3))
    insertEvent(connection, node = "C", target = "B", type = "DEGRADED", time = startTime.plusSeconds(4))

    insertEvent(connection, node = "D", target = null, type = "DEGRADED", time = startTime.plusSeconds(8))
    insertEvent(connection, node = "B", target = null, type = "DEGRADED", time = startTime.plusSeconds(9))

    insertEvent(connection, node = "C", target = "A", type = "LINK_UP", time = startTime.plusSeconds(10))
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