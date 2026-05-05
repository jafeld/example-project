package org.example.domain

data class Link(
    val first: Node,
    val second: Node
) {
    init {
        require(first != second) {
            "Nodes can't be directly linked to themselves"
        }
    }
}