package org.example.analyzer

import org.example.domain.EventType
import org.example.domain.Link
import org.example.domain.NetworkEvent
import org.example.domain.NetworkGraph
import org.example.domain.Node
import org.example.domain.RootCause

class ScoreCalculator(
    private val weights: Map<EventType, Int>
) {

    fun calculateScores(
        events: List<NetworkEvent>,
        graph: NetworkGraph
    ): Map<RootCause, Double> {
        val scores = mutableMapOf<RootCause, Double>()

        if (events.isEmpty()) {
            return scores
        }

        // Use full event interval to value early events higher
        // Later events may be consequences of initial events
        val earliestTimestamp = events.minOf { it.timestamp.toEpochMilli() }
        val latestTimestamp = events.maxOf { it.timestamp.toEpochMilli() }

        // Failsafe to avoid dividing by 0 later in case all events have same timestamp
        val timeWindow = (latestTimestamp - earliestTimestamp).coerceAtLeast(minimumValue = 1)

        // Add scores based on direct events (time aware)
        addDirectEventScores(
            events = events,
            scores = scores,
            earliestTimestamp = earliestTimestamp,
            timeWindow = timeWindow
        )

        // Add scores based on topology (time aware)
        addTopologyHypotheses(
            events = events,
            graph = graph,
            scores = scores,
            earliestTimestamp = earliestTimestamp,
            timeWindow = timeWindow
        )

        return scores
    }

    private fun addDirectEventScores(
        events: List<NetworkEvent>,
        scores: MutableMap<RootCause, Double>,
        earliestTimestamp: Long,
        timeWindow: Long
    ) {
        for (event in events) {
            val weight = calculateTimeAwareWeight(
                event = event,
                earliestTimestamp = earliestTimestamp,
                timeWindow = timeWindow
            )

            val rootCause = rootCauseFor(event = event)

            addScore(
                scores = scores,
                rootCause = rootCause,
                score = weight
            )
        }
    }

    private fun addTopologyHypotheses(
        events: List<NetworkEvent>,
        graph: NetworkGraph,
        scores: MutableMap<RootCause, Double>,
        earliestTimestamp: Long,
        timeWindow: Long
    ) {
        // Collect reported LINK_DOWN issues per endpoint.
        // If several distinct links around the same node are affected,
        // that may indicate either a node failure or several local link failures.
        val linkIssuesByNode = mutableMapOf<Node, MutableList<Pair<Link, Double>>>()

        for (event in events) {
            val targetNode = event.target ?: continue

            val weight = calculateTimeAwareWeight(
                event = event,
                earliestTimestamp = earliestTimestamp,
                timeWindow = timeWindow
            )

            if (weight == 0.0) {
                continue
            }

            // If several events point to the same target node, the node itself is a possible cause.
            // Scoring weight of 0.5 is because this is only inferred evidence, not direct.
            // ToDo: Move weight out, possibly as a run config parameter
            addScore(
                scores = scores,
                rootCause = RootCause.NodeCause(node = targetNode),
                score = weight / 2.0
            )

            // For LINK_DOWN events, only add to linkIssues if target is a direct neighbour
            if (event.type == EventType.LINK_DOWN && graph.neighborsOf(node = event.node).contains(targetNode)) {
                val link = linkBetween(
                    first = event.node,
                    second = targetNode
                )

                // Register the same link for both endpoints so we can detect clusters of affected links around either node.
                linkIssuesByNode
                    .getOrPut(key = event.node) { mutableListOf() }
                    .add(link to weight)

                linkIssuesByNode
                    .getOrPut(key = targetNode) { mutableListOf() }
                    .add(link to weight)
            }
        }

        for ((node, linkIssues) in linkIssuesByNode) {
            // Multiple events may report same physical link
            // Group by link so we only use distinct links
            val distinctLinksWithScores = linkIssues
                .groupBy(keySelector = { it.first }, valueTransform = { it.second })
                .mapValues { (_, scores) -> scores.sum() }

            // A group cause only make sense when multiple distinct links have issues to the same node
            // ToDo: Move value to a global/run config parameter
            if (distinctLinksWithScores.size < 2) {
                continue
            }

            // Add weight to the group of links
            // Reduced grouped score because this is an inferred hypothesis
            // ToDo: Move value to a global/run config parameter (and lower value?)
            val links = distinctLinksWithScores.keys.toList()
            val groupScore = distinctLinksWithScores.values.sum() * 0.75

            addScore(
                scores = scores,
                rootCause = RootCause.LinkGroupCause(
                    node = node,
                    links = links
                ),
                score = groupScore
            )
        }
    }

    private fun rootCauseFor(event: NetworkEvent): RootCause {
        return if (event.type == EventType.LINK_DOWN && event.target != null) {
            RootCause.LinkCause(
                link = linkBetween(
                    first = event.node,
                    second = event.target
                )
            )
        } else {
            RootCause.NodeCause(
                node = event.target ?: event.node
            )
        }
    }

    private fun calculateTimeAwareWeight(
        event: NetworkEvent,
        earliestTimestamp: Long,
        timeWindow: Long
    ): Double {
        val baseWeight = (weights[event.type] ?: 0).toDouble()

        if (baseWeight == 0.0) {
            return 0.0
        }

        val eventOffset = event.timestamp.toEpochMilli() - earliestTimestamp
        val eventPosition = eventOffset.toDouble() / timeWindow.toDouble()

        // Earlier events keep more weight. Later events are reduced, but still count.
        // If first event, time factor is 1.0
        // If middle event, time factor is 0.75
        // If last event, time factor is 0.5
        val timeFactor = 1.0 - (eventPosition * 0.5)

        return baseWeight * timeFactor
    }

    private fun addScore(
        scores: MutableMap<RootCause, Double>,
        rootCause: RootCause,
        score: Double
    ) {
        scores[rootCause] = scores.getOrDefault(
            key = rootCause,
            defaultValue = 0.0
        ) + score
    }

    // Links are treated as undirected by storing them in the same order
    private fun linkBetween(
        first: Node,
        second: Node
    ): Link {
        return if (first.id <= second.id) {
            Link(first = first, second = second)
        } else {
            Link(first = second, second = first)
        }
    }
}