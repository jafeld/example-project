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

class RootCauseAnalyzerTest : FreeSpec({

    val analyzer = RootCauseAnalyzer()

    "findRootCause" - {
        "no events should return null" {
            val graph = NetworkGraph.fromLinks(links = emptyList())

            analyzer.findRootCause(
                events = emptyList(),
                graph = graph
            ) shouldBe null
        }

        "single event without target should return the node itself as root cause" {
            val nodeC = Node(id = "C")
            val graph = NetworkGraph.fromLinks(links = emptyList())

            val events = listOf(
                NetworkEvent(
                    node = nodeC,
                    type = EventType.DEGRADED,
                    timestamp = Instant.parse("2026-01-01T10:00:00Z")
                )
            )

            analyzer.findRootCause(
                events = events,
                graph = graph
            ) shouldBe RootCause.NodeCause(node = nodeC)
        }

        "link down event with target should return the link as root cause" {
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

            analyzer.findRootCause(
                events = events,
                graph = graph
            ) shouldBe RootCause.LinkCause(
                link = Link(first = nodeA, second = nodeB)
            )
        }
    }

    "findRootCauseResults" - {
        "should return ranked root cause results with confidence" {
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

            results[0].rootCause shouldBe RootCause.LinkCause(
                link = Link(first = nodeA, second = nodeB)
            )
            results[0].score shouldBe 3.0
            results[0].confidence shouldBe 0.6666666666666666

            results[1].rootCause shouldBe RootCause.NodeCause(node = nodeA)
            results[1].score shouldBe 1.5
            results[1].confidence shouldBe 0.3333333333333333
        }
    }
})