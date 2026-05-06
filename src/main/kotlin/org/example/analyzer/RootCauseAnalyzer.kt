package org.example.analyzer

import org.example.domain.EventType
import org.example.domain.NetworkEvent
import org.example.domain.NetworkGraph
import org.example.domain.Node

class RootCauseAnalyzer(
    private val weights: Map<EventType, Int> = defaultWeights()
) {
    private val scoreCalculator = ScoreCalculator(weights = weights)

    // Initial function to return highest scoring node as likely root cause
    fun findRootCause(events: List<NetworkEvent>): Node? {
        val scores = scoreCalculator.calculateSimpleScores(events = events)
        return scores.maxByOrNull { it.value }?.key
    }

    // Uses improved scoring approach with time and topology awareness
    fun findRootCause(
        events: List<NetworkEvent>,
        graph: NetworkGraph
    ): Node? {
        val scores = scoreCalculator.calculateTimeAndTopologyAwareScores(
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