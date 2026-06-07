package com.terralite.launcher

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        val databaseFile = File("launcher.db")
        val driver: SqlDriver = JdbcSqliteDriver(url = "jdbc:sqlite:${databaseFile.absolutePath}")
        if (!databaseFile.exists()) {
            LauncherDatabase.Schema.create(driver)
        }
        return driver
    }
}
