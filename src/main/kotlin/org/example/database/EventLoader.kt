package org.example.database

import org.example.domain.EventType
import org.example.domain.NetworkEvent
import org.example.domain.Node
import java.sql.Connection

class EventLoader(
    private val connection: Connection
) {
    fun loadEvents(nodes: Set<Node>): List<NetworkEvent> {
        val nodesById = nodes.associateBy { it.id }
        val events = mutableListOf<NetworkEvent>()

        connection.createStatement().use { statement ->
            statement.executeQuery(
                """
                SELECT node_id, target_node_id, event_type, event_time
                FROM events
                ORDER BY event_time
                """.trimIndent()
            ).use { resultSet ->
                while (resultSet.next()) {
                    val node = nodesById.getValue(resultSet.getString("node_id"))

                    val targetId = resultSet.getString("target_node_id")
                    val target = targetId?.let { nodesById.getValue(it) }

                    events.add(
                        NetworkEvent(
                            node = node,
                            target = target,
                            type = EventType.valueOf(resultSet.getString("event_type")),
                            timestamp = resultSet.getTimestamp("event_time").toInstant()
                        )
                    )
                }
            }
        }

        return events
    }
}