package com.massivecraft.factions.bank;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.massivecraft.factions.zcore.util.BackupService;
import com.massivecraft.factions.zcore.util.VersionedFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Append-only journal for bank movements.
 *
 * <p>Every movement is first written as {@link State#PENDING}; the final state
 * is appended as a second record. The latest record for an id is the current
 * state, so replaying the file reconstructs what happened without rewriting
 * history. A failed refund becomes {@link State#COMPENSATION_PENDING} and is
 * retried by {@link BankService#retryPendingCompensations()}.
 *
 * <p>This is a durable retry log, not a transactional WAL on the economy
 * provider: a crash between the provider call and the final record can leave a
 * pending movement that needs manual reconciliation.
 *
 * @author MarcDoesntExists
 */
public final class BankJournal {

    private static final int CURRENT_VERSION = 2;
    private final Path file;
    private final Logger logger;
    private final Clock clock;
    private final Gson gson = new GsonBuilder().create();
    private final Map<String, Entry> latest = new LinkedHashMap<>();
    private boolean locked;
    public BankJournal(Path file, Logger logger) {
        this(file, logger, Clock.systemUTC());
    }
    public BankJournal(Path file, Logger logger, Clock clock) {
        this.file = file;
        this.logger = logger;
        this.clock = clock;
        load();
    }

    private static boolean isTerminal(State state) {
        return state == State.COMMITTED || state == State.ABORTED
            || state == State.COMPENSATED;
    }

    private static JsonElement parseLine(String line) {
        try {
            return JsonParser.parseString(line);
        } catch (RuntimeException invalid) {
            return null;
        }
    }

    public synchronized String begin(String operation, String from, String to, double amount,
                                     String actor, String compensationAccount,
                                     double compensationAmount, String detail) {
        String id = UUID.randomUUID().toString();
        Entry entry = new Entry(id, this.clock.millis(), State.PENDING, operation, from, to,
            amount, actor, compensationAccount, compensationAmount, detail);
        return append(entry) ? id : null;
    }

    public synchronized boolean commit(String id, String detail) {
        return transition(id, State.COMMITTED, detail);
    }

    public synchronized boolean abort(String id, String detail) {
        return transition(id, State.ABORTED, detail);
    }

    public synchronized boolean compensationPending(String id, String detail) {
        return transition(id, State.COMPENSATION_PENDING, detail);
    }

    public synchronized boolean compensated(String id, String detail) {
        return transition(id, State.COMPENSATED, detail);
    }

    public synchronized Entry entry(String id) {
        return this.latest.get(id);
    }

    public synchronized List<Entry> entries() {
        return Collections.unmodifiableList(new ArrayList<>(this.latest.values()));
    }

    public synchronized List<Entry> pendingCompensations() {
        List<Entry> pending = new ArrayList<>();
        for (Entry entry : this.latest.values()) {
            if (entry.state() != State.COMPENSATION_PENDING
                || entry.compensationAccount() == null
                || entry.compensationAccount().isBlank()
                || entry.compensationAmount() <= 0.0) {
                continue;
            }
            pending.add(entry);
        }
        return pending;
    }

    public synchronized int pendingCompensationCount() {
        return pendingCompensations().size();
    }

    /**
     * Drops terminal entries whose last record is older than the window.
     * Pending movements and pending compensations are never removed. The file
     * is rewritten atomically after a rotated backup is taken.
     *
     * @return number of entries removed
     */
    public synchronized int retain(long now, long retentionMillis) {
        if (this.locked) {
            return 0;
        }
        long cutoff = now - Math.max(0L, retentionMillis);
        Map<String, Entry> kept = new LinkedHashMap<>();
        int removed = 0;
        for (Entry entry : this.latest.values()) {
            if (isTerminal(entry.state()) && entry.at() <= cutoff) {
                removed++;
            } else {
                kept.put(entry.id(), entry);
            }
        }
        if (removed == 0 || !rewrite(kept)) {
            return 0;
        }
        this.latest.clear();
        this.latest.putAll(kept);
        return removed;
    }

    public Path file() {
        return this.file;
    }

    private boolean rewrite(Map<String, Entry> entries) {
        if (this.locked) {
            return false;
        }
        if (Files.exists(this.file)) {
            BackupService.backupBeforeWrite(this.file);
        }
        Path temporary = this.file.resolveSibling(
            this.file.getFileName() + "." + UUID.randomUUID() + ".tmp");
        try {
            Path parent = this.file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            StringBuilder text = new StringBuilder(headerLine()).append('\n');
            for (Entry entry : entries.values()) {
                text.append(this.gson.toJson(Stored.from(entry))).append('\n');
            }
            Files.writeString(temporary, text.toString(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, this.file, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException fallback) {
                Files.move(temporary, this.file, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException exception) {
            this.logger.log(Level.SEVERE, "Could not compact bank journal " + this.file,
                exception);
            return false;
        } finally {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException ignored) {
                // A fresh temporary name is used on the next rewrite.
            }
        }
    }

    private boolean transition(String id, State state, String detail) {
        Entry current = this.latest.get(id);
        if (current == null) {
            return false;
        }
        return append(new Entry(current.id(), this.clock.millis(), state, current.operation(),
            current.from(), current.to(), current.amount(), current.actor(),
            current.compensationAccount(), current.compensationAmount(), detail));
    }

    private boolean append(Entry entry) {
        if (this.locked) {
            return false;
        }
        try {
            Path parent = this.file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            String line = this.gson.toJson(Stored.from(entry)) + "\n";
            boolean empty = !Files.exists(this.file) || Files.size(this.file) == 0L;
            String text = empty ? headerLine() + "\n" + line : line;
            Files.writeString(this.file, text, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            this.latest.put(entry.id(), entry);
            return true;
        } catch (IOException exception) {
            this.logger.log(Level.SEVERE, "Could not append to bank journal " + this.file, exception);
            return false;
        }
    }

    private void load() {
        if (!Files.exists(this.file)) {
            return;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(this.file, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            this.logger.log(Level.SEVERE, "Could not read bank journal " + this.file,
                exception);
            VersionedFile.quarantine(this.file, this.logger);
            return;
        }

        int firstNonBlank = -1;
        for (int index = 0; index < lines.size(); index++) {
            if (!lines.get(index).isBlank()) {
                firstNonBlank = index;
                break;
            }
        }

        int headerIndex = -1;
        Integer headerVersion = null;
        if (firstNonBlank >= 0) {
            JsonElement element = parseLine(lines.get(firstNonBlank));
            if (element != null && element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                if (object.has("version") && !object.has("id")) {
                    headerIndex = firstNonBlank;
                    headerVersion = VersionedFile.version(object);
                }
            }
        }

        if (headerVersion != null && headerVersion > CURRENT_VERSION) {
            this.locked = true;
            this.logger.severe("Bank journal " + this.file + " uses version " + headerVersion
                + " but this build supports " + CURRENT_VERSION
                + "; refusing to load or append.");
            return;
        }

        int validEntries = 0;
        for (int index = 0; index < lines.size(); index++) {
            if (index == headerIndex || lines.get(index).isBlank()) {
                continue;
            }
            try {
                Stored stored = this.gson.fromJson(lines.get(index), Stored.class);
                Entry entry = stored == null ? null : stored.toEntry();
                if (entry != null) {
                    this.latest.put(entry.id(), entry);
                    validEntries++;
                }
            } catch (RuntimeException malformed) {
                this.logger.log(Level.WARNING,
                    "Ignoring a malformed bank journal line in " + this.file, malformed);
            }
        }

        if (headerVersion == null && validEntries == 0 && firstNonBlank >= 0) {
            this.logger.severe("Bank journal " + this.file
                + " has no readable records; quarantining it.");
            VersionedFile.quarantine(this.file, this.logger);
            this.latest.clear();
            return;
        }

        if (headerVersion == null || headerVersion < CURRENT_VERSION) {
            int fromVersion = headerVersion == null
                ? VersionedFile.LEGACY_VERSION : headerVersion;
            if (rewrite(this.latest)) {
                this.logger.info("Migrated bank journal " + this.file + " from version "
                    + fromVersion + " to " + CURRENT_VERSION);
            } else {
                this.logger.warning("Could not migrate bank journal " + this.file
                    + " to version " + CURRENT_VERSION);
            }
        }
    }

    private String headerLine() {
        JsonObject header = new JsonObject();
        header.addProperty("version", CURRENT_VERSION);
        return this.gson.toJson(header);
    }

    public enum State {
        PENDING, COMMITTED, ABORTED, COMPENSATION_PENDING, COMPENSATED
    }

    public record Entry(String id, long at, State state, String operation, String from,
                        String to, double amount, String actor, String compensationAccount,
                        double compensationAmount, String detail) {
    }

    /**
     * On-disk shape. Gson needs mutable fields and a no-argument constructor.
     */
    private static final class Stored {
        String id;
        long at;
        String state;
        String operation;
        String from;
        String to;
        double amount;
        String actor;
        String compensationAccount;
        double compensationAmount;
        String detail;

        static Stored from(Entry entry) {
            Stored stored = new Stored();
            stored.id = entry.id();
            stored.at = entry.at();
            stored.state = entry.state().name();
            stored.operation = entry.operation();
            stored.from = entry.from();
            stored.to = entry.to();
            stored.amount = entry.amount();
            stored.actor = entry.actor();
            stored.compensationAccount = entry.compensationAccount();
            stored.compensationAmount = entry.compensationAmount();
            stored.detail = entry.detail();
            return stored;
        }

        Entry toEntry() {
            if (this.id == null || this.id.isBlank() || this.state == null) {
                return null;
            }
            State parsed;
            try {
                parsed = State.valueOf(this.state);
            } catch (IllegalArgumentException invalidState) {
                return null;
            }
            return new Entry(this.id, this.at, parsed, this.operation, this.from, this.to,
                this.amount, this.actor, this.compensationAccount, this.compensationAmount,
                this.detail);
        }
    }
}
