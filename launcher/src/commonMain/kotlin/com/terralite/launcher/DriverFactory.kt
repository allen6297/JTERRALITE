package com.terralite.launcher

import app.cash.sqldelight.db.SqlDriver

expect class DriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DriverFactory): LauncherDatabase {
    val driver = driverFactory.createDriver()
    return LauncherDatabase(driver)
}
