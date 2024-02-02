group = "ru.descend"
version = "1.2.1"
description = "Unofficial Bot for League of Legends"

plugins {
    application
    id("java")

    kotlin("jvm") version "1.9.10"
    kotlin("plugin.serialization") version "1.9.10"
}

//application {
//    mainClass.set("ru.descend.bot.MainApp.kt")
//
//}

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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.2")
    testImplementation(kotlin("test"))

    implementation("com.github.SergeyHSE7:Kotlin-ORM:01c23e02a5ede73647c5f4fc1cdefb8014b700c1")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
        dependsOn("writeProperties")
    }

    register<WriteProperties>("writeProperties") {
        property("name", project.name)
        property("description", project.description.toString())
        property("version", version.toString())
        property("url", "https://github.com/Sperance/LOLPentaStealBot")
        setOutputFile("src/main/resources/bot.properties")
    }
}

tasks.withType<Tar> { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }
tasks.withType<Zip> { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }
tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "ru.descend.bot.MainAppKt"
        manifest.attributes["Class-Path"] = configurations
            .runtimeClasspath
            .get()
            .joinToString(separator = " ") { file ->
                "libs/${file.name}"
            }
    }
    archiveFileName.set("${project.name}_${version}.jar")

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

tasks.test {
    useJUnitPlatform()
}