package com.massivecraft.factions.zcore.util;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Keeps timestamped copies of data files before they are overwritten and
 * rotates older copies. Backups live in a {@code backups} directory next to
 * the data file, so they travel with the plugin data folder.
 *
 * <p>The copy is written to a temporary file and moved into place, exactly
 * like the data files themselves. A failed backup is logged and never blocks
 * the actual save.</p>
 *
 * @author MarcDoesntExists
 */
public final class BackupService {

    /**
     * Number of backups kept per data file.
     */
    public static final int DEFAULT_BACKUPS_TO_KEEP = 10;

    private static final Logger LOGGER = Logger.getLogger("FactionsOne");
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter
        .ofPattern("yyyyMMdd-HHmmss-SSS")
        .withZone(ZoneOffset.UTC);

    private BackupService() {
    }

    /**
     * Backs up an existing file using the default retention.
     *
     * @param target file that is about to be overwritten
     */
    public static void backupBeforeWrite(Path target) {
        backupBeforeWrite(target, DEFAULT_BACKUPS_TO_KEEP);
    }

    static void backupBeforeWrite(Path target, int keep) {
        if (target == null || !Files.isRegularFile(target)) {
            return;
        }

        Path absolute = target.toAbsolutePath().normalize();
        Path parent = absolute.getParent();
        if (parent == null) {
            return;
        }

        Path backupDirectory = parent.resolve("backups");
        try {
            Files.createDirectories(backupDirectory);
            String fileName = absolute.getFileName().toString();
            Path backup = uniqueBackupPath(backupDirectory, fileName);
            Path temporary = Files.createTempFile(backupDirectory, "backup-", ".tmp");
            try {
                Files.copy(absolute, temporary, StandardCopyOption.REPLACE_EXISTING);
                moveIntoPlace(temporary, backup);
            } finally {
                Files.deleteIfExists(temporary);
            }
            rotate(backupDirectory, fileName, keep);
        } catch (IOException | RuntimeException ex) {
            LOGGER.log(Level.WARNING,
                "Could not back up \"" + absolute + "\": " + ex.getMessage());
        }
    }

    private static Path uniqueBackupPath(Path directory, String fileName) {
        String stamp = TIMESTAMP.format(Instant.now());
        Path candidate = directory.resolve(fileName + "." + stamp + ".bak");
        int suffix = 1;
        while (Files.exists(candidate)) {
            candidate = directory.resolve(fileName + "." + stamp + "-" + suffix++ + ".bak");
        }
        return candidate;
    }

    private static void moveIntoPlace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException | UnsupportedOperationException ex) {
            // Some file systems cannot move atomically; fall back to a replace.
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void rotate(Path directory, String fileName, int keep) throws IOException {
        int limit = Math.max(1, keep);
        List<Path> backups;
        try (Stream<Path> stream = Files.list(directory)) {
            backups = stream
                .filter(Files::isRegularFile)
                .filter(path -> isBackup(path.getFileName().toString(), fileName))
                .sorted(Comparator
                    .comparing(BackupService::lastModified)
                    .thenComparing(path -> path.getFileName().toString())
                    .reversed())
                .toList();
        }

        for (int index = limit; index < backups.size(); index++) {
            Files.deleteIfExists(backups.get(index));
        }
    }

    private static boolean isBackup(String candidate, String fileName) {
        return candidate.startsWith(fileName + ".") && candidate.endsWith(".bak");
    }

    private static FileTime lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path);
        } catch (IOException ex) {
            return FileTime.fromMillis(0L);
        }
    }
}
