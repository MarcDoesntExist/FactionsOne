package com.massivecraft.factions;

import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.struct.FFlag;

/**
 * Applies shop perks after the point and inventory checks. Returns false
 * without changing permanent state when the offer is invalid.
 *
 * @author MarcDoesntExists
 */
public final class ShopPerkService {

    public boolean apply(Faction faction, FeatureConfig.ShopOffer offer) {
        if (faction == null || offer == null
            || offer.type() != FeatureConfig.ShopOfferType.PERK
            || offer.perk() == null) {
            return false;
        }
        long until = System.currentTimeMillis()
            + offer.durationMinutes() * 60_000L;
        return switch (offer.perk()) {
            case POWER_BOOST -> {
                if (offer.perkAmount() <= 0.0 || offer.durationMinutes() <= 0) {
                    yield false;
                }
                faction.getPerks().setPower(offer.perkAmount(), until);
                yield true;
            }
            case MONEY -> offer.perkAmount() > 0.0
                && Econ.modifyMoney(faction, offer.perkAmount(), null, "shop perk");
            case POINTS -> {
                if (offer.perkAmount() <= 0.0) {
                    yield false;
                }
                faction.addFactionPoints((int) Math.floor(offer.perkAmount()));
                yield true;
            }
            case TEMP_FLAG -> {
                FFlag flag = FFlag.parse(offer.perkFlag());
                if (flag == null || offer.durationMinutes() <= 0) {
                    yield false;
                }
                faction.getPerks().setFlag(flag, until);
                yield true;
            }
        };
    }
}
