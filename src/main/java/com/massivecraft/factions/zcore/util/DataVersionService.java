package com.massivecraft.factions.zcore.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Tracks the on-disk data version and prepares the data folder before the
 * plugin reads any JSON file.
 *
 * <p>Unversioned data is snapshotted into
 * {@code backups/pre-migration-<timestamp>/} and then marked with the current
 * version. A data folder from a newer build, or one with a corrupt version
 * file, is refused instead of being silently overwritten.</p>
 *
 * @author MarcDoesntExists
 */
public final class DataVersionService {

    static final String VERSION_FILE = "data-version.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter
            .ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneOffset.UTC);

    private DataVersionService() {
    }

    /**
     * Prepares the data folder before any load or save.
     *
     * @param dataFolder plugin data folder
     * @param logger     logger used for the snapshot and refusal messages
     * @return {@code true} when the data can be loaded safely
     */
    public static boolean prepare(Path dataFolder, Logger logger) {
        try {
            Files.createDirectories(dataFolder);
            Path versionFile = dataFolder.resolve(VERSION_FILE);
            int storedVersion = readVersion(versionFile);

            if (storedVersion > Migrations.CURRENT_VERSION) {
                logger.severe("Data folder uses version " + storedVersion
                        + " but this build supports up to " + Migrations.CURRENT_VERSION
                        + "; refusing to load it.");
                return false;
            }

            if (storedVersion == Migrations.CURRENT_VERSION) {
                return true;
            }

            if (hasExistingData(dataFolder)) {
                Path snapshot = createSnapshot(dataFolder, storedVersion);
                logger.info("Backed up unversioned data to "
                        + snapshot.toAbsolutePath());
            }

            Migrations.migrate(dataFolder, storedVersion, Migrations.CURRENT_VERSION);
            writeVersion(versionFile);
            return true;
        } catch (IOException | RuntimeException ex) {
            logger.log(Level.SEVERE,
                    "Could not prepare the data folder; refusing to enable.", ex);
            return false;
        }
    }

    private static int readVersion(Path versionFile) throws IOException {
        if (!Files.exists(versionFile)) {
            return 0;
        }

        JsonElement parsed = JsonParser.parseString(Files.readString(versionFile));
        if (!parsed.isJsonObject()) {
            throw new IOException("Data version file is not a JSON object");
        }
        JsonElement version = parsed.getAsJsonObject().get("version");
        if (version == null || !version.isJsonPrimitive()
                || !version.getAsJsonPrimitive().isNumber()) {
            throw new IOException("Data version file has no numeric version");
        }

        int value;
        try {
            value = Integer.parseInt(version.getAsString());
        } catch (NumberFormatException ex) {
            throw new IOException("Data version must be an integer", ex);
        }
        if (value < 1) {
            throw new IOException("Data version must be at least 1");
        }
        return value;
    }

    private static boolean hasExistingData(Path dataFolder) throws IOException {
        try (Stream<Path> stream = Files.list(dataFolder)) {
            return stream.anyMatch(path -> Files.isRegularFile(path)
                    && !path.getFileName().toString().equals(VERSION_FILE));
        }
    }

    private static Path createSnapshot(Path dataFolder, int fromVersion) throws IOException {
        Path backupRoot = dataFolder.resolve("backups");
        Files.createDirectories(backupRoot);

        Path snapshot = uniqueSnapshotPath(backupRoot);
        Path temporary = backupRoot.resolve(snapshot.getFileName().toString() + ".tmp");
        Files.createDirectories(temporary);

        try {
            List<Path> files;
            try (Stream<Path> stream = Files.list(dataFolder)) {
                files = stream
                        .filter(Files::isRegularFile)
                        .filter(path -> !path.getFileName().toString().equals(VERSION_FILE))
                        .toList();
            }

            for (Path file : files) {
                Files.copy(file, temporary.resolve(file.getFileName()),
                        StandardCopyOption.REPLACE_EXISTING);
            }

            Files.writeString(temporary.resolve("MANIFEST.txt"),
                    "FactionsOne data snapshot\n"
                            + "created=" + Instant.now() + "\n"
                            + "fromVersion=" + fromVersion + "\n"
                            + "toVersion=" + Migrations.CURRENT_VERSION + "\n");

            moveDirectory(temporary, snapshot);
            return snapshot;
        } catch (IOException | RuntimeException ex) {
            deleteRecursively(temporary);
            throw ex;
        }
    }

    private static Path uniqueSnapshotPath(Path backupRoot) {
        String stamp = TIMESTAMP.format(Instant.now());
        Path candidate = backupRoot.resolve("pre-migration-" + stamp);
        int suffix = 1;
        while (Files.exists(candidate)) {
            candidate = backupRoot.resolve("pre-migration-" + stamp + "-" + suffix++);
        }
        return candidate;
    }

    private static void moveDirectory(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException | UnsupportedOperationException ex) {
            Files.move(source, target);
        }
    }

    private static void deleteRecursively(Path path) {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder()).forEach(candidate -> {
                try {
                    Files.deleteIfExists(candidate);
                } catch (IOException ignored) {
                    // Best effort cleanup of an incomplete snapshot.
                }
            });
        } catch (IOException ignored) {
            // Best effort cleanup of an incomplete snapshot.
        }
    }

    private static void writeVersion(Path versionFile) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("version", Migrations.CURRENT_VERSION);
        DiscUtil.write(versionFile.toFile(), GSON.toJson(root));
    }
}
