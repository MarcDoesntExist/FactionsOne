package com.massivecraft.factions.zcore.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Version helpers for a single durable data file.
 *
 * <p>An unversioned file is treated as version {@link #LEGACY_VERSION}. A file
 * that cannot be parsed is moved to {@code <name>.corrupt-<timestamp>} and the
 * caller starts from empty state, so corrupt evidence is never overwritten.</p>
 *
 * @author MarcDoesntExists
 */
public final class VersionedFile {

    public static final int LEGACY_VERSION = 1;

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter
        .ofPattern("yyyyMMdd-HHmmss-SSS")
        .withZone(ZoneOffset.UTC);

    private VersionedFile() {
    }

    /**
     * Reads a file as JSON. Returns {@code null} when the file is missing or
     * unreadable (the unreadable file is quarantined), and an empty object for
     * a blank file.
     */
    public static JsonElement read(Path file, Logger logger) {
        if (file == null || !Files.exists(file)) {
            return null;
        }
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            if (text.isBlank()) {
                return new JsonObject();
            }
            return JsonParser.parseString(text);
        } catch (IOException | RuntimeException exception) {
            logger.log(Level.SEVERE, "Could not read data file " + file, exception);
            quarantine(file, logger);
            return null;
        }
    }

    /**
     * Reads the numeric {@code version} of a root object. Missing or malformed
     * versions are treated as {@link #LEGACY_VERSION}.
     */
    public static int version(JsonObject root) {
        JsonElement value = root == null ? null : root.get("version");
        if (value == null || !value.isJsonPrimitive()
            || !value.getAsJsonPrimitive().isNumber()) {
            return LEGACY_VERSION;
        }
        return value.getAsInt();
    }

    /**
     * Copies the file into {@code backups/} before a format rewrite.
     */
    public static void backup(Path file) {
        if (file != null && Files.isRegularFile(file)) {
            BackupService.backupBeforeWrite(file);
        }
    }

    /**
     * Moves a corrupt file aside so a fresh store can be created without losing
     * the original bytes for manual inspection.
     *
     * @return the quarantine path, or {@code null} when the move failed
     */
    public static Path quarantine(Path file, Logger logger) {
        if (file == null || !Files.exists(file)) {
            return null;
        }
        Path target = uniqueCorruptPath(file);
        try {
            try {
                Files.move(file, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException | UnsupportedOperationException fallback) {
                Files.move(file, target);
            }
            logger.severe("Moved corrupt data file " + file.toAbsolutePath()
                + " to " + target.toAbsolutePath() + "; starting from empty state.");
            return target;
        } catch (IOException exception) {
            logger.log(Level.SEVERE, "Could not quarantine corrupt data file " + file,
                exception);
            return null;
        }
    }

    /**
     * Writes a file with a temporary sibling and an atomic move when supported.
     */
    public static boolean write(Path file, String content, Logger logger) {
        Path temporary = file.resolveSibling(
            file.getFileName() + "." + UUID.randomUUID() + ".tmp");
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException | UnsupportedOperationException fallback) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException exception) {
            logger.log(Level.SEVERE, "Could not write data file " + file, exception);
            return false;
        } finally {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException ignored) {
                // A fresh temporary name is used on the next write.
            }
        }
    }

    private static Path uniqueCorruptPath(Path file) {
        String stamp = TIMESTAMP.format(Instant.now());
        String name = file.getFileName() + ".corrupt-" + stamp;
        Path candidate = file.resolveSibling(name);
        int suffix = 1;
        while (Files.exists(candidate)) {
            candidate = file.resolveSibling(name + "-" + suffix++);
        }
        return candidate;
    }
}
