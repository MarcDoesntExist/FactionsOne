package com.massivecraft.factions.zcore.util;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless tests for data version preparation and pre-migration snapshots.
 *
 * @author MarcDoesntExists
 */
class DataVersionServiceTest {

    private static final Logger LOGGER = Logger.getLogger("DataVersionServiceTest");

    @TempDir
    Path tempDir;

    @Test
    void freshFolderGetsACurrentVersionWithoutSnapshot() throws IOException {
        assertTrue(DataVersionService.prepare(tempDir, LOGGER));

        assertEquals(1, readVersion());
        assertTrue(snapshots().isEmpty());
    }

    @Test
    void existingDataGetsASnapshotBeforeTheVersionMarker() throws IOException {
        Files.writeString(tempDir.resolve("factions.json"), "factions");
        Files.writeString(tempDir.resolve("players.json"), "players");

        assertTrue(DataVersionService.prepare(tempDir, LOGGER));

        assertEquals(1, readVersion());
        List<Path> snapshots = snapshots();
        assertEquals(1, snapshots.size());

        Path snapshot = snapshots.get(0);
        assertEquals("factions", Files.readString(snapshot.resolve("factions.json")));
        assertEquals("players", Files.readString(snapshot.resolve("players.json")));
        assertTrue(Files.exists(snapshot.resolve("MANIFEST.txt")));
        assertFalse(Files.exists(snapshot.resolve(DataVersionService.VERSION_FILE)));
    }

    @Test
    void preparationIsIdempotent() throws IOException {
        Files.writeString(tempDir.resolve("board.json"), "board");

        assertTrue(DataVersionService.prepare(tempDir, LOGGER));
        assertTrue(DataVersionService.prepare(tempDir, LOGGER));

        assertEquals(1, readVersion());
        assertEquals(1, snapshots().size());
    }

    @Test
    void newerDataVersionIsRefusedWithoutTouchingTheFolder() throws IOException {
        Files.writeString(tempDir.resolve(DataVersionService.VERSION_FILE), "{\"version\": 99}");
        Files.writeString(tempDir.resolve("factions.json"), "factions");

        assertFalse(DataVersionService.prepare(tempDir, LOGGER));

        assertEquals(99, readVersion());
        assertTrue(snapshots().isEmpty());
        assertEquals("factions", Files.readString(tempDir.resolve("factions.json")));
    }

    @Test
    void corruptVersionFileIsRefused() throws IOException {
        Path versionFile = tempDir.resolve(DataVersionService.VERSION_FILE);
        Files.writeString(versionFile, "not-json");
        Files.writeString(tempDir.resolve("factions.json"), "factions");

        assertFalse(DataVersionService.prepare(tempDir, LOGGER));

        assertEquals("not-json", Files.readString(versionFile));
        assertTrue(snapshots().isEmpty());
    }

    @Test
    void fractionalVersionIsRefused() throws IOException {
        Path versionFile = tempDir.resolve(DataVersionService.VERSION_FILE);
        Files.writeString(versionFile, "{\"version\": 1.5}");
        Files.writeString(tempDir.resolve("factions.json"), "factions");

        assertFalse(DataVersionService.prepare(tempDir, LOGGER));

        assertEquals("{\"version\": 1.5}", Files.readString(versionFile));
        assertTrue(snapshots().isEmpty());
    }

    @Test
    void snapshotFailureLeavesTheFolderUnversioned() throws IOException {
        Files.writeString(tempDir.resolve("factions.json"), "factions");
        Files.writeString(tempDir.resolve("backups"), "not a directory");

        assertFalse(DataVersionService.prepare(tempDir, LOGGER));

        assertFalse(Files.exists(tempDir.resolve(DataVersionService.VERSION_FILE)));
        assertEquals("factions", Files.readString(tempDir.resolve("factions.json")));
    }

    private int readVersion() throws IOException {
        return JsonParser.parseString(Files.readString(
                        tempDir.resolve(DataVersionService.VERSION_FILE)))
                .getAsJsonObject()
                .get("version")
                .getAsInt();
    }

    private List<Path> snapshots() throws IOException {
        Path directory = tempDir.resolve("backups");
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(directory)) {
            return stream
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().startsWith("pre-migration-"))
                    .filter(path -> !path.getFileName().toString().endsWith(".tmp"))
                    .sorted()
                    .toList();
        }
    }
}
