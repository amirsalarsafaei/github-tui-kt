package com.amirsalarsafaei.github_tui_kt.database

import java.io.File

object Data {


    const val appName = "github-tui-kt"

    val directory: File by lazy {

        val configDir = when {
            System.getProperty("os.name").lowercase().contains("win") -> File(
                System.getenv("APPDATA") ?: System.getProperty("user.home"), appName
            )

            System.getProperty("os.name").lowercase().contains("mac") -> File(
                System.getProperty("user.home"),
                "Library/Application Support/$appName"
            )

            else -> File(System.getProperty("user.home"), ".config/$appName")
        }

        if (!configDir.exists()) {
            configDir.mkdirs()
        }

        configDir
    }
}