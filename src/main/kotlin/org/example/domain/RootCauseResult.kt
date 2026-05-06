package org.example.domain

data class RootCauseResult(
    val rootCause: RootCause,
    val score: Double,
    val confidence: Double,
    val reason: String
)
