package com.massivecraft.factions.bank;

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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless checks for bank journal file versioning and recovery.
 *
 * @author MarcDoesntExists
 */
class BankJournalVersionTest {

    private static final String LEGACY_ENTRY = "{\"id\":\"old\",\"at\":1,"
            + "\"state\":\"PENDING\",\"operation\":\"credit\",\"from\":\"\","
            + "\"to\":\"a\",\"amount\":5.0,\"actor\":\"system\","
            + "\"compensationAmount\":0.0,\"detail\":\"legacy\"}";

    @TempDir
    Path directory;

    @Test
    void legacyLinesAreMigratedWithHeaderAndBackup() throws IOException {
        Path file = this.directory.resolve("journal.jsonl");
        Files.writeString(file, LEGACY_ENTRY + "\n", StandardCharsets.UTF_8);

        BankJournal journal = journal(file);
        assertNotNull(journal.entry("old"));

        String text = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(text.startsWith("{\"version\":2}\n"), text);
        assertTrue(text.contains("\"id\":\"old\""));
        assertTrue(Files.readString(backup(file), StandardCharsets.UTF_8)
                .contains("\"old\""));
    }

    @Test
    void emptyFileGetsAHeader() throws IOException {
        Path file = this.directory.resolve("journal.jsonl");
        Files.writeString(file, "", StandardCharsets.UTF_8);

        assertTrue(journal(file).entries().isEmpty());

        assertTrue(Files.readString(file, StandardCharsets.UTF_8)
                .startsWith("{\"version\":2}\n"));
    }

    @Test
    void corruptFileIsQuarantinedAndNewWritesWork() throws IOException {
        Path file = this.directory.resolve("journal.jsonl");
        Files.writeString(file, "not-json\n", StandardCharsets.UTF_8);

        BankJournal journal = journal(file);
        assertTrue(journal.entries().isEmpty());
        assertFalse(Files.exists(file));
        assertEquals("not-json\n",
                Files.readString(quarantined(file), StandardCharsets.UTF_8));

        String id = journal.begin("credit", "", "a", 1.0, "system", null, 0.0, "fresh");
        assertNotNull(id);
        assertTrue(Files.readString(file, StandardCharsets.UTF_8)
                .startsWith("{\"version\":2}\n"));
        assertNotNull(journal(file).entry(id));
    }

    @Test
    void futureHeaderIsRefusedWithoutWriting() {
        Path file = this.directory.resolve("journal.jsonl");
        String original = "{\"version\":99}\n";
        try {
            Files.writeString(file, original, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }

        BankJournal journal = journal(file);
        assertTrue(journal.entries().isEmpty());
        assertNull(journal.begin("credit", "", "a", 1.0, "system", null, 0.0, "blocked"));
        assertEquals(0, journal.retain(0L, 0L));

        try {
            assertEquals(original, Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    private BankJournal journal(Path file) {
        return new BankJournal(file, Logger.getLogger("test"));
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
}
