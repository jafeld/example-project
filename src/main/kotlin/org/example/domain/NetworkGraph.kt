package org.example.domain

data class NetworkGraph(
    private val adjacency: Map<Node, Set<Node>>
) {
    fun neighborsOf(node: Node): Set<Node> =
        adjacency[node] ?: emptySet()

    // Build graph from list of undirected links (neighbours)
    companion object {
        fun fromLinks(links: Collection<Link>): NetworkGraph {
            val adjacency = mutableMapOf<Node, MutableSet<Node>>()

            for (link in links) {
                adjacency.getOrPut(key = link.first) { mutableSetOf() }.add(link.second)
                adjacency.getOrPut(key = link.second) { mutableSetOf() }.add(link.first)
            }

            return NetworkGraph(adjacency = adjacency)
        }
    }
}