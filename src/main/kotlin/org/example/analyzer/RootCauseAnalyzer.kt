package org.example.analyzer

import org.example.domain.EventType
import org.example.domain.NetworkEvent
import org.example.domain.NetworkGraph
import org.example.domain.RootCause
import org.example.domain.RootCauseResult

class RootCauseAnalyzer(
    private val config: ScoringConfig = ScoringConfig()
) {
    private val scoreCalculator = ScoreCalculator(config = config)

    fun findRootCause(
        events: List<NetworkEvent>,
        graph: NetworkGraph
    ): RootCause? {
        val scores = scoreCalculator.calculateScores(
            events = events,
            graph = graph
        )

        return scores.maxByOrNull { it.value }?.key
    }

    fun findRootCauseResults(
        events: List<NetworkEvent>,
        graph: NetworkGraph,
        maxCandidates: Int = config.maxCandidates
    ): List<RootCauseResult> {
        val scores = scoreCalculator.calculateScores(
            events = events,
            graph = graph
        )

        return createRootCauseResults(
            scores = scores,
            maxCandidates = maxCandidates
        )
    }

    private fun createRootCauseResults(
        scores: Map<RootCause, Double>,
        maxCandidates: Int = 3
    ): List<RootCauseResult> {
        val totalScore = scores.values.sum()

        if (totalScore == 0.0) {
            return emptyList()
        }

        return scores.entries
            .sortedByDescending { it.value }
            .take(maxCandidates)
            .map { (rootCause, score) ->
                RootCauseResult(
                    rootCause = rootCause,
                    score = score,
                    confidence = score / totalScore,
                    reason = createReason(
                        rootCause = rootCause,
                        score = score,
                    )
                )
            }
    }

    private fun createReason(
        rootCause: RootCause,
        score: Double,
    ): String =
        when (rootCause) {
            is RootCause.NodeCause ->
                "Node ${rootCause.node.id} has accumulated a score of ${"%.2f".format(score)} from reported symptoms and neighbouring topology impact."

            is RootCause.LinkCause ->
                "Link ${rootCause.link.first.id}-${rootCause.link.second.id} was directly reported as down."

            is RootCause.LinkGroupCause ->
                "Several links connected to node ${rootCause.node.id} show issues."
        }
}