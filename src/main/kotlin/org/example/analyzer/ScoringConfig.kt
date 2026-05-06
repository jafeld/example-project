package org.example.analyzer

import org.example.domain.EventType

data class ScoringConfig(
    // Base score per event type
    val eventWeights: Map<EventType, Int> = defaultEventWeights(),

    // Weight multiplier used when inferring that a target node itself
    // may be the root cause of surrounding symptoms.
    //
    // Example: If B reports LINK_DOWN towards A, A receives partial inferred score.
    val inferredNodeWeightFactor: Double = 0.5,

    // Weight multiplier applied to grouped link hypotheses.
    //
    // Example: Multiple failing links around the same node may indicate:
    // - node failure
    // - local topology issue
    val linkGroupWeightFactor: Double = 0.75,

    // Number of links required for group cause hypothesis
    val minimumLinksForGroupCause: Int = 2,

    // Max potential root causes to show in output
    val maxCandidates: Int = 3
) {
    companion object {
        fun defaultEventWeights(): Map<EventType, Int> =
            mapOf(
                EventType.LINK_DOWN to 3,
                EventType.NODE_UNREACHABLE to 2,
                EventType.DEGRADED to 1
            )
    }
}