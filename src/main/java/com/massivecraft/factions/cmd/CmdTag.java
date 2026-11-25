package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.event.FactionRenameEvent;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.util.MiscUtil;
import org.bukkit.Bukkit;

import java.util.ArrayList;

/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class CmdTag extends FCommand {

    public CmdTag() {
        this.aliases.add("tag");

        this.requiredArgs.add("new tag");
        //this.optionalArgs.put("", "");

        this.permission = Permission.TAG.node;
        this.disableOnLock = true;

        senderMustBePlayer = true;
        senderMustBeMember = false;
        senderMustBeOfficer = true;
        senderMustBeLeader = false;
    }

    @Override
    public void perform() {
        String tag = this.argAsString(0);

        // TODO does not first test cover selfcase?
        if (Factions.i.isTagTaken(tag) && !MiscUtil.getComparisonString(tag)
                .equals(myFaction.getComparisonTag())) {
            msg("<b>That tag is already taken");
            return;
        }

        ArrayList<net.kyori.adventure.text.Component> errors = new ArrayList<>();
        errors.addAll(Factions.validateTag(tag));
        if (errors.size() > 0) {
            sendMessage(errors);
            return;
        }

        // if economy is enabled, they're not on the bypass list, and this command has a cost set, make sure they can pay
        if (!canAffordCommand(Conf.econCostTag, "to change the faction tag"))
            return;

        // trigger the faction rename event (cancellable)
        FactionRenameEvent renameEvent = new FactionRenameEvent(fme, tag);
        Bukkit.getServer().getPluginManager().callEvent(renameEvent);
        if (renameEvent.isCancelled())
            return;

        // then make 'em pay (if applicable)
        if (!payForCommand(Conf.econCostTag, "to change the faction tag",
                "for changing the faction tag"))
            return;

        String oldtag = myFaction.getTagPlain();
        myFaction.setTag(tag);

        // Inform
        myFaction.msg("%s<i> changed your faction tag to %s", fme.describeTo(myFaction, true),
                myFaction.getTag(myFaction));
        for (Faction faction : Factions.i.get()) {
            if (faction == myFaction) {
                continue;
            }
            faction.msg("<i>The faction %s<i> changed their name to %s.",
                    net.kyori.adventure.text.Component.text(oldtag,
                            net.kyori.adventure.text.format.Style.style(
                                    fme.getColorTo(faction).toTextColor())),
                    myFaction.getTag(faction));
        }
    }

}
