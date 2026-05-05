package org.example.analyzer

import org.example.domain.EventType
import org.example.domain.NetworkEvent
import org.example.domain.Node

class RootCauseAnalyzer(
    private val weights: Map<EventType, Int> = defaultWeights()
) {

    fun calculateScores(events: List<NetworkEvent>): Map<Node, Int> {
        val scores = mutableMapOf<Node, Int>()

        for (event in events) {
            val weight = weights[event.type] ?: 0 // Default to 0 if weight is undefined
            val targetNode = event.target ?: event.node // If target exists, add score to target, otherwise add score to self

            scores[targetNode] = scores.getOrDefault(key = targetNode, defaultValue = 0) + weight // add the score to the node's total
        }

        return scores
    }

    // Likely root cause based on highest value. ToDo: Rename and move to a util function
    fun findRootCause(events: List<NetworkEvent>): Node? {
        val scores = calculateScores(events)

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