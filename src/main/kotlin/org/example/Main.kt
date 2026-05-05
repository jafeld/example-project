package org.example

import org.example.database.Database
import org.example.database.TopologyLoader
import org.example.database.createTables
import org.example.database.seedDemoData

fun main() {
    Database.startServer()

    Database.connect().use { connection ->
        createTables(connection)
        seedDemoData(connection)

        val topologyLoader = TopologyLoader(connection)
        val nodes = topologyLoader.loadNodes()
        val links = topologyLoader.loadLinks(nodes)
    }
}