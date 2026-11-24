plugins {
    java
}

group = "de.erethon"
version = "1.4.0"
description = "FactionsOne (Factions) for Paper 26.3"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io") {
        // JitPack publishes Gradle metadata whose version can differ from the
        // requested one; resolve the artifact directly for reproducible pins.
        metadataSources { artifact() }
    }
    maven("https://repo.essentialsx.net/releases/")
    maven("https://maven.enginehub.org/repo/")
}

val paperApiVersion = "26.3.build.259-stable"

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")

    // Optional integrations: the classes are supplied by the plugins on the server.
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
    compileOnly("net.essentialsx:EssentialsX:2.21.2") {
        isTransitive = false
    }
    // WorldGuard/WorldEdit pin the Mojang-provided libraries with `strictly`
    // constraints that clash with paper-api. The server supplies them at runtime.
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.17") {
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "com.google.code.gson", module = "gson")
        exclude(group = "it.unimi.dsi", module = "fastutil")
        exclude(group = "org.apache.logging.log4j", module = "log4j-api")
        exclude(group = "org.apache.logging.log4j", module = "log4j-bom")
        exclude(group = "com.google.errorprone", module = "error_prone_annotations")
    }
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.4.6") {
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "com.google.code.gson", module = "gson")
        exclude(group = "it.unimi.dsi", module = "fastutil")
        exclude(group = "org.apache.logging.log4j", module = "log4j-api")
        exclude(group = "org.apache.logging.log4j", module = "log4j-bom")
        exclude(group = "com.google.errorprone", module = "error_prone_annotations")
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
    options.compilerArgs.addAll(listOf("-Xlint:deprecation,removal,unchecked"))
}

tasks.processResources {
    val props = mapOf(
        "version" to project.version.toString(),
        "apiVersion" to "26.3",
    )
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.jar {
    archiveBaseName.set("factionsone")
    manifest {
        attributes(
            "Implementation-Title" to "FactionsOne",
            "Implementation-Version" to project.version.toString(),
        )
    }
}
