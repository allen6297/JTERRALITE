package com.terralite.launcher

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.terralite.content.loading.ContentPackDiscovery
import com.terralite.core.logging.Loggers
import java.nio.file.Paths

private val log = Loggers.get("com.terralite.launcher.Main")

fun main() {
    val packsRoot = Paths.get("packs").toAbsolutePath().normalize()
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

    application {
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
                onLaunchClient = { launchMode = "client"; exitApplication() },
                onLaunchServer = { launchMode = "server"; exitApplication() }
            )
        }
    }

    when (launchMode) {
        "client" -> {
            log.info("Launching client...")
            ClientApp(packsRoot).run()
        }
        "server" -> {
            log.info("Launching server...")
            ServerApp(packsRoot).run()
        }
    }
}
