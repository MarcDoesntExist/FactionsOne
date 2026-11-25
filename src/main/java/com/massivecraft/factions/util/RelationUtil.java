package com.massivecraft.factions.util;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.iface.RelationParticipator;
import com.massivecraft.factions.struct.FFlag;
import com.massivecraft.factions.struct.Rel;
import com.massivecraft.factions.zcore.util.TextUtil;
import net.kyori.adventure.text.Component;

/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class RelationUtil {
    public static Component describeThatToMe(RelationParticipator that, RelationParticipator me,
                                             boolean ucfirst) {
        if (that == null) {
            return Component.text("A server admin");
        }

        Faction thatFaction = getFaction(that);
        if (thatFaction == null)
            return Component.text("ERROR"); // ERROR

        Faction myFaction = getFaction(me);
        String ret = "";

        if (that instanceof Faction) {
            if (me instanceof FPlayer && myFaction == thatFaction) {
                ret = "your faction";
            } else {
                ret = thatFaction.getTagPlain();
            }
        } else if (that instanceof FPlayer) {
            FPlayer fplayerthat = (FPlayer) that;
            if (that == me) {
                ret = "you";
            } else if (thatFaction == myFaction) {
                ret = TextUtil.plain(fplayerthat.getNameAndTitle());
            } else {
                ret = TextUtil.plain(fplayerthat.getNameAndTag());
            }
        }

        if (ucfirst) {
            ret = TextUtil.upperCaseFirst(ret);
        }

        return Component.text(ret).colorIfAbsent(getColorOfThatToMe(that, me).toTextColor());
    }

    public static Component describeThatToMe(RelationParticipator that, RelationParticipator me) {
        return describeThatToMe(that, me, false);
    }

    public static Rel getRelationOfThatToMe(RelationParticipator that, RelationParticipator me) {
        return getRelationOfThatToMe(that, me, false);
    }

    public static Rel getRelationOfThatToMe(RelationParticipator that, RelationParticipator me,
                                            boolean ignorePeaceful) {
        Rel ret = null;

        Faction myFaction = getFaction(me);
        if (myFaction == null)
            return Rel.NEUTRAL; // ERROR

        Faction thatFaction = getFaction(that);
        if (thatFaction == null)
            return Rel.NEUTRAL; // ERROR

        // The faction with the lowest wish "wins"
        if (thatFaction.getRelationWish(myFaction)
                .isLessThan(myFaction.getRelationWish(thatFaction))) {
            ret = thatFaction.getRelationWish(myFaction);
        } else {
            ret = myFaction.getRelationWish(thatFaction);
        }

        if (myFaction.equals(thatFaction)) {
            ret = Rel.MEMBER;
            // Do officer and leader check
            //Main.main.log("getRelationOfThatToMe the factions are the same for "+that.getClass().getSimpleName()+" and observer "+me.getClass().getSimpleName());
            if (that instanceof FPlayer) {
                ret = ((FPlayer) that).getRole();
                //Main.main.log("getRelationOfThatToMe it was a player and role is "+ret);
            }
        } else if (!ignorePeaceful && (thatFaction.getFlag(FFlag.PEACEFUL) || myFaction
                .getFlag(FFlag.PEACEFUL))) {
            ret = Rel.TRUCE;
        }

        return ret;
    }

    public static Faction getFaction(RelationParticipator rp) {
        if (rp instanceof Faction) {
            return (Faction) rp;
        }

        if (rp instanceof FPlayer) {
            return ((FPlayer) rp).getFaction();
        }

        // ERROR
        return null;
    }

    public static Color getColorOfThatToMe(RelationParticipator that, RelationParticipator me) {
        Faction thatFaction = getFaction(that);
        if (thatFaction != null && thatFaction != getFaction(me)) {
            if (thatFaction.getFlag(FFlag.FRIENDLYFIRE) == true) {
                return Conf.colorFriendlyFire;
            }

            if (thatFaction.getFlag(FFlag.PVP) == false) {
                return Conf.colorNoPVP;
            }
        }
        return getRelationOfThatToMe(that, me).getColor();
    }
}
