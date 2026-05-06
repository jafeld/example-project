package org.example

import org.example.analyzer.RootCauseAnalyzer
import org.example.database.Database
import org.example.database.EventLoader
import org.example.database.TopologyLoader
import org.example.database.createTables
import org.example.database.seedDemoData
import org.example.domain.EventType
import org.example.domain.NetworkGraph

fun main() {
    Database.startServer()

    Database.connect().use { connection ->
        createTables(connection)
        seedDemoData(connection)

        val topologyLoader = TopologyLoader(connection)
        val nodes = topologyLoader.loadNodes()
        val links = topologyLoader.loadLinks(nodes)

        val graph = NetworkGraph.fromLinks(links = links)

        val eventLoader = EventLoader(connection)
        val events = eventLoader.loadEvents(nodes)

        val analyzer = RootCauseAnalyzer(
            weights = mapOf(
                EventType.LINK_DOWN to 5, // override default due to severity
                EventType.NODE_UNREACHABLE to 2,
                EventType.DEGRADED to 1
            )
        )

        val basicScores = analyzer.calculateScores(events = events)
        val graphScores = analyzer.calculateScores(
            events = events,
            graph = graph
        )

        println("Basic scores:")
        printScores(scores = basicScores)

        println()
        println("Graph-aware scores:")
        printScores(scores = graphScores)

        println()
        println("Basic root cause: ${analyzer.findRootCause(events = events)}")
        println(
            "Graph-aware root cause: ${
                analyzer.findRootCause(
                    events = events,
                    graph = graph
                )
            }"
        )
    }
}

private fun printScores(scores: Map<org.example.domain.Node, Int>) {
    scores.entries
        .sortedByDescending { it.value }
        .forEach { (node, score) ->
            println("$node: $score")
        }
}