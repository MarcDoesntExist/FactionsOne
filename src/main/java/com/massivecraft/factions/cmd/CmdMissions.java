package com.massivecraft.factions.cmd;

import com.massivecraft.factions.FeatureConfig;
import com.massivecraft.factions.FeatureService;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.struct.FPerm;
import com.massivecraft.factions.struct.Permission;

/**
 * Lists and claims faction missions.
 *
 * @author MarcDoesntExists
 */
public final class CmdMissions extends FCommand {

    public CmdMissions() {
        super();
        this.aliases.add("missions");
        this.aliases.add("mission");
        this.optionalArgs.put("mission", "list");
        this.permission = Permission.MISSIONS.node;
        this.disableOnLock = true;
        this.senderMustBePlayer = true;
        this.senderMustBeMember = true;
    }

    @Override
    public void perform() {
        if (!FPerm.MISSIONS.has(this.fme, this.myFaction, true)) {
            return;
        }
        FeatureService service = Main.main.featureService;
        if (!this.argIsSet(0)) {
            msg("<gold>Faction missions <gray>(points: <white>%s<gray>)",
                    this.myFaction.getFactionPoints());
            for (FeatureConfig.MissionDefinition mission : service.getConfig().missions()) {
                msg("<aqua>%s <gray>%s/%s%s", mission.name(),
                        this.myFaction.getMissionProgress(mission.id()), mission.target(),
                        this.myFaction.isMissionCompleted(mission.id()) ? " (done)" : "");
            }
            return;
        }
        FeatureConfig.MissionDefinition mission = service.getConfig().mission(this.argAsString(0));
        if (mission == null) {
            msg("<b>That mission does not exist.");
            return;
        }
        FeatureService.Result result = service.claimMission(this.fme, this.myFaction, mission.id());
        if (result == FeatureService.Result.SUCCESS) {
            msg("<g>Mission completed: <h>%s<g> (+%s points).",
                    mission.name(), mission.rewardPoints());
        } else {
            msg(FeatureService.message(result));
        }
    }
}
