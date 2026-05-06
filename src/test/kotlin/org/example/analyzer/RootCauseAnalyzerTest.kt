package org.example.analyzer

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.example.domain.EventType
import org.example.domain.NetworkEvent
import org.example.domain.Node
import java.time.Instant

class RootCauseAnalyzerTest : StringSpec({

    val analyzer = RootCauseAnalyzer()

    // ToDo: Move events out of tests and reuse for both functions?
    "findRootCause" {
        "no events should return null" {
            analyzer.findRootCause(events = emptyList()) shouldBe null
        }

        "single event without target should return the node itself" {
            val nodeC = Node(id = "C")

            val events = listOf(
                NetworkEvent(
                    node = nodeC,
                    type = EventType.DEGRADED,
                    timestamp = Instant.parse("2026-01-01T10:00:00Z")
                )
            )

            analyzer.findRootCause(events = events) shouldBe nodeC
        }

        "event with target should return target node" {
            val nodeA = Node(id = "A")
            val nodeB = Node(id = "B")

            val events = listOf(
                NetworkEvent(
                    node = nodeA,
                    target = nodeB,
                    type = EventType.LINK_DOWN,
                    timestamp = Instant.parse("2026-01-01T10:00:00Z")
                )
            )

            analyzer.findRootCause(events = events) shouldBe nodeB
        }

        "node with highest total score should be returned" {
            val nodeA = Node(id = "A")
            val nodeC = Node(id = "C")

            val events = listOf(
                NetworkEvent(
                    node = nodeA,
                    type = EventType.DEGRADED,
                    timestamp = Instant.parse("2026-01-01T10:00:00Z")
                ),
                NetworkEvent(
                    node = nodeC,
                    type = EventType.LINK_DOWN,
                    timestamp = Instant.parse("2026-01-01T10:01:00Z")
                )
            )

            analyzer.findRootCause(events = events) shouldBe nodeC
        }
    }

    "calculateScores" {
        "no events should return empty map" {
            analyzer.calculateScores(events = emptyList()) shouldBe emptyMap()
        }

        "event without target should assign score to node itself" {
            val nodeC = Node(id = "C")

            val events = listOf(
                NetworkEvent(
                    node = nodeC,
                    type = EventType.LINK_DOWN,
                    timestamp = Instant.parse("2026-01-01T10:00:00Z")
                )
            )

            analyzer.calculateScores(events = events) shouldBe mapOf(
                nodeC to 3
            )
        }

        "event with target should assign score to target" {
            val nodeA = Node(id = "A")
            val nodeB = Node(id = "B")

            val events = listOf(
                NetworkEvent(
                    node = nodeA,
                    target = nodeB,
                    type = EventType.LINK_DOWN,
                    timestamp = Instant.parse("2026-01-01T10:00:00Z")
                )
            )

            analyzer.calculateScores(events = events) shouldBe mapOf(
                nodeB to 3
            )
        }

        "multiple events should accumulate score on same node" {
            val nodeC = Node(id = "C")

            val events = listOf(
                NetworkEvent(
                    node = nodeC,
                    type = EventType.LINK_DOWN, // 3
                    timestamp = Instant.parse("2026-01-01T10:00:00Z")
                ),
                NetworkEvent(
                    node = nodeC,
                    type = EventType.NODE_UNREACHABLE, // 2
                    timestamp = Instant.parse("2026-01-01T10:01:00Z")
                ),
                NetworkEvent(
                    node = nodeC,
                    type = EventType.DEGRADED, // 1
                    timestamp = Instant.parse("2026-01-01T10:02:00Z")
                )
            )

            analyzer.calculateScores(events = events) shouldBe mapOf(
                nodeC to 6
            )
        }

        "multiple nodes reporting to same target should accumulate score on target" {
            val nodeA = Node(id = "A")
            val nodeB = Node(id = "B")
            val nodeC = Node(id = "C")

            val events = listOf(
                NetworkEvent(
                    node = nodeA,
                    target = nodeC,
                    type = EventType.LINK_DOWN,
                    timestamp = Instant.parse("2026-01-01T10:00:00Z")
                ),
                NetworkEvent(
                    node = nodeB,
                    target = nodeC,
                    type = EventType.NODE_UNREACHABLE,
                    timestamp = Instant.parse("2026-01-01T10:01:00Z")
                )
            )

            analyzer.calculateScores(events = events) shouldBe mapOf(
                nodeC to 5
            )
        }

        "event type without weight should give zero score" {
            val nodeC = Node(id = "C")

            val events = listOf(
                NetworkEvent(
                    node = nodeC,
                    type = EventType.NODE_DOWN,
                    timestamp = Instant.parse("2026-01-01T10:00:00Z")
                )
            )

            analyzer.calculateScores(events = events) shouldBe mapOf(
                nodeC to 0
            )
        }
    }
})