package com.massivecraft.factions.bank;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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

    public enum State {
        PENDING, COMMITTED, ABORTED, COMPENSATION_PENDING, COMPENSATED
    }

    public record Entry(String id, long at, State state, String operation, String from,
                        String to, double amount, String actor, String compensationAccount,
                        double compensationAmount, String detail) {
    }

    private final Path file;
    private final Logger logger;
    private final Gson gson = new GsonBuilder().create();
    private final Map<String, Entry> latest = new LinkedHashMap<>();

    public BankJournal(Path file, Logger logger) {
        this.file = file;
        this.logger = logger;
        load();
    }

    public synchronized String begin(String operation, String from, String to, double amount,
                                     String actor, String compensationAccount,
                                     double compensationAmount, String detail) {
        String id = UUID.randomUUID().toString();
        Entry entry = new Entry(id, System.currentTimeMillis(), State.PENDING, operation, from, to,
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

    public Path file() {
        return this.file;
    }

    private boolean transition(String id, State state, String detail) {
        Entry current = this.latest.get(id);
        if (current == null) {
            return false;
        }
        return append(new Entry(current.id(), System.currentTimeMillis(), state, current.operation(),
                current.from(), current.to(), current.amount(), current.actor(),
                current.compensationAccount(), current.compensationAmount(), detail));
    }

    private boolean append(Entry entry) {
        try {
            Path parent = this.file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            String line = this.gson.toJson(Stored.from(entry)) + "\n";
            Files.writeString(this.file, line, StandardCharsets.UTF_8,
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
        try {
            for (String line : Files.readAllLines(this.file, StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                try {
                    Stored stored = this.gson.fromJson(line, Stored.class);
                    Entry entry = stored == null ? null : stored.toEntry();
                    if (entry != null) {
                        this.latest.put(entry.id(), entry);
                    }
                } catch (RuntimeException malformed) {
                    this.logger.log(Level.WARNING,
                            "Ignoring a malformed bank journal line in " + this.file, malformed);
                }
            }
        } catch (IOException exception) {
            this.logger.log(Level.SEVERE, "Could not read bank journal " + this.file, exception);
        }
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
