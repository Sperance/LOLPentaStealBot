group = "ru.descend"
version = "0.1a"
description = "Unofficial Bot for League of Legends"

plugins {
    kotlin("jvm") version "1.9.10"
    kotlin("plugin.serialization") version "1.9.10"
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("me.jakejmattson", "DiscordKt", "0.23.4")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.github.jr-selphius:LeagueOfLegendsAPI:1.0.0")

    implementation("junit:junit:4.12")
    implementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation(kotlin("test"))
}

tasks {
    compileKotlin {
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

tasks.test {
    useJUnitPlatform()
}