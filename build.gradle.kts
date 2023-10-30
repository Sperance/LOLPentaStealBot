group = "ru.descend"
version = "0.2"
description = "Unofficial Bot for League of Legends"

plugins {
    application
    kotlin("jvm") version "1.9.10"
    kotlin("plugin.serialization") version "1.9.10"
    id("java")
}

application {
    mainClass.set("ru.descend.bot.MainApp.kt")
}

repositories {
    mavenCentral()
    google()
    maven("https://jitpack.io")
}

dependencies {
    implementation("me.jakejmattson", "DiscordKt", "0.23.4")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.github.jr-selphius:LeagueOfLegendsAPI:1.0.0")

    implementation("junit:junit:4.13.2")
    implementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation(kotlin("test"))
}

tasks.withType<Tar> { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }
tasks.withType<Zip> { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }
tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "ru.descend.bot.MainAppKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // To add all of the dependencies
    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

tasks.test {
    useJUnitPlatform()
}