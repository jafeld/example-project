package org.example.domain

sealed interface RootCause {

    data class NodeCause(
        val node: Node
    ) : RootCause {
        override fun toString(): String =
            "Node ${node.id}"
    }

    data class LinkCause(
        val link: Link
    ) : RootCause {
        override fun toString(): String =
            "Link ${link.first.id}-${link.second.id}"
    }

    data class LinkGroupCause(
        val node: Node,
        val links: List<Link>
    ) : RootCause {
        override fun toString(): String =
            "Links around ${node.id}"
    }
}