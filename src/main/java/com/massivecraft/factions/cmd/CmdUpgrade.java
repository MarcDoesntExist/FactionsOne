package com.massivecraft.factions.cmd;

import com.massivecraft.factions.FeatureConfig;
import com.massivecraft.factions.FeatureService;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.struct.FPerm;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.Lang;

/**
 * Lists and buys faction upgrades with faction points.
 *
 * @author MarcDoesntExists
 */
public final class CmdUpgrade extends FCommand {

    public CmdUpgrade() {
        super();
        this.aliases.add("upgrade");
        this.aliases.add("upgrades");
        this.optionalArgs.put("upgrade", "list");
        this.permission = Permission.UPGRADE.node;
        this.disableOnLock = true;
        this.senderMustBePlayer = true;
        this.senderMustBeMember = true;
    }

    @Override
    public void perform() {
        FeatureService service = Main.main.featureService;
        if (!FPerm.UPGRADE.has(this.fme, this.myFaction, true)) {
            return;
        }
        if (!this.argIsSet(0)) {
            msg(Lang.upgradeHeader, this.myFaction.getFactionPoints());
            for (FeatureConfig.UpgradeDefinition definition : service.getConfig().upgrades()) {
                int level = this.myFaction.getUpgradeLevel(definition.id());
                msg(Lang.upgradeLine, definition.name(), level, definition.maxLevel(),
                    service.upgradeCost(this.myFaction, definition));
            }
            return;
        }
        FeatureConfig.UpgradeDefinition definition = service.getConfig().upgrade(this.argAsString(0));
        if (definition == null) {
            msg(Lang.upgradeNotFound);
            return;
        }
        FeatureService.Result result = service.buyUpgrade(this.fme, this.myFaction, definition.id());
        if (result == FeatureService.Result.SUCCESS) {
            msg(Lang.upgradeBought, definition.name());
        } else {
            msg(FeatureService.message(result));
        }
    }
}
