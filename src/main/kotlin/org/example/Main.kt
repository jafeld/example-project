package org.example

import org.example.analyzer.RootCauseAnalyzer
import org.example.database.Database
import org.example.database.EventLoader
import org.example.database.TopologyLoader
import org.example.database.createTables
import org.example.database.seedDemoData
import org.example.domain.NetworkGraph
import org.example.domain.RootCauseResult

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

        val analyzer = RootCauseAnalyzer()

        val rootCauseResults = analyzer.findRootCauseResults(
            events = events,
            graph = graph
        )

        println("Root cause candidates")
        printRootCauseResults(results = rootCauseResults)
    }
}

private fun printRootCauseResults(results: List<RootCauseResult>) {
    results.forEach { result ->
        println(
            "${result.rootCause}: " +
                    "score=${"%.2f".format(result.score)}, " +
                    "confidence=${"%.2f".format(result.confidence)}, " +
                    "reason=${result.reason}"
        )
    }
}