package org.example.analyzer

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.example.domain.EventType
import org.example.domain.Link
import org.example.domain.NetworkEvent
import org.example.domain.NetworkGraph
import org.example.domain.Node
import java.time.Instant

class ScoreCalculatorTest : FreeSpec({

    val scoreCalculator = ScoreCalculator(
        weights = RootCauseAnalyzer.defaultWeights()
    )

    "calculateSimpleScores" - {
        "no events should return empty map" {
            scoreCalculator.calculateSimpleScores(events = emptyList()) shouldBe emptyMap()
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

            scoreCalculator.calculateSimpleScores(events = events) shouldBe mapOf(
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

            scoreCalculator.calculateSimpleScores(events = events) shouldBe mapOf(
                nodeB to 3
            )
        }

        "multiple events should accumulate score on same node" {
            val nodeC = Node(id = "C")

            val events = listOf(
                NetworkEvent(
                    node = nodeC,
                    type = EventType.LINK_DOWN,
                    timestamp = Instant.parse("2026-01-01T10:00:00Z")
                ),
                NetworkEvent(
                    node = nodeC,
                    type = EventType.NODE_UNREACHABLE,
                    timestamp = Instant.parse("2026-01-01T10:01:00Z")
                ),
                NetworkEvent(
                    node = nodeC,
                    type = EventType.DEGRADED,
                    timestamp = Instant.parse("2026-01-01T10:02:00Z")
                )
            )

            scoreCalculator.calculateSimpleScores(events = events) shouldBe mapOf(
                nodeC to 6
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

            scoreCalculator.calculateSimpleScores(events = events) shouldBe mapOf(
                nodeC to 0
            )
        }
    }

    "calculateTopologyAwareScores" - {
        "should add reduced score to neighbours of the target node" {
            val nodeA = Node(id = "A")
            val nodeB = Node(id = "B")
            val nodeC = Node(id = "C")

            val graph = NetworkGraph.fromLinks(
                links = listOf(
                    Link(first = nodeA, second = nodeB),
                    Link(first = nodeA, second = nodeC)
                )
            )

            val events = listOf(
                NetworkEvent(
                    node = nodeB,
                    target = nodeA,
                    type = EventType.LINK_DOWN,
                    timestamp = Instant.parse("2026-01-01T10:00:00Z")
                )
            )

            scoreCalculator.calculateTopologyAwareScores(
                events = events,
                graph = graph
            ) shouldBe mapOf(
                nodeA to 3,
                nodeB to 1,
                nodeC to 1
            )
        }
    }

    "calculateTimeAwareScores" - {
        "should reduce score for later events" {
            val nodeC = Node(id = "C")

            val events = listOf(
                NetworkEvent(
                    node = nodeC,
                    type = EventType.LINK_DOWN,
                    timestamp = Instant.parse("2026-01-01T10:00:00Z")
                ),
                NetworkEvent(
                    node = nodeC,
                    type = EventType.NODE_UNREACHABLE,
                    timestamp = Instant.parse("2026-01-01T10:00:02Z")
                )
            )

            scoreCalculator.calculateTimeAwareScores(events = events) shouldBe mapOf(
                nodeC to 4
            )
        }
    }

    "calculateTimeAndTopologyAwareScores" - {
        "should combine time weighting and neighbour scoring" {
            val nodeA = Node(id = "A")
            val nodeB = Node(id = "B")

            val graph = NetworkGraph.fromLinks(
                links = listOf(
                    Link(first = nodeA, second = nodeB)
                )
            )

            val events = listOf(
                NetworkEvent(
                    node = nodeB,
                    target = nodeA,
                    type = EventType.LINK_DOWN,
                    timestamp = Instant.parse("2026-01-01T10:00:00Z")
                ),
                NetworkEvent(
                    node = nodeB,
                    type = EventType.NODE_UNREACHABLE,
                    timestamp = Instant.parse("2026-01-01T10:00:02Z")
                )
            )

            scoreCalculator.calculateTimeAndTopologyAwareScores(
                events = events,
                graph = graph
            ) shouldBe mapOf(
                nodeA to 3,
                nodeB to 2
            )
        }
    }
})