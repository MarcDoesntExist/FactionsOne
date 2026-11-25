package com.massivecraft.factions.bank;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless checks for idempotency file versioning and recovery.
 *
 * @author MarcDoesntExists
 */
class IdempotencyStoreVersionTest {

    @TempDir
    Path directory;

    @Test
    void legacyEntriesAreMigratedWithABackup() throws IOException {
        Path file = this.directory.resolve("idempotency.json");
        Files.writeString(file, "{\"entries\":{\"tax:a:1\":1234}}",
                StandardCharsets.UTF_8);

        IdempotencyStore store = store(file);
        assertTrue(store.isClaimed("tax:a:1"));

        JsonObject root = read(file);
        assertEquals(2, root.get("version").getAsInt());
        assertEquals(1234L, root.getAsJsonObject("entries")
                .get("tax:a:1").getAsLong());
        assertTrue(Files.readString(backup(file), StandardCharsets.UTF_8)
                .contains("tax:a:1"));
    }

    @Test
    void emptyFileIsMigratedToTheCurrentVersion() throws IOException {
        Path file = this.directory.resolve("idempotency.json");
        Files.writeString(file, "", StandardCharsets.UTF_8);

        assertEquals(0, store(file).size());

        assertEquals(2, read(file).get("version").getAsInt());
        assertTrue(Files.exists(backup(file)));
    }

    @Test
    void corruptFileIsQuarantinedAndStartsEmpty() throws IOException {
        Path file = this.directory.resolve("idempotency.json");
        Files.writeString(file, "not-json", StandardCharsets.UTF_8);

        IdempotencyStore store = store(file);
        assertEquals(0, store.size());
        assertFalse(Files.exists(file));

        Path quarantined = quarantined(file);
        assertEquals("not-json", Files.readString(quarantined, StandardCharsets.UTF_8));
    }

    @Test
    void futureVersionIsLeftUntouched() throws IOException {
        Path file = this.directory.resolve("idempotency.json");
        String original = "{\"version\":99,\"entries\":{\"x\":1}}";
        Files.writeString(file, original, StandardCharsets.UTF_8);

        assertEquals(0, store(file).size());

        assertEquals(original, Files.readString(file, StandardCharsets.UTF_8));
        assertFalse(Files.exists(this.directory.resolve("backups")));
        assertFalse(hasQuarantined(file));
    }

    private IdempotencyStore store(Path file) {
        return new IdempotencyStore(file, Logger.getLogger("test"));
    }

    private static JsonObject read(Path file) throws IOException {
        return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
                .getAsJsonObject();
    }

    private Path backup(Path file) throws IOException {
        Path backups = this.directory.resolve("backups");
        assertTrue(Files.isDirectory(backups));
        try (Stream<Path> stream = Files.list(backups)) {
            List<Path> files = stream
                    .filter(path -> path.getFileName().toString()
                            .startsWith(file.getFileName() + "."))
                    .toList();
            assertEquals(1, files.size());
            return files.get(0);
        }
    }

    private Path quarantined(Path file) throws IOException {
        try (Stream<Path> stream = Files.list(this.directory)) {
            List<Path> files = stream
                    .filter(path -> path.getFileName().toString()
                            .startsWith(file.getFileName() + ".corrupt-"))
                    .toList();
            assertEquals(1, files.size());
            return files.get(0);
        }
    }

    private boolean hasQuarantined(Path file) throws IOException {
        try (Stream<Path> stream = Files.list(this.directory)) {
            return stream.anyMatch(path -> path.getFileName().toString()
                    .startsWith(file.getFileName() + ".corrupt-"));
        }
    }
}
