package com.massivecraft.factions;

import com.massivecraft.factions.struct.FFlag;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Temporary faction perks bought from the shop. Entries are persisted but
 * ignored once their expiry passes; nothing here is a permanent upgrade.
 *
 * @author MarcDoesntExists
 */
public final class FactionPerks {

    private Map<String, Double> amounts = new LinkedHashMap<String, Double>();
    private Map<String, Long> expiries = new LinkedHashMap<String, Long>();

    public void setPower(double amount, long until) {
        this.set("power", amount, until);
    }

    public void setFlag(FFlag flag, long until) {
        if (flag != null) {
            this.set(flag.name(), 1.0, until);
        }
    }

    public double power(long now) {
        return this.active("power", now);
    }

    public boolean flag(FFlag flag, long now) {
        return flag != null && this.active(flag.name(), now) > 0.0;
    }

    public void sanitize(long now) {
        if (this.amounts == null) {
            this.amounts = new LinkedHashMap<String, Double>();
        }
        if (this.expiries == null) {
            this.expiries = new LinkedHashMap<String, Long>();
        }
        this.expiries.entrySet().removeIf(entry -> entry.getKey() == null
                || entry.getValue() == null || entry.getValue() <= now);
        this.amounts.keySet().removeIf(key -> key == null || !this.expiries.containsKey(key));
    }

    private void set(String key, double amount, long until) {
        if (key == null || amount <= 0.0) {
            return;
        }
        if (this.amounts == null) {
            this.amounts = new LinkedHashMap<String, Double>();
        }
        if (this.expiries == null) {
            this.expiries = new LinkedHashMap<String, Long>();
        }
        this.amounts.put(key, amount);
        this.expiries.put(key, until);
    }

    private double active(String key, long now) {
        if (this.amounts == null || this.expiries == null) {
            return 0.0;
        }
        Long expiry = this.expiries.get(key);
        if (expiry == null || expiry <= now) {
            this.expiries.remove(key);
            this.amounts.remove(key);
            return 0.0;
        }
        Double amount = this.amounts.get(key);
        return amount == null ? 0.0 : Math.max(0.0, amount);
    }
}
