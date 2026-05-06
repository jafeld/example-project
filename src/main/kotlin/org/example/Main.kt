package org.example

import org.example.analyzer.RootCauseAnalyzer
import org.example.analyzer.ScoreCalculator
import org.example.database.Database
import org.example.database.EventLoader
import org.example.database.TopologyLoader
import org.example.database.createTables
import org.example.database.seedDemoData
import org.example.domain.EventType
import org.example.domain.NetworkGraph
import org.example.domain.Node
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

        val weights = mapOf(
            EventType.LINK_DOWN to 5, // override default due to severity
            EventType.NODE_UNREACHABLE to 2,
            EventType.DEGRADED to 1
        )

        val analyzer = RootCauseAnalyzer(weights = weights)
        val scoreCalculator = ScoreCalculator(weights = weights)

        val simpleScores = scoreCalculator.calculateSimpleScores(events = events)

        val topologyScores = scoreCalculator.calculateTopologyAwareScores(
            events = events,
            graph = graph
        )

        val timeScores = scoreCalculator.calculateTimeAwareScores(events = events)

        val timeAndTopologyScores = scoreCalculator.calculateTimeAndTopologyAwareScores(
            events = events,
            graph = graph
        )

        println("Simple scores (baseline)")
        printScores(scores = simpleScores)

        println()
        println("Topology-aware scores")
        printScores(scores = topologyScores)

        println()
        println("Time-aware scores")
        printScores(scores = timeScores)

        println()
        println("Time + Topology-aware scores")
        printScores(scores = timeAndTopologyScores)

        println()
        val rootCauseResults = analyzer.findRootCauseResults(
            events = events,
            graph = graph
        )

        println()
        println("Root cause candidates")
        printRootCauseResults(results = rootCauseResults)
    }
}

private fun printScores(scores: Map<Node, Double>) {
    scores.entries
        .sortedByDescending { it.value }
        .forEach { (node, score) ->
            println("${node.id}: ${"%.2f".format(score)}")
        }
}

private fun printRootCauseResults(results: List<RootCauseResult>) {
    results.forEach { result ->
        println(
            "${result.node.id}: " +
                    "score=${"%.2f".format(result.score)}, " +
                    "confidence=${"%.2f".format(result.confidence)}, " +
                    "reason=${result.reason}"
        )
    }
}