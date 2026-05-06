package org.example.analyzer

import org.example.domain.EventType
import org.example.domain.NetworkEvent
import org.example.domain.NetworkGraph
import org.example.domain.Node

class ScoreCalculator(
    private val weights: Map<EventType, Int>
) {

    // Initial scoring approach. Not time or topology aware
    fun calculateSimpleScores(events: List<NetworkEvent>): Map<Node, Int> {
        val scores = mutableMapOf<Node, Int>()

        for (event in events) {
            val weight = weights[event.type] ?: 0 // Default to 0 if weight is undefined
            val targetNode = event.target ?: event.node // If target exists, score target. Otherwise score reporting node.

            scores[targetNode] = scores.getOrDefault(key = targetNode, defaultValue = 0) + weight
        }

        return scores
    }

    fun calculateTopologyAwareScores(
        events: List<NetworkEvent>,
        graph: NetworkGraph
    ): Map<Node, Int> {
        val scores = calculateSimpleScores(events = events).toMutableMap()

        for (event in events) {
            val weight = weights[event.type] ?: 0
            val targetNode = event.target ?: event.node
            val neighbourScore = weight / 2

            for (neighbor in graph.neighborsOf(node = targetNode)) {
                scores[neighbor] = scores.getOrDefault(key = neighbor, defaultValue = 0) + neighbourScore
            }
        }

        return scores
    }

    fun calculateTimeAwareScores(events: List<NetworkEvent>): Map<Node, Int> {
        val scores = mutableMapOf<Node, Int>()

        if (events.isEmpty()) {
            return scores
        }

        val earliestTimestamp = events.minOf { it.timestamp.toEpochMilli() }
        val latestTimestamp = events.maxOf { it.timestamp.toEpochMilli() }
        val timeWindow = (latestTimestamp - earliestTimestamp).coerceAtLeast(minimumValue = 1)

        for (event in events) {
            val weight = calculateTimeAwareWeight(
                event = event,
                earliestTimestamp = earliestTimestamp,
                timeWindow = timeWindow
            )

            val targetNode = event.target ?: event.node

            scores[targetNode] = scores.getOrDefault(key = targetNode, defaultValue = 0) + weight
        }

        return scores
    }

    fun calculateTimeAndTopologyAwareScores(
        events: List<NetworkEvent>,
        graph: NetworkGraph
    ): Map<Node, Int> {
        val scores = calculateTimeAwareScores(events = events).toMutableMap()

        if (events.isEmpty()) {
            return scores
        }

        val earliestTimestamp = events.minOf { it.timestamp.toEpochMilli() }
        val latestTimestamp = events.maxOf { it.timestamp.toEpochMilli() }
        val timeWindow = (latestTimestamp - earliestTimestamp).coerceAtLeast(minimumValue = 1)

        for (event in events) {
            val weight = calculateTimeAwareWeight(
                event = event,
                earliestTimestamp = earliestTimestamp,
                timeWindow = timeWindow
            )

            val targetNode = event.target ?: event.node
            val neighbourScore = weight / 2

            for (neighbor in graph.neighborsOf(node = targetNode)) {
                scores[neighbor] = scores.getOrDefault(key = neighbor, defaultValue = 0) + neighbourScore
            }
        }

        return scores
    }

    private fun calculateTimeAwareWeight(
        event: NetworkEvent,
        earliestTimestamp: Long,
        timeWindow: Long
    ): Int {
        val baseWeight = weights[event.type] ?: 0

        if (baseWeight == 0) {
            return 0
        }

        val eventOffset = event.timestamp.toEpochMilli() - earliestTimestamp
        val eventPosition = eventOffset.toDouble() / timeWindow.toDouble()

        // Earlier events keep more weight. Later events are reduced, but still count.
        val timeFactor = 1.0 - (eventPosition * 0.5)

        return (baseWeight * timeFactor).toInt().coerceAtLeast(minimumValue = 1)
    }
}