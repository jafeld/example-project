package org.example.analyzer

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.example.domain.EventType
import org.example.domain.Link
import org.example.domain.NetworkEvent
import org.example.domain.NetworkGraph
import org.example.domain.Node
import org.example.domain.RootCause
import java.time.Instant

class ScoreCalculatorTest : FreeSpec({

    val scoreCalculator = ScoreCalculator(
        config = ScoringConfig()
    )

    "calculateScores" - {
        "no events should return empty map" {
            val graph = NetworkGraph.fromLinks(links = emptyList())

            scoreCalculator.calculateScores(
                events = emptyList(),
                graph = graph
            ) shouldBe emptyMap()
        }

        "event without target should assign score to node cause" {
            val nodeC = Node(id = "C")
            val graph = NetworkGraph.fromLinks(links = emptyList())

            val events = listOf(
                NetworkEvent(
                    node = nodeC,
                    type = EventType.DEGRADED,
                    timestamp = Instant.parse("2026-01-01T10:00:00Z")
                )
            )

            scoreCalculator.calculateScores(
                events = events,
                graph = graph
            ) shouldBe mapOf(
                RootCause.NodeCause(node = nodeC) to 1.0
            )
        }

        "link down event with target should score both link and target node hypothesis" {
            val nodeA = Node(id = "A")
            val nodeB = Node(id = "B")

            val graph = NetworkGraph.fromLinks(
                links = listOf(
                    Link(first = nodeA, second = nodeB)
                )
            )

            val events = listOf(
                NetworkEvent(
                    node = nodeA,
                    target = nodeB,
                    type = EventType.LINK_DOWN,
                    timestamp = Instant.parse("2026-01-01T10:00:00Z")
                )
            )

            scoreCalculator.calculateScores(
                events = events,
                graph = graph
            ) shouldBe mapOf(
                RootCause.LinkCause(
                    link = Link(first = nodeA, second = nodeB)
                ) to 3.0,
                RootCause.NodeCause(node = nodeB) to 1.5
            )
        }

        "later events should receive reduced score" {
            val nodeA = Node(id = "A")
            val nodeB = Node(id = "B")

            val graph = NetworkGraph.fromLinks(
                links = listOf(
                    Link(first = nodeA, second = nodeB)
                )
            )

            val events = listOf(
                NetworkEvent(
                    node = nodeA,
                    target = nodeB,
                    type = EventType.LINK_DOWN,
                    timestamp = Instant.parse("2026-01-01T10:00:00Z")
                ),
                NetworkEvent(
                    node = nodeB,
                    type = EventType.NODE_UNREACHABLE,
                    timestamp = Instant.parse("2026-01-01T10:00:02Z")
                )
            )

            scoreCalculator.calculateScores(
                events = events,
                graph = graph
            ) shouldBe mapOf(
                RootCause.LinkCause(
                    link = Link(first = nodeA, second = nodeB)
                ) to 3.0,
                RootCause.NodeCause(node = nodeB) to 2.5
            )
        }

        "multiple link issues around same node should create link group cause" {
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
                ),
                NetworkEvent(
                    node = nodeC,
                    target = nodeA,
                    type = EventType.LINK_DOWN,
                    timestamp = Instant.parse("2026-01-01T10:00:00Z")
                )
            )

            scoreCalculator.calculateScores(
                events = events,
                graph = graph
            ) shouldBe mapOf(
                RootCause.LinkCause(
                    link = Link(first = nodeA, second = nodeB)
                ) to 3.0,
                RootCause.LinkCause(
                    link = Link(first = nodeA, second = nodeC)
                ) to 3.0,
                RootCause.NodeCause(node = nodeA) to 3.0,
                RootCause.LinkGroupCause(
                    node = nodeA,
                    links = listOf(
                        Link(first = nodeA, second = nodeB),
                        Link(first = nodeA, second = nodeC)
                    )
                ) to 4.5
            )
        }
    }
})