package com.massivecraft.factions.shield;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.event.ShieldChangeEvent;
import com.massivecraft.factions.struct.FFlag;
import com.massivecraft.factions.struct.Rel;

import java.time.Clock;

/**
 * Applies the configured shield and grace windows to faction territory.
 * Members and allies are never blocked; truces, neutrals and enemies are.
 *
 * @author MarcDoesntExists
 */
public final class ShieldService {

    private final Clock clock;

    public ShieldService(Clock clock) {
        this.clock = clock;
    }

    public boolean isEnabled() {
        return Conf.shieldEnabled;
    }

    public boolean isProtected(Faction faction) {
        if (!Conf.shieldEnabled || faction == null || !faction.isNormal()
                || faction.getFlag(FFlag.PERMANENT)) {
            return false;
        }
        long now = this.clock.millis();
        return ShieldPolicy.blocks(faction.getGraceUntil(), faction.getShieldUntil(), now);
    }

    public boolean isGrace(Faction faction) {
        return Conf.shieldEnabled && faction != null
                && ShieldPolicy.active(faction.getGraceUntil(), this.clock.millis());
    }

    public boolean canAct(Faction territory, FPlayer actor) {
        if (!this.isProtected(territory)) {
            return true;
        }
        if (actor == null) {
            return false;
        }
        Faction actorFaction = actor.getFaction();
        if (actorFaction == null) {
            return false;
        }
        if (actorFaction == territory) {
            return true;
        }
        return territory.getRelationTo(actorFaction).isAtLeast(Rel.ALLY);
    }

    /**
     * Attack/overclaim gate: a shielded faction cannot attack an unprotected
     * target, and an unprotected actor cannot attack a shielded target.
     */
    public boolean canAttack(Faction territory, FPlayer actor) {
        if (actor == null) {
            return !this.isProtected(territory);
        }
        Faction actorFaction = actor.getFaction();
        if (actorFaction == null) {
            return !this.isProtected(territory);
        }
        if (actorFaction == territory) {
            return true;
        }
        if (this.isProtected(actorFaction)) {
            return false;
        }
        return !this.isProtected(territory)
                || territory.getRelationTo(actorFaction).isAtLeast(Rel.ALLY);
    }

    public long remainingMinutes(Faction faction) {
        if (faction == null) {
            return 0;
        }
        long now = this.clock.millis();
        return Math.max(ShieldPolicy.remainingMinutes(faction.getGraceUntil(), now),
                ShieldPolicy.remainingMinutes(faction.getShieldUntil(), now));
    }

    /**
     * Starts or extends a shield from now, clamped to the configured maximum.
     *
     * @return the number of hours actually applied
     */
    public int setShield(Faction faction, int hours) {
        int max = Math.max(1, Conf.shieldMaxHours);
        int applied = Math.max(1, Math.min(hours, max));
        long until = ShieldPolicy.addHours(this.clock.millis(), applied);
        long oldUntil = faction.getShieldUntil();
        ShieldChangeEvent event = new ShieldChangeEvent(faction, oldUntil,
                Math.max(oldUntil, until), applied);
        if (org.bukkit.Bukkit.getServer() != null) {
            org.bukkit.Bukkit.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return 0;
            }
        }
        faction.setShieldUntil(event.getNewUntil());
        return applied;
    }
}
