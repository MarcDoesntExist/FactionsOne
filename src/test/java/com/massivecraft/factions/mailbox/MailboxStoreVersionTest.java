package com.massivecraft.factions.mailbox;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless checks for mailbox file versioning and recovery.
 *
 * @author MarcDoesntExists
 */
class MailboxStoreVersionTest {

    private static final String LEGACY = "{\"claims\":[{"
            + "\"id\":\"c1\",\"playerId\":\"p1\",\"amount\":25.0,"
            + "\"sourceAccount\":\"src\",\"source\":\"test\",\"reason\":\"why\","
            + "\"createdAt\":1,\"expiresAt\":2,\"state\":\"PENDING\","
            + "\"refunded\":false,\"completedAt\":0}]}";

    @TempDir
    Path directory;

    @Test
    void legacyClaimsAreMigratedWithABackup() throws IOException {
        Path file = this.directory.resolve("mailbox.json");
        Files.writeString(file, LEGACY, StandardCharsets.UTF_8);

        MailboxStore store = store(file);
        Map<String, FactionMailbox.Claim> claims = store.load();
        assertEquals(1, claims.size());
        assertEquals(25.0, claims.get("c1").amount());

        JsonObject root = read(file);
        assertEquals(2, root.get("version").getAsInt());
        assertEquals(1, root.getAsJsonArray("claims").size());
        assertTrue(Files.readString(backup(file), StandardCharsets.UTF_8)
                .contains("\"c1\""));
    }

    @Test
    void emptyFileIsMigratedToTheCurrentVersion() throws IOException {
        Path file = this.directory.resolve("mailbox.json");
        Files.writeString(file, "", StandardCharsets.UTF_8);

        assertTrue(store(file).load().isEmpty());

        assertEquals(2, read(file).get("version").getAsInt());
        assertTrue(Files.exists(backup(file)));
    }

    @Test
    void corruptFileIsQuarantinedAndStartsEmpty() throws IOException {
        Path file = this.directory.resolve("mailbox.json");
        Files.writeString(file, "{broken", StandardCharsets.UTF_8);

        assertTrue(store(file).load().isEmpty());
        assertFalse(Files.exists(file));

        Path quarantined = quarantined(file);
        assertEquals("{broken", Files.readString(quarantined, StandardCharsets.UTF_8));
    }

    @Test
    void futureVersionIsLeftUntouched() throws IOException {
        Path file = this.directory.resolve("mailbox.json");
        String original = "{\"version\":99,\"claims\":[]}";
        Files.writeString(file, original, StandardCharsets.UTF_8);

        assertTrue(store(file).load().isEmpty());

        assertEquals(original, Files.readString(file, StandardCharsets.UTF_8));
        assertFalse(Files.exists(this.directory.resolve("backups")));
        assertFalse(hasQuarantined(file));
    }

    private MailboxStore store(Path file) {
        return new MailboxStore(file, Logger.getLogger("test"));
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
