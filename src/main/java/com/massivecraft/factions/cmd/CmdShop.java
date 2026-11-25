package com.massivecraft.factions.cmd;

import com.massivecraft.factions.FeatureConfig;
import com.massivecraft.factions.FeatureService;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.struct.FPerm;
import com.massivecraft.factions.struct.Permission;

/**
 * Lists and buys faction shop offers with faction points.
 *
 * @author MarcDoesntExists
 */
public final class CmdShop extends FCommand {

    public CmdShop() {
        super();
        this.aliases.add("shop");
        this.optionalArgs.put("offer", "list");
        this.permission = Permission.SHOP.node;
        this.disableOnLock = true;
        this.senderMustBePlayer = true;
        this.senderMustBeMember = true;
    }

    @Override
    public void perform() {
        FeatureService service = Main.main.featureService;
        if (!FPerm.SHOP.has(this.fme, this.myFaction, true)) {
            return;
        }
        if (!this.argIsSet(0)) {
            msg("<gold>Faction shop <gray>(points: <white>%s<gray>)",
                    this.myFaction.getFactionPoints());
            for (FeatureConfig.ShopOffer offer : service.getConfig().shopOffers()) {
                msg("<aqua>%s <gray>- %s x%s for %s points",
                        offer.id(), offer.material(), offer.amount(), offer.cost());
            }
            return;
        }
        FeatureConfig.ShopOffer offer = service.getConfig().shopOffer(this.argAsString(0));
        if (offer == null) {
            msg("<b>That shop offer does not exist.");
            return;
        }
        FeatureService.Result result = service.buyShopOffer(this.fme, this.myFaction, offer.id());
        if (result == FeatureService.Result.SUCCESS) {
            msg("<g>Bought <h>%s<g>.", offer.name());
        } else {
            msg(FeatureService.message(result));
        }
    }
}
