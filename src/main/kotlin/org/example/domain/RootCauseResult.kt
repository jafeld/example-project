package org.example.domain

data class RootCauseResult(
    val node: Node,
    val score: Double,
    val confidence: Double,
    val reason: String
)