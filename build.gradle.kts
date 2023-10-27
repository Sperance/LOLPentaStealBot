group = "ru.descend"
version = "0.1a"
description = "Unofficial Bot for League of Legends"

plugins {
    kotlin("jvm") version "1.9.10"
    kotlin("plugin.serialization") version "1.9.10"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("me.jakejmattson", "DiscordKt", "0.23.4")
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks {
    compileKotlin {
        kotlinOptions {
            freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
        }
        dependsOn("writeProperties")
    }

    register<WriteProperties>("writeProperties") {
        property("name", "LOLPentaSteal")
        property("description", project.description.toString())
        property("version", project.version.toString())
        property("url", "https://github.com/DiscordKt/ExampleBot")
        setOutputFile("src/main/resources/bot.properties")
    }
}