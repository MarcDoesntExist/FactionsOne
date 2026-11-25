package com.massivecraft.factions;

/**
 * One persisted faction audit-log entry.
 *
 * @author MarcDoesntExists
 */
public final class FactionLogEntry {

    private long timestamp;
    private String actor;
    private String action;
    private String detail;

    public FactionLogEntry() {
        // Gson constructor.
    }

    public FactionLogEntry(long timestamp, String actor, String action, String detail) {
        this.timestamp = timestamp;
        this.actor = actor;
        this.action = action;
        this.detail = detail;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public String getActor() {
        return this.actor;
    }

    public String getAction() {
        return this.action;
    }

    public String getDetail() {
        return this.detail;
    }
}
