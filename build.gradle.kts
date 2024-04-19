import java.util.Properties

group = "ru.descend"
version = "1.6.0"
description = "Unofficial Bot for League of Legends"

plugins {
    application
    id("java")
    id("com.google.devtools.ksp") version "1.9.23-1.0.19"

    kotlin("jvm") version "1.9.23"
    kotlin("plugin.serialization") version "1.9.23"
}

repositories {
    mavenCentral()
    mavenLocal()
    google()
    maven("https://jitpack.io")
}

val komapperVersion = "1.17.0"

dependencies {
    platform("org.komapper:komapper-platform:$komapperVersion").let {
        implementation(it)
        ksp(it)
    }
    implementation("org.komapper:komapper-tx-core:1.12.1")
    implementation("org.komapper:komapper-template:$komapperVersion")
    implementation("org.komapper:komapper-starter-r2dbc:$komapperVersion")
    implementation("org.komapper:komapper-dialect-postgresql-r2dbc")
    ksp("org.komapper:komapper-processor")

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.retrofit2:retrofit:2.10.0")
    implementation("com.squareup.retrofit2:converter-gson:2.10.0")

    implementation("me.jakejmattson", "DiscordKt", "0.24.0")

    implementation("junit:junit:4.13.2")
    implementation("org.junit.jupiter:junit-jupiter:5.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation(kotlin("test"))
    implementation(kotlin("reflect"))

    implementation("dev.shreyaspatil.generativeai:generativeai-google:0.2.2-1.0.0")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "17"

        Properties().apply {
            setProperty("name", project.name)
            setProperty("description", project.description)
            setProperty("version", version.toString())
            setProperty("url", "https://github.com/Sperance/LOLPentaStealBot")

            store(file("src/main/resources/bot.properties").outputStream(), null)
        }
    }
}

tasks.withType<Tar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<Zip> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<Jar> {
    manifest {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        attributes["Main-Class"] = "ru.descend.bot.MainAppKt"
        manifest.attributes["Class-Path"] = configurations
            .runtimeClasspath
            .get()
            .joinToString(separator = " ") { file ->
                "libs/${file.name}"
            }
    }
    archiveFileName.set("${project.name}_${version}.jar")

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

tasks {
    withType<Test>().configureEach {
        useJUnitPlatform()
    }
}