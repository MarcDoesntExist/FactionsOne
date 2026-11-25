import java.io.File
import java.util.concurrent.TimeUnit

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

// Compiles the local Vault stand-in used by the live bank test. Not part of `build`.
val probeCompile: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    probeCompile("io.papermc.paper:paper-api:$paperApiVersion")
    probeCompile("com.github.MilkBowl:VaultAPI:1.7.1") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
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

    testImplementation("io.papermc.paper:paper-api:$paperApiVersion")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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

tasks.test {
    useJUnitPlatform()
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

// ---------------------------------------------------------------------------
// Real-server smoke test
//
// Boots the built jar on a real Paper server, dispatches every command root
// from the console and fails on ERROR/SEVERE, stack traces or unknown commands.
// This is the check that a green unit build cannot give: the plugin actually
// enabling, reading its data folder and answering its whole command tree.
// ---------------------------------------------------------------------------
tasks.register("smokeTest") {
    group = "verification"
    description = "Boots the built jar on a real Paper server, dispatches every command root and fails on errors."
    dependsOn(tasks.jar)

    doLast {
        val serverJar = resolvePaperServerJar(project)
        val pluginJar = project.layout.buildDirectory.dir("libs").get().asFile
            .listFiles { file -> file.name.startsWith("factionsone-") && file.extension == "jar" }
            ?.maxByOrNull(File::lastModified)
            ?: throw GradleException("smokeTest: build/libs has no factionsone-*.jar; run 'jar' first")

        val workDir = project.layout.buildDirectory.dir("smoke").get().asFile
        workDir.deleteRecursively()
        workDir.mkdirs()

        // Warm the launcher caches from the local test server so the boot works offline.
        listOf("cache", "libraries").forEach { name ->
            val source = project.file("test-server/$name")
            if (source.isDirectory) {
                source.copyRecursively(File(workDir, name), overwrite = true)
            }
        }

        File(workDir, "eula.txt").writeText("eula=true\n")
        File(workDir, "server.properties").writeText(smokeServerProperties())
        val pluginsDir = File(workDir, "plugins").apply { mkdirs() }
        pluginJar.copyTo(File(pluginsDir, pluginJar.name), overwrite = true)

        val javaExecutable = project.javaToolchains
            .launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) }
            .get().executablePath.asFile.absolutePath

        val commandRoots = listOf(
            "", "access", "admin", "autoclaim", "ban", "banlist", "chat", "claim", "config",
            "create", "deinvite", "delwarp", "demote", "desc", "disband", "flag", "fly",
            "help", "home", "invite", "join", "kick", "leader", "leave", "list", "lock",
            "logs", "mail", "map", "menu", "missions", "money", "officer", "open", "perm", "power",
            "powerboost", "promote", "ally", "enemy", "neutral", "truce", "reload", "saveall",
            "sc", "setwarp", "sethome", "shield", "shop", "show", "status", "stuck", "coords", "near", "top", "tag", "tax", "title", "tnt", "unban",
            "unclaim", "unclaimall", "upgrade", "version", "warp", "warps"
        )

        val logFile = File(workDir, "smoke.log")
        val log = StringBuilder()
        val process = ProcessBuilder(
            javaExecutable, "-Xms512M", "-Xmx1G",
            "-jar", serverJar.absolutePath, "--nogui"
        )
            .directory(workDir)
            .redirectErrorStream(true)
            .start()

        val writer = logFile.bufferedWriter()
        val reader = Thread {
            try {
                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        synchronized(log) { log.append(line).append('\n') }
                        synchronized(writer) {
                            writer.write(line)
                            writer.newLine()
                            writer.flush()
                        }
                    }
                }
            } catch (ignored: Exception) {
                // The stream closes when the server exits; everything read so far is written.
            }
        }.apply {
            isDaemon = true
            start()
        }

        try {
            val deadline = System.currentTimeMillis() + 300_000L
            while (System.currentTimeMillis() < deadline && !synchronized(log) { log.contains("Done (") }) {
                if (!process.isAlive) {
                    throw GradleException("smokeTest: server exited before it was ready; see ${logFile.absolutePath}")
                }
                Thread.sleep(500L)
            }
            if (!synchronized(log) { log.contains("Done (") }) {
                throw GradleException("smokeTest: server did not reach 'Done' within 300s; see ${logFile.absolutePath}")
            }

            val stdin = process.outputStream
            for (root in commandRoots) {
                val command = if (root.isEmpty()) "factions" else "factions $root"
                stdin.write((command + "\n").toByteArray(Charsets.UTF_8))
                stdin.flush()
                Thread.sleep(150L)
            }
            stdin.write("stop\n".toByteArray(Charsets.UTF_8))
            stdin.flush()
            stdin.close()

            if (!process.waitFor(180, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                throw GradleException("smokeTest: server did not stop within 180s")
            }
            reader.join(5_000L)
        } finally {
            writer.close()
            if (process.isAlive) {
                process.destroyForcibly()
            }
        }

        val text = synchronized(log) { log.toString() }
        if (!text.contains("=== ENABLE DONE")) {
            throw GradleException("smokeTest: FactionsOne did not finish enabling; see ${logFile.absolutePath}")
        }

        // Paper's own update checker and auth discovery need internet; their
        // offline failures are not plugin failures.
        val serverNoise = listOf("PaperVersionFetcher", "authlib", "UnknownHostException",
            "MinecraftClientException", "SocketTimeoutException", "discovery.minecraftservices")
        val failures = text.lines().filter { line ->
            val relevant = line.contains("ERROR]") || line.contains("SEVERE") ||
                line.contains("Exception") || line.contains("Could not load") ||
                line.contains("Unknown command")
            relevant && serverNoise.none { line.contains(it) }
        }
        if (failures.isNotEmpty()) {
            throw GradleException(
                "smokeTest: ${failures.size} error line(s) in the server log:\n" +
                    failures.take(20).joinToString("\n") +
                    "\nFull log: ${logFile.absolutePath}"
            )
        }

        project.logger.lifecycle(
            "smokeTest: PASSED — ${commandRoots.size} command roots, ${text.lines().size} log lines"
        )
    }
}

tasks.register<JavaCompile>("probeClasses") {
    group = "verification"
    description = "Compiles the local Vault stand-in used by the live bank test."
    val probeSource = project.file("test-server/econprobe/src")
    onlyIf { probeSource.isDirectory }
    source(probeSource)
    classpath = probeCompile
    destinationDirectory.set(project.layout.buildDirectory.dir("econprobe/classes"))
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.register<Jar>("probeJar") {
    group = "verification"
    description = "Packages the local Vault stand-in used by the live bank test."
    dependsOn("probeClasses")
    onlyIf { project.file("test-server/econprobe/src").isDirectory }
    archiveBaseName.set("Vault")
    archiveVersion.set("1.0.0")
    destinationDirectory.set(project.layout.buildDirectory.dir("econprobe"))
    from(project.layout.buildDirectory.dir("econprobe/classes"))
    from(project.file("test-server/econprobe/plugin.yml"))
    // The real Vault plugin ships the API classes; this stand-in must too.
    from({
        probeCompile.filter { it.name.startsWith("VaultAPI") }
            .map { project.zipTree(it) }
    })
}

tasks.register<JavaCompile>("refundProbeClasses") {
    group = "verification"
    description = "Compiles the local mailbox refund crash probe used by the live refund test."
    val probeSource = project.file("test-server/refundprobe/src")
    onlyIf { probeSource.isDirectory }
    source(probeSource)
    classpath = probeCompile
    destinationDirectory.set(project.layout.buildDirectory.dir("refundprobe/classes"))
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.register<Jar>("refundProbeJar") {
    group = "verification"
    description = "Packages the local mailbox refund crash probe used by the live refund test."
    dependsOn("refundProbeClasses")
    onlyIf { project.file("test-server/refundprobe/src").isDirectory }
    archiveBaseName.set("RefundProbe")
    archiveVersion.set("1.0.0")
    destinationDirectory.set(project.layout.buildDirectory.dir("refundprobe"))
    from(project.layout.buildDirectory.dir("refundprobe/classes"))
    from(project.file("test-server/refundprobe/plugin.yml"))
}

tasks.register("liveTest") {
    group = "verification"
    description = "Runs the local Paper crash harnesses for journal compensation and mailbox refunds."
    dependsOn(tasks.jar, "probeJar", "refundProbeJar")

    doLast {
        val paperJar = resolvePaperServerJarOrNull(project)
        val node = resolveNodeExecutable()
        val harnesses = listOf("live-journal.js", "live-refund.js")
        val missing = mutableListOf<String>()
        if (paperJar == null) {
            missing.add("Paper server jar (test-server/paper-26.3-259.jar, -PpaperServerJar or PAPER_SERVER_JAR)")
        }
        if (node == null) {
            missing.add("node executable on PATH")
        }
        for (harness in harnesses) {
            if (!project.file("test-server/$harness").isFile) {
                missing.add("test-server/$harness")
            }
        }
        for (probe in listOf("econprobe", "refundprobe")) {
            if (!project.file("test-server/$probe/src").isDirectory) {
                missing.add("test-server/$probe")
            }
        }
        if (missing.isNotEmpty()) {
            project.logger.lifecycle("liveTest: SKIPPED — missing " + missing.joinToString(", "))
            return@doLast
        }
        val executable = node ?: return@doLast
        val logDir = project.layout.buildDirectory.dir("live-test").get().asFile.apply { mkdirs() }
        for (harness in harnesses) {
            val logFile = File(logDir, harness.removeSuffix(".js") + ".log")
            val process = ProcessBuilder(executable,
                project.file("test-server/$harness").absolutePath)
                .directory(project.projectDir)
                .redirectErrorStream(true)
                .redirectOutput(logFile)
                .start()
            val finished = process.waitFor(10, TimeUnit.MINUTES)
            if (!finished) {
                process.destroyForcibly()
                throw GradleException(
                    "liveTest: $harness timed out after 10 minutes; see ${logFile.absolutePath}"
                )
            }
            if (process.exitValue() != 0) {
                val tail = logFile.readLines().takeLast(20).joinToString("\n")
                throw GradleException(
                    "liveTest: $harness failed with exit ${process.exitValue()}; " +
                        "see ${logFile.absolutePath}\n$tail"
                )
            }
            project.logger.lifecycle(
                "liveTest: PASSED — $harness (log ${logFile.absolutePath})"
            )
        }
    }
}

tasks.register("probeClasspath") {
    group = "verification"
    description = "Prints the classpath used to compile the local Vault stand-in."
    doLast {
        println(probeCompile.resolve().joinToString(File.pathSeparator) { it.absolutePath })
    }
}

fun resolvePaperServerJarOrNull(project: Project): File? {
    val candidates = mutableListOf<File>()
    (project.findProperty("paperServerJar") as String?)?.let { candidates.add(project.file(it)) }
    System.getenv("PAPER_SERVER_JAR")?.let { candidates.add(project.file(it)) }
    candidates.add(project.file("test-server/paper-26.3-259.jar"))
    return candidates.firstOrNull { it.isFile }
}

fun resolvePaperServerJar(project: Project): File = resolvePaperServerJarOrNull(project)
    ?: throw GradleException(
        "smokeTest: no Paper server jar found. Pass -PpaperServerJar=<path>, set PAPER_SERVER_JAR, " +
            "or place the 26.3 jar at test-server/paper-26.3-259.jar."
    )

fun resolveNodeExecutable(): String? {
    val names = if (System.getProperty("os.name").lowercase().contains("win"))
        listOf("node.exe", "node") else listOf("node")
    for (name in names) {
        try {
            val process = ProcessBuilder(name, "--version")
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start()
            val finished = process.waitFor(5, TimeUnit.SECONDS)
            if (finished && process.exitValue() == 0) {
                return name
            }
            if (!finished) {
                process.destroyForcibly()
            }
        } catch (ignored: Exception) {
            // Try the next executable name.
        }
    }
    return null
}

fun smokeServerProperties(): String = """
    online-mode=false
    level-type=minecraft\:normal
    generate-structures=false
    spawn-protection=0
    max-players=5
    view-distance=4
    simulation-distance=4
    allow-nether=false
    allow-end=false
    enable-command-block=false
    difficulty=peaceful
    motd=FactionsOne smoke test
    server-port=${(25600..25900).random()}
""".trimIndent() + "\n"
