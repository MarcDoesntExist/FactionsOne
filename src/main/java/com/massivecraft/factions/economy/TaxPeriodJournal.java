package com.massivecraft.factions.economy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.massivecraft.factions.bank.IdempotencyStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Append-only journal of faction tax periods.
 *
 * <p>A period is written as {@link State#PENDING} before its idempotency key is
 * claimed. If the process stops between the claim and the debit, recovery marks
 * it {@link State#TAX_UNKNOWN}. Such a period is never charged again
 * automatically: an operator checks the bank and runs
 * {@code /f tax run <faction> retry}.
 *
 * @author MarcDoesntExists
 */
public final class TaxPeriodJournal {

    public enum State {
        PENDING, SETTLED, ABORTED, TAX_UNKNOWN
    }

    public record Entry(String key, String factionId, double amount, long at, State state,
                        String detail) {
    }

    private final Path file;
    private final Logger logger;
    private final Gson gson = new GsonBuilder().create();
    private final Map<String, Entry> latest = new LinkedHashMap<>();

    public TaxPeriodJournal(Path file, Logger logger) {
        this.file = file;
        this.logger = logger;
        load();
    }

    public synchronized boolean begin(String key, String factionId, double amount, long at) {
        if (key == null || key.isBlank() || factionId == null || factionId.isBlank()) {
            return false;
        }
        if (this.latest.containsKey(key)) {
            return false;
        }
        return append(new Entry(key, factionId, amount, at, State.PENDING, ""));
    }

    public synchronized boolean settled(String key, String detail) {
        return transition(key, State.SETTLED, detail);
    }

    public synchronized boolean abort(String key, String detail) {
        return transition(key, State.ABORTED, detail);
    }

    public synchronized boolean unknown(String key, String detail) {
        return transition(key, State.TAX_UNKNOWN, detail);
    }

    public synchronized Entry entry(String key) {
        return this.latest.get(key);
    }

    public synchronized Entry latestUnknown(String factionId) {
        Entry found = null;
        for (Entry entry : this.latest.values()) {
            if (entry.state() != State.TAX_UNKNOWN || !entry.factionId().equals(factionId)) {
                continue;
            }
            if (found == null || entry.at() >= found.at()) {
                found = entry;
            }
        }
        return found;
    }

    public synchronized List<Entry> entries() {
        return List.copyOf(this.latest.values());
    }

    /**
     * Resolves periods left pending by a previous run. A claimed key means the
     * charge may have happened and becomes {@link State#TAX_UNKNOWN}; an
     * unclaimed key was interrupted before money moved and is aborted.
     *
     * @return number of periods marked unknown
     */
    public synchronized int recover(IdempotencyStore idempotency) {
        int ambiguous = 0;
        for (Entry entry : new ArrayList<>(this.latest.values())) {
            if (entry.state() != State.PENDING) {
                continue;
            }
            if (idempotency != null && idempotency.isClaimed(entry.key())) {
                transition(entry.key(), State.TAX_UNKNOWN, "claimed without a settled record");
                this.logger.warning("Tax period " + entry.key() + " for faction "
                        + entry.factionId() + " has an unknown charge; check the bank and run "
                        + "/f tax run " + entry.factionId() + " retry.");
                ambiguous++;
            } else {
                transition(entry.key(), State.ABORTED, "interrupted before the claim");
            }
        }
        return ambiguous;
    }

    private boolean transition(String key, State state, String detail) {
        Entry current = this.latest.get(key);
        if (current == null || current.state() == state) {
            return false;
        }
        return append(new Entry(current.key(), current.factionId(), current.amount(),
                current.at(), state, detail == null ? "" : detail));
    }

    private boolean append(Entry entry) {
        try {
            Path parent = this.file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(this.file, this.gson.toJson(Stored.from(entry)) + "\n",
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
            this.latest.put(entry.key(), entry);
            return true;
        } catch (IOException exception) {
            this.logger.log(Level.SEVERE, "Could not append to tax journal " + this.file,
                    exception);
            return false;
        }
    }

    private void load() {
        if (!Files.exists(this.file)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(this.file, StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                try {
                    Stored stored = this.gson.fromJson(line, Stored.class);
                    Entry entry = stored == null ? null : stored.toEntry();
                    if (entry != null) {
                        this.latest.put(entry.key(), entry);
                    }
                } catch (RuntimeException malformed) {
                    this.logger.log(Level.WARNING,
                            "Ignoring a malformed tax journal line in " + this.file, malformed);
                }
            }
        } catch (IOException exception) {
            this.logger.log(Level.SEVERE, "Could not read tax journal " + this.file, exception);
        }
    }

    /**
     * On-disk shape. Gson needs mutable fields and a no-argument constructor.
     */
    private static final class Stored {
        String key;
        String factionId;
        double amount;
        long at;
        String state;
        String detail;

        static Stored from(Entry entry) {
            Stored stored = new Stored();
            stored.key = entry.key();
            stored.factionId = entry.factionId();
            stored.amount = entry.amount();
            stored.at = entry.at();
            stored.state = entry.state().name();
            stored.detail = entry.detail();
            return stored;
        }

        Entry toEntry() {
            if (this.key == null || this.key.isBlank() || this.factionId == null
                    || this.factionId.isBlank() || this.state == null) {
                return null;
            }
            State parsed;
            try {
                parsed = State.valueOf(this.state);
            } catch (IllegalArgumentException invalidState) {
                return null;
            }
            return new Entry(this.key, this.factionId, this.amount, this.at, parsed,
                    this.detail == null ? "" : this.detail);
        }
    }
}
