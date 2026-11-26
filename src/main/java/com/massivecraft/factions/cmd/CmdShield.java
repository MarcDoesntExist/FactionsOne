package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.shield.ShieldService;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.Lang;
import org.bukkit.entity.Player;

/**
 * Shows the shield/grace state of a faction.
 *
 * @author MarcDoesntExists
 */
public class CmdShield extends FCommand {
    public final CmdShieldSet cmdShieldSet = new CmdShieldSet();

    public CmdShield() {
        super();
        this.aliases.add("shield");
        this.optionalArgs.put("faction", "yours");

        this.permission = Permission.SHIELD.node;
        this.disableOnLock = false;

        senderMustBePlayer = false;
        senderMustBeMember = false;
        senderMustBeOfficer = false;
        senderMustBeLeader = false;

        this.setHelpShort("faction shield status");
        this.helpLong.add(p.txt.parseTags("<i>Shows the shield or grace window protecting a faction."));

        this.addSubCommand(this.cmdShieldSet);
    }

    @Override
    public void perform() {
        if (!Conf.shieldEnabled) {
            msg(Lang.shieldDisabled);
            return;
        }

        Faction faction = this.argIsSet(0) ? this.argAsFaction(0) : this.myFaction;
        if (faction == null || !faction.isNormal()) {
            msg(Lang.shieldNoFaction);
            return;
        }
        if (this.sender instanceof Player && faction != this.myFaction) {
            msg(Lang.shieldOwnOnly);
            return;
        }

        ShieldService shields = Main.main.shieldService;
        if (shields == null) {
            msg(Lang.shieldDisabled);
            return;
        }

        String state;
        if (!shields.isProtected(faction)) {
            state = Lang.shieldInactive;
        } else {
            state = String.format(Lang.shieldState,
                shields.isGrace(faction) ? Lang.shieldGrace : Lang.shieldActive,
                shields.remainingMinutes(faction));
        }
        msg(Lang.shieldStatus, faction.getTagPlain(), state);
    }
}
