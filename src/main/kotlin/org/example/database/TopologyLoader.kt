package org.example.database

import org.example.domain.Link
import org.example.domain.Node
import java.sql.Connection

class TopologyLoader(
    private val connection: Connection
) {
    fun loadNodes(): Set<Node> {
        val nodes = mutableSetOf<Node>()

        connection.createStatement().use { statement ->
            statement.executeQuery(
                """
                SELECT id
                FROM nodes
                ORDER BY id
                """.trimIndent()
            ).use { resultSet ->
                while (resultSet.next()) {
                    nodes.add(Node(id = resultSet.getString("id")))
                }
            }
        }

        return nodes
    }

    fun loadLinks(nodes: Set<Node>): Set<Link> {
        val nodesById = nodes.associateBy { it.id }
        val links = mutableSetOf<Link>()

        connection.createStatement().use { statement ->
            statement.executeQuery(
                """
                SELECT source_node_id, target_node_id
                FROM links
                ORDER BY id
                """.trimIndent()
            ).use { resultSet ->
                while (resultSet.next()) {
                    val source = nodesById.getValue(resultSet.getString("source_node_id"))
                    val target = nodesById.getValue(resultSet.getString("target_node_id"))

                    links.add(Link(source, target))
                }
            }
        }

        return links
    }
}