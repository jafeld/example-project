package org.example.database

import org.h2.tools.Server
import java.sql.Connection
import java.sql.DriverManager

object Database {
    private const val databaseFile = "./data/rootcause"
    private const val username = "sa"
    private const val password = ""
    private const val port = "9092"

    private var tcpServer: Server? = null

    fun startServer() {
        if (tcpServer != null) return

        tcpServer = Server.createTcpServer(
            "-tcp",
            "-tcpPort", port,
            "-ifNotExists"
        ).start()

        println("H2 TCP server started on port $port")
    }

    fun connect(): Connection =
        DriverManager.getConnection(
            "jdbc:h2:$databaseFile;AUTO_SERVER=TRUE",
            username,
            password
        )

    fun stopServer() {
        tcpServer?.stop()
        tcpServer = null
    }
}