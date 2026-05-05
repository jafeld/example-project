package org.example.domain

import java.time.Instant

enum class EventType {
    LINK_DOWN,
    LINK_UP,
    NODE_DOWN,
    NODE_UNREACHABLE,
    LINK_DEGRADED,
    DEGRADED
}

data class NetworkEvent(
    val node: Node,
    val type: EventType,
    val timestamp: Instant,
    val target: Node? = null
)