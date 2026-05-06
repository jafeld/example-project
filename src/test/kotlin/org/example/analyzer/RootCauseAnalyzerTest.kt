package org.example.analyzer

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.example.domain.EventType
import org.example.domain.Link
import org.example.domain.NetworkEvent
import org.example.domain.NetworkGraph
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

    "calculateScores with graph" {

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

            analyzer.calculateScores(
                events = events,
                graph = graph
            ) shouldBe mapOf(
                nodeA to 3,
                nodeB to 1,
                nodeC to 1
            )
        }

        "should combine direct score and neighbour score" {
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
                    type = EventType.DEGRADED,
                    timestamp = Instant.parse("2026-01-01T10:01:00Z")
                )
            )

            analyzer.calculateScores(
                events = events,
                graph = graph
            ) shouldBe mapOf(
                nodeA to 3,
                nodeB to 2
            )
        }

        "should return same direct scores when target has no neighbours" {
            val nodeA = Node(id = "A")

            val graph = NetworkGraph.fromLinks(links = emptyList())

            val events = listOf(
                NetworkEvent(
                    node = nodeA,
                    type = EventType.LINK_DOWN,
                    timestamp = Instant.parse("2026-01-01T10:00:00Z")
                )
            )

            analyzer.calculateScores(
                events = events,
                graph = graph
            ) shouldBe mapOf(
                nodeA to 3
            )
        }
    }
})