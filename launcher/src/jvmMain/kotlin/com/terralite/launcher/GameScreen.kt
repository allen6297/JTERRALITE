package com.terralite.launcher

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.awt.Canvas
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A full-screen Compose composable that hosts the Vulkan game inside an AWT [Canvas].
 *
 * The [Canvas] is embedded via [SwingPanel] so that Compose manages layout while
 * Vulkan renders directly into the native window handle exposed by the canvas.
 *
 * The game loop runs on [Dispatchers.IO] and is signalled to stop when this
 * composable leaves the composition (e.g. the window is closed).
 *
 * @param packsRoot   path to the content pack directory
 * @param onGameExit  called (on any thread) when the game loop exits normally (e.g. QUIT selected)
 */
@Composable
fun GameScreen(
    packsRoot: Path,
    onGameExit: () -> Unit = {}
) {
    val shutdownSignal = remember { AtomicBoolean(false) }
    val scope = rememberCoroutineScope()
    var gameJob by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            shutdownSignal.set(true)
            gameJob?.cancel()
        }
    }

    SwingPanel(
        modifier = Modifier.fillMaxSize(),
        factory = {
            Canvas().apply {
                isFocusable = true
                // Start the game loop as soon as the canvas factory runs.
                // The game thread will block in AwtRenderWindow.create() until this
                // canvas obtains a native peer (which happens right after this factory
                // returns and the component is added to the Swing/AWT hierarchy).
                gameJob = scope.launch(Dispatchers.IO) {
                    try {
                        TerraliteGame.runOnCanvas(this@apply, packsRoot, shutdownSignal)
                    } finally {
                        // If the game loop exits on its own (QUIT selected), also
                        // signal the Compose window to close.
                        if (!shutdownSignal.get()) {
                            onGameExit()
                        }
                    }
                }
            }
        },
        update = { canvas ->
            // Request focus so AWT KeyListeners receive events
            canvas.requestFocusInWindow()
        }
    )
}
