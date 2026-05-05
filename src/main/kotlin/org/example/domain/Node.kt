package org.example.domain

data class Node(
    val id: String
) {
    init {
        require(id.isNotBlank()) {
            "Node id cannot be blank"
        }
    }

    override fun toString(): String = id
}