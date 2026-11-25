package com.massivecraft.factions.zcore.util;

import org.bukkit.Bukkit;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * Writes files on a single background thread. Saves queued for the same file
 * before the worker gets to them are coalesced: only the latest content is
 * written, so a burst of saves cannot queue up stale copies.
 *
 * <p>Callers serialize their data on the calling thread (a consistent snapshot)
 * and only the disk I/O runs asynchronously.</p>
 *
 * @author MarcDoesntExist
 */
public final class SaveQueue {

    private final Map<File, String> pending = new ConcurrentHashMap<>();
    private final AtomicBoolean draining = new AtomicBoolean();
    private final AtomicInteger inFlight = new AtomicInteger();
    private volatile ExecutorService executor;

    private ExecutorService executor() {
        ExecutorService current = this.executor;
        if (current == null || current.isShutdown()) {
            synchronized (this) {
                current = this.executor;
                if (current == null || current.isShutdown()) {
                    current = Executors.newSingleThreadExecutor(runnable -> {
                        Thread thread = new Thread(runnable, "FactionsOne-Save");
                        thread.setDaemon(true);
                        return thread;
                    });
                    this.executor = current;
                }
            }
        }
        return current;
    }

    public void submit(File file, String content) {
        this.pending.put(file.getAbsoluteFile(), content);
        this.schedule();
    }

    private void schedule() {
        if (this.draining.compareAndSet(false, true)) {
            this.inFlight.incrementAndGet();
            try {
                this.executor().execute(this::drain);
            } catch (RuntimeException ex) {
                this.inFlight.decrementAndGet();
                this.draining.set(false);
                throw ex;
            }
        }
    }

    private void drain() {
        try {
            while (true) {
                Iterator<Map.Entry<File, String>> iterator = this.pending.entrySet().iterator();
                if (!iterator.hasNext()) {
                    break;
                }
                Map.Entry<File, String> next = iterator.next();
                iterator.remove();
                try {
                    DiscUtil.write(next.getKey(), next.getValue());
                } catch (Exception ex) {
                    Bukkit.getLogger().log(Level.WARNING,
                            "[Factions] Could not save \"" + next.getKey() + "\": " + ex.getMessage());
                }
            }
        } finally {
            this.inFlight.decrementAndGet();
            this.draining.set(false);
            if (!this.pending.isEmpty()) {
                this.schedule();
            }
        }
    }

    /**
     * Waits until everything queued so far has been written, or the timeout expires.
     */
    public void flush(long timeoutMillis) {
        long deadline = System.currentTimeMillis() + Math.max(0L, timeoutMillis);
        while ((!this.pending.isEmpty() || this.inFlight.get() > 0)
                && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(10L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        if (!this.pending.isEmpty() || this.inFlight.get() > 0) {
            Bukkit.getLogger().warning("[Factions] Timed out while waiting for pending saves");
        }
    }

    /**
     * Flushes pending writes and stops the worker. A later submit starts a new one.
     */
    public void shutdown(long timeoutMillis) {
        this.flush(timeoutMillis);
        ExecutorService current = this.executor;
        if (current != null) {
            current.shutdown();
        }
    }
}
