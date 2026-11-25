package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.shield.ShieldService;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Rel;
import com.massivecraft.factions.zcore.Lang;
import org.bukkit.entity.Player;

/**
 * Sets a shield window for a faction. Players may only shield their own
 * faction and must be its leader; the console may target any faction.
 *
 * @author MarcDoesntExists
 */
public class CmdShieldSet extends FCommand {

    public CmdShieldSet() {
        super();
        this.aliases.add("set");
        this.requiredArgs.add("hours");
        this.optionalArgs.put("faction", "yours");

        this.permission = Permission.SHIELD_SET.node;
        this.disableOnLock = false;

        senderMustBePlayer = false;
        senderMustBeMember = false;
        senderMustBeOfficer = false;
        senderMustBeLeader = false;
    }

    @Override
    public void perform() {
        if (!Conf.shieldEnabled) {
            msg(Lang.shieldDisabled);
            return;
        }

        ShieldService shields = Main.main.shieldService;
        if (shields == null) {
            msg(Lang.shieldDisabled);
            return;
        }

        int max = Math.max(1, Conf.shieldMaxHours);
        Integer hours = this.argAsInt(0);
        if (hours == null || hours < 1 || hours > max) {
            msg(Lang.shieldInvalidHours, max);
            return;
        }

        Faction faction = this.argIsSet(1) ? this.argAsFaction(1) : this.myFaction;
        if (faction == null || !faction.isNormal()) {
            msg(Lang.shieldNoFaction);
            return;
        }

        if (this.sender instanceof Player) {
            if (faction != this.myFaction) {
                msg(Lang.shieldOwnOnly);
                return;
            }
            if (this.fme == null || this.fme.getRole() != Rel.LEADER) {
                msg(Lang.shieldLeaderOnly);
                return;
            }
        }

        int applied = shields.setShield(faction, hours);
        faction.addLog(actorName(), "SHIELD_SET", applied + "h");
        msg(Lang.shieldSet, faction.getTagPlain(), applied);
    }
}
