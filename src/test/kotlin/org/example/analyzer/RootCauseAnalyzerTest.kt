package org.example.analyzer

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.example.domain.EventType
import org.example.domain.Link
import org.example.domain.NetworkEvent
import org.example.domain.NetworkGraph
import org.example.domain.Node
import java.time.Instant

class RootCauseAnalyzerTest : FreeSpec({

    val analyzer = RootCauseAnalyzer()

    "findRootCause" - {
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

    "findRootCauseResults" - {
        "should return ranked time and topology aware results with confidence" {
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
                )
            )

            val results = analyzer.findRootCauseResults(
                events = events,
                graph = graph
            )

            results.size shouldBe 2

            results[0].node shouldBe nodeA
            results[0].score shouldBe 3.0
            results[0].confidence shouldBe 0.6666666666666666

            results[1].node shouldBe nodeB
            results[1].score shouldBe 1.5
            results[1].confidence shouldBe 0.3333333333333333

            results[0].reason shouldBe "Score calculated using time and topology aware scoring"
        }
    }
})