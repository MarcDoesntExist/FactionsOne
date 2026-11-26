package com.massivecraft.factions.rank;

import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.bank.BankService;
import com.massivecraft.factions.struct.FFlag;
import com.massivecraft.factions.value.ClaimValueService;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Builds a cached, immutable leaderboard from a provider that reads live
 * factions. The cache is invalidated by faction/land/bank changes and expires
 * after the configured TTL.
 *
 * @author MarcDoesntExists
 */
public final class FactionRankingService {

    public record Snapshot(List<FactionRankEntry> entries, long builtAt) {
        public Snapshot {
            entries = List.copyOf(entries);
        }
    }

    private final Clock clock;
    private final long ttlMillis;
    private final Supplier<List<FactionRankEntry>> provider;
    private volatile Snapshot snapshot;

    public FactionRankingService(Clock clock, int cacheSeconds,
                                 Supplier<List<FactionRankEntry>> provider) {
        this.clock = clock;
        this.ttlMillis = Math.max(0, cacheSeconds) * 1000L;
        this.provider = provider;
    }

    public Snapshot snapshot() {
        Snapshot current = this.snapshot;
        long now = this.clock.millis();
        if (current != null && now - current.builtAt() < this.ttlMillis) {
            return current;
        }
        synchronized (this) {
            current = this.snapshot;
            if (current != null && now - current.builtAt() < this.ttlMillis) {
                return current;
            }
            List<FactionRankEntry> entries = this.provider.get();
            current = new Snapshot(entries == null ? List.of() : entries, now);
            this.snapshot = current;
            return current;
        }
    }

    public List<FactionRankEntry> top(FactionRankEntry.Metric metric, int limit) {
        int clamped = Math.max(1, Math.min(50, limit));
        List<FactionRankEntry> sorted = new ArrayList<>(this.snapshot().entries());
        sorted.sort(FactionRankEntry.order(metric));
        return List.copyOf(sorted.subList(0, Math.min(clamped, sorted.size())));
    }

    public void invalidate() {
        this.snapshot = null;
    }

    /** Live provider used by the plugin; separated so tests can inject rows. */
    public static List<FactionRankEntry> collectLive() {
        ClaimValueService values = Main.main == null ? null : Main.main.claimValueService;
        List<FactionRankEntry> entries = new ArrayList<>();
        for (Faction faction : Factions.i.get()) {
            if (!faction.isNormal() || faction.getFlag(FFlag.PERMANENT)) {
                continue;
            }
            entries.add(new FactionRankEntry(
                    faction.getId(),
                    faction.getTagPlain(),
                    faction.getPower(),
                    faction.getLandRounded(),
                    faction.getFPlayers().size(),
                    BankService.get().balance(faction),
                    faction.getFactionPoints(),
                    values == null ? 0.0 : values.value(faction.getId())));
        }
        return entries;
    }
}
