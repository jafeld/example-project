package org.example.analyzer

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.example.domain.EventType
import org.example.domain.NetworkEvent
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
})