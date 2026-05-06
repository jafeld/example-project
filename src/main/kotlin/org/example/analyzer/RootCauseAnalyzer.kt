package org.example.analyzer

import org.example.domain.EventType
import org.example.domain.NetworkEvent
import org.example.domain.NetworkGraph
import org.example.domain.Node

class RootCauseAnalyzer(
    private val weights: Map<EventType, Int> = defaultWeights()
) {

    fun calculateScores(events: List<NetworkEvent>): Map<Node, Int> {
        val scores = mutableMapOf<Node, Int>()

        for (event in events) {
            val weight = weights[event.type] ?: 0 // Default to 0 if weight is undefined
            val targetNode = event.target ?: event.node // If target exists, score target. Otherwise score reporting node.

            scores[targetNode] = scores.getOrDefault(key = targetNode, defaultValue = 0) + weight
        }

        return scores
    }

    fun calculateScores(
        events: List<NetworkEvent>,
        graph: NetworkGraph
    ): Map<Node, Int> {
        val scores = calculateScores(events = events).toMutableMap()

        for (event in events) {
            val weight = weights[event.type] ?: 0
            val targetNode = event.target ?: event.node

            // Give connected neighbours a smaller score because they may be part of the same failure area.
            // ToDo: Change to input or global parameter so weighing can be better controlled
            val neighbourScore = weight / 2

            for (neighbor in graph.neighborsOf(node = targetNode)) {
                scores[neighbor] = scores.getOrDefault(key = neighbor, defaultValue = 0) + neighbourScore
            }
        }

        return scores
    }

    // Likely root cause based on highest score
    fun findRootCause(events: List<NetworkEvent>): Node? {
        val scores = calculateScores(events = events)

        return scores.maxByOrNull { it.value }?.key
    }

    // Likely root cause based on highest score, including simple topology awareness
    fun findRootCause(
        events: List<NetworkEvent>,
        graph: NetworkGraph
    ): Node? {
        val scores = calculateScores(
            events = events,
            graph = graph
        )

        return scores.maxByOrNull { it.value }?.key
    }

    companion object {
        fun defaultWeights(): Map<EventType, Int> =
            mapOf(
                EventType.LINK_DOWN to 3,
                EventType.NODE_UNREACHABLE to 2,
                EventType.DEGRADED to 1
            )
    }
}