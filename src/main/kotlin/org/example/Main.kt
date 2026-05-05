package org.example

import org.example.analyzer.RootCauseAnalyzer
import org.example.database.Database
import org.example.database.EventLoader
import org.example.database.TopologyLoader
import org.example.database.createTables
import org.example.database.seedDemoData
import org.example.domain.EventType

fun main() {
    Database.startServer()

    Database.connect().use { connection ->
        createTables(connection)
        seedDemoData(connection)

        val topologyLoader = TopologyLoader(connection)
        val nodes = topologyLoader.loadNodes()
        val links = topologyLoader.loadLinks(nodes)

        val eventLoader = EventLoader(connection)
        val events = eventLoader.loadEvents(nodes)

        val analyzer = RootCauseAnalyzer(
            weights = mapOf(
                EventType.LINK_DOWN to 5, // override default due to severity
                EventType.NODE_UNREACHABLE to 2,
                EventType.DEGRADED to 1
            )
        )

        val scores = analyzer.calculateScores(events)

        println("Scores:")
        scores.forEach { (node, score) ->
            println("$node: $score")
        }
    }
}