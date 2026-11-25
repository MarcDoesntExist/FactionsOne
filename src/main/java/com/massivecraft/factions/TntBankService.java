package com.massivecraft.factions;

import com.massivecraft.factions.struct.FPerm;
import com.massivecraft.factions.zcore.Lang;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Moves TNT between a player inventory and the faction bank without duplicating
 * items. Capacity includes the configured TNT upgrade.
 *
 * @author MarcDoesntExists
 */
public final class TntBankService {

    public enum Result {
        SUCCESS,
        NO_PERMISSION,
        INVALID_AMOUNT,
        NOT_ENOUGH_ITEMS,
        FULL,
        NO_SPACE,
        DISABLED
    }

    private final FeatureService features;

    public TntBankService(FeatureService features) {
        this.features = features;
    }

    public Result deposit(FPlayer fplayer, int requested) {
        if (!Conf.tntBankEnabled) {
            return Result.DISABLED;
        }
        if (!FPerm.TNT_DEPOSIT.has(fplayer, fplayer.getFaction(), false)) {
            return Result.NO_PERMISSION;
        }
        if (requested <= 0) {
            return Result.INVALID_AMOUNT;
        }
        Player player = fplayer.getPlayer();
        Faction faction = fplayer.getFaction();
        int capacity = this.features.tntLimit(faction);
        int room = Math.max(0, capacity - faction.getTntBank());
        if (room == 0) {
            return Result.FULL;
        }

        int amount = Math.min(requested, room);
        Map<Integer, ItemStack> leftover = player.getInventory()
                .removeItem(new ItemStack(Material.TNT, amount));
        int removed = amount - leftover.values().stream().mapToInt(ItemStack::getAmount).sum();
        if (removed <= 0) {
            return Result.NOT_ENOUGH_ITEMS;
        }

        faction.setTntBank(faction.getTntBank() + removed);
        faction.addLog(fplayer.getName(), "TNT_DEPOSIT", String.valueOf(removed));
        this.features.progress(faction, FeatureConfig.MissionType.TNT_DEPOSIT, removed);
        return Result.SUCCESS;
    }

    public Result withdraw(FPlayer fplayer, int amount) {
        if (!Conf.tntBankEnabled) {
            return Result.DISABLED;
        }
        if (!FPerm.TNT_WITHDRAW.has(fplayer, fplayer.getFaction(), false)) {
            return Result.NO_PERMISSION;
        }
        if (amount <= 0) {
            return Result.INVALID_AMOUNT;
        }
        Faction faction = fplayer.getFaction();
        if (faction.getTntBank() < amount) {
            return Result.NOT_ENOUGH_ITEMS;
        }

        Player player = fplayer.getPlayer();
        faction.setTntBank(faction.getTntBank() - amount);
        Map<Integer, ItemStack> leftover = player.getInventory()
                .addItem(new ItemStack(Material.TNT, amount));
        int rejected = leftover.values().stream().mapToInt(ItemStack::getAmount).sum();
        if (rejected > 0) {
            faction.setTntBank(faction.getTntBank() + rejected);
            if (rejected == amount) {
                return Result.NO_SPACE;
            }
        }
        faction.addLog(fplayer.getName(), "TNT_WITHDRAW", String.valueOf(amount - rejected));
        return Result.SUCCESS;
    }

    public static String message(Result result) {
        return switch (result) {
            case SUCCESS -> Lang.featureSuccess;
            case NO_PERMISSION -> Lang.featureNoPermission;
            case INVALID_AMOUNT -> "<b>Enter a positive TNT amount.";
            case NOT_ENOUGH_ITEMS -> "<b>There is not enough TNT.";
            case FULL -> "<b>The faction TNT bank is full.";
            case NO_SPACE -> Lang.featureNoSpace;
            case DISABLED -> "<b>The TNT bank is disabled.";
        };
    }
}
