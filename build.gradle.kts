plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.shadow) // Add Shadow plugin for proper fat JAR
    application
}

application {
    mainClass.set("com.amirsalarsafaei.github_tui_kt.MainKt")
}

version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
}

dependencies {
    // Core dependencies
    implementation(libs.datetime)
    implementation(libs.mordant)
    implementation(libs.mordant.coroutines)
    implementation(libs.mordant.markdown)
    implementation(libs.sqldelight.coroutines)
    implementation(libs.sqldelight.adapters)
    implementation(libs.gson)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converters.gson)
    implementation(libs.scrimage)
    implementation(libs.logback)
    implementation(libs.okhttp)
    implementation(libs.logging)

    // JVM-specific dependencies
    implementation(libs.sqldelight.driver.jvm)
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
    }
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("com.amirsalarsafaei.github_tui_kt.database")
        }
    }
}