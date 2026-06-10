package com.terralite.launcher

import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.terralite.content.loading.ContentPackDiscovery
import com.terralite.core.logging.Loggers
import com.terralite.launcher.net.NetworkServer
import java.nio.file.Paths

private val log = Loggers.get("com.terralite.launcher.Main")

fun main() {
    val packsRoot = findPacksRoot()
    val packs = runCatching { ContentPackDiscovery().discover(packsRoot) }.getOrElse { emptyList() }
    val displayPacks = packs.map { pack ->
        PackDisplayInfo(
            name = pack.manifest().name(),
            version = pack.manifest().version(),
            description = pack.manifest().description()
        )
    }

    val database = createDatabase(DriverFactory())
    val contentRepository = SqlContentRepository(database)

    var launchMode: String? = null
    var joinAddress: String? = null  // null = local single-player

    application(exitProcessOnExit = false) {
        Window(
            onCloseRequest = ::exitApplication,
            title = "TERRALITE Launcher",
            state = WindowState(width = 1280.dp, height = 720.dp),
            resizable = true
        ) {
            LauncherScreen(
                packsPath = packsRoot.toString(),
                packs = displayPacks,
                contentRepository = contentRepository,
                onLaunchClient = { serverAddress ->
                    joinAddress = serverAddress?.takeIf { it.isNotBlank() }
                    launchMode = "client"
                    exitApplication()
                },
                onLaunchServer = {
                    launchMode = "server"
                    exitApplication()
                }
            )
        }
    }

    when (launchMode) {
        "client" -> {
            val address = joinAddress
            if (address != null) {
                log.info("Joining server at {}...", address)
                val (host, portStr) = parseAddress(address)
                RemoteClientApp(packsRoot, host, portStr).run()
            } else {
                log.info("Launching single-player client in Compose window...")
                application(exitProcessOnExit = false) {
                    var isOpen by remember { mutableStateOf(true) }
                    if (isOpen) {
                        Window(
                            onCloseRequest = { isOpen = false },
                            title = "TERRALITE",
                            state = WindowState(width = 1280.dp, height = 720.dp),
                            resizable = true,
                            undecorated = false
                        ) {
                            GameScreen(
                                packsRoot = packsRoot,
                                onGameExit = { isOpen = false }
                            )
                        }
                    }
                }
            }
        }
        "server" -> {
            log.info("Launching dedicated server...")
            ServerApp(packsRoot).run()
        }
    }
}

private fun findPacksRoot(): java.nio.file.Path {
    var dir: java.nio.file.Path? = Paths.get("").toAbsolutePath()
    while (dir != null) {
        val candidate = dir.resolve("packs")
        if (java.nio.file.Files.isDirectory(candidate)) return candidate.normalize()
        dir = dir.parent
    }
    return Paths.get("packs").toAbsolutePath().normalize()
}

private fun parseAddress(address: String): Pair<String, Int> {
    val colon = address.lastIndexOf(':')
    return if (colon > 0) {
        val port = address.substring(colon + 1).toIntOrNull() ?: NetworkServer.DEFAULT_PORT
        address.substring(0, colon) to port
    } else {
        // bare number → treat as port on localhost
        val port = address.trim().toIntOrNull()
        if (port != null) "localhost" to port
        else address to NetworkServer.DEFAULT_PORT
    }
}
