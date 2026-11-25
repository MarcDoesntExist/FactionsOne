package com.massivecraft.factions.zcore.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless tests for the rotating backup service.
 *
 * @author MarcDoesntExists
 */
class BackupServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void copiesAnExistingFileWithoutModifyingIt() throws IOException {
        Path target = tempDir.resolve("factions.json");
        Files.writeString(target, "old");

        BackupService.backupBeforeWrite(target);

        List<Path> backups = backups();
        assertEquals(1, backups.size());
        assertTrue(backups.get(0).getFileName().toString().startsWith("factions.json."));
        assertTrue(backups.get(0).getFileName().toString().endsWith(".bak"));
        assertEquals("old", Files.readString(backups.get(0)));
        assertEquals("old", Files.readString(target));
    }

    @Test
    void missingFileDoesNotCreateABackupDirectory() {
        BackupService.backupBeforeWrite(tempDir.resolve("missing.json"));

        assertFalse(Files.exists(tempDir.resolve("backups")));
    }

    @Test
    void backupsHaveUniqueNamesEvenWithinTheSameMillisecond() throws IOException {
        Path target = tempDir.resolve("players.json");
        for (int index = 0; index < 5; index++) {
            Files.writeString(target, "content-" + index);
            BackupService.backupBeforeWrite(target, 10);
        }

        assertEquals(5, backups().size());
    }

    @Test
    void rotationKeepsOnlyTheNewestBackups() throws Exception {
        Path target = tempDir.resolve("board.json");
        for (int index = 0; index < 6; index++) {
            Files.writeString(target, "content-" + index);
            BackupService.backupBeforeWrite(target, 3);
            // Distinct timestamps make the retention order deterministic.
            Thread.sleep(5L);
        }

        List<Path> backups = backups();
        assertEquals(3, backups.size());

        List<String> contents = new ArrayList<>();
        for (Path backup : backups) {
            contents.add(Files.readString(backup));
        }
        assertTrue(contents.contains("content-5"));
        assertFalse(contents.contains("content-2"));
    }

    @Test
    void zeroKeepStillRetainsOneRecoveryPoint() throws IOException {
        Path target = tempDir.resolve("conf.json");
        for (int index = 0; index < 2; index++) {
            Files.writeString(target, "content-" + index);
            BackupService.backupBeforeWrite(target, 0);
        }

        assertEquals(1, backups().size());
    }

    @Test
    void discUtilBacksUpBeforeOverwriting() throws IOException {
        Path target = tempDir.resolve("factions.json");
        DiscUtil.write(target.toFile(), "first");
        DiscUtil.write(target.toFile(), "second");

        List<Path> backups = backups();
        assertEquals(1, backups.size());
        assertEquals("first", Files.readString(backups.get(0)));
        assertEquals("second", DiscUtil.read(target.toFile()));
    }

    @Test
    void discUtilDoesNotBackUpAFileThatDidNotExist() throws IOException {
        Path target = tempDir.resolve("new.json");
        DiscUtil.write(target.toFile(), "new");

        assertTrue(backups().isEmpty());
    }

    private List<Path> backups() throws IOException {
        Path directory = tempDir.resolve("backups");
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".bak"))
                    .sorted()
                    .toList();
        }
    }
}
