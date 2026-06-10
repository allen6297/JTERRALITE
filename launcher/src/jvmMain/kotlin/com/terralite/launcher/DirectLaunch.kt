package com.terralite.launcher

import java.nio.file.Paths

fun main(args: Array<String>) {
    val mode = args.getOrNull(0) ?: "client"
    val packsRoot = Paths.get(args.getOrElse(1) { "packs" })

    when (mode) {
        "server" -> ServerApp(packsRoot).run()
        else -> TerraliteGame.main(arrayOf(packsRoot.toString()))
    }
}
