package com.massivecraft.factions;

import com.massivecraft.factions.listeners.FactionsBlockListener;
import com.massivecraft.factions.struct.FPerm;
import com.massivecraft.factions.zcore.Lang;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Moves TNT between a player inventory and the faction bank without duplicating
 * items. Capacity includes the configured TNT upgrade.
 *
 * @author MarcDoesntExists
 */
public final class TntBankService {

    private static final int MAX_RADIUS = 10;
    private final FeatureService features;

    public TntBankService(FeatureService features) {
        this.features = features;
    }

    public static String message(Result result) {
        return switch (result) {
            case SUCCESS -> Lang.featureSuccess;
            case NO_PERMISSION -> Lang.featureNoPermission;
            case INVALID_AMOUNT -> "<b>Enter a positive TNT amount.";
            case NOT_ENOUGH_ITEMS -> "<b>There is not enough TNT.";
            case FULL -> "<b>The faction TNT bank is full.";
            case NO_SPACE -> Lang.featureNoSpace;
            case NO_CONTAINER -> Lang.tntNoContainer;
            case NO_PLAYER -> Lang.featureNoPlayer;
            case DISABLED -> "<b>The TNT bank is disabled.";
        };
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

    /**
     * Takes TNT from the faction bank and fills dispensers around the player.
     * The bank is decremented only by the amount the inventories actually
     * accepted, never by the requested amount.
     */
    public Result fill(FPlayer fplayer, int radius, int requested) {
        if (!Conf.tntBankEnabled) {
            return Result.DISABLED;
        }
        Faction faction = fplayer.getFaction();
        Player player = fplayer.getPlayer();
        if (faction == null || player == null) {
            return Result.NO_PLAYER;
        }
        if (!FPerm.TNT_WITHDRAW.has(fplayer, faction, false)) {
            return Result.NO_PERMISSION;
        }
        if (requested <= 0 || radius < 1 || radius > MAX_RADIUS) {
            return Result.INVALID_AMOUNT;
        }
        int available = Math.min(requested, faction.getTntBank());
        if (available <= 0) {
            return Result.NOT_ENOUGH_ITEMS;
        }

        int remaining = available;
        int moved = 0;
        for (Block block : this.containers(player, radius, Material.DISPENSER)) {
            if (remaining <= 0) {
                break;
            }
            if (!FactionsBlockListener.playerCanBuildDestroyBlock(player, block, "fill TNT", true)) {
                continue;
            }
            Dispenser dispenser = (Dispenser) block.getState();
            Inventory inventory = dispenser.getInventory();
            Map<Integer, ItemStack> leftover = inventory.addItem(new ItemStack(Material.TNT, remaining));
            int rejected = leftover.values().stream().mapToInt(ItemStack::getAmount).sum();
            int added = remaining - rejected;
            if (added > 0) {
                moved += added;
                remaining -= added;
                Main.main.getLogger().fine("TNT fill added " + added + " to "
                    + block.getLocation() + " for " + fplayer.getName());
            }
        }

        if (moved == 0) {
            return Result.NO_SPACE;
        }
        faction.setTntBank(faction.getTntBank() - moved);
        faction.addLog(fplayer.getName(), "TNT_FILL", String.valueOf(moved));
        return Result.SUCCESS;
    }

    /**
     * Removes TNT from dispensers around the player and credits only the
     * stacks actually removed, capped by the bank capacity.
     */
    public Result siphon(FPlayer fplayer, int radius) {
        if (!Conf.tntBankEnabled) {
            return Result.DISABLED;
        }
        Faction faction = fplayer.getFaction();
        Player player = fplayer.getPlayer();
        if (faction == null || player == null) {
            return Result.NO_PLAYER;
        }
        if (!FPerm.TNT_DEPOSIT.has(fplayer, faction, false)) {
            return Result.NO_PERMISSION;
        }
        if (radius < 1 || radius > MAX_RADIUS) {
            return Result.INVALID_AMOUNT;
        }
        int room = Math.max(0, this.features.tntLimit(faction) - faction.getTntBank());
        if (room <= 0) {
            return Result.FULL;
        }

        int remaining = room;
        int moved = 0;
        for (Block block : this.containers(player, radius, Material.DISPENSER)) {
            if (remaining <= 0) {
                break;
            }
            if (!FactionsBlockListener.playerCanBuildDestroyBlock(player, block, "siphon TNT", true)) {
                continue;
            }
            Dispenser dispenser = (Dispenser) block.getState();
            Inventory inventory = dispenser.getInventory();
            Map<Integer, ItemStack> leftover = inventory.removeItem(new ItemStack(Material.TNT, remaining));
            int rejected = leftover.values().stream().mapToInt(ItemStack::getAmount).sum();
            int removed = remaining - rejected;
            if (removed > 0) {
                moved += removed;
                remaining -= removed;
                Main.main.getLogger().fine("TNT siphon removed " + removed + " from "
                    + block.getLocation() + " for " + fplayer.getName());
            }
        }

        if (moved == 0) {
            return Result.NOT_ENOUGH_ITEMS;
        }
        faction.setTntBank(faction.getTntBank() + moved);
        faction.addLog(fplayer.getName(), "TNT_SIPHON", String.valueOf(moved));
        return Result.SUCCESS;
    }

    private Iterable<Block> containers(Player player, int radius, Material material) {
        java.util.List<Block> blocks = new java.util.ArrayList<Block>();
        org.bukkit.Location center = player.getLocation();
        org.bukkit.World world = center.getWorld();
        if (world == null) {
            return blocks;
        }
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int y = Math.max(world.getMinHeight(), cy - radius);
                 y <= Math.min(world.getMaxHeight() - 1, cy + radius); y++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == material) {
                        blocks.add(block);
                    }
                }
            }
        }
        return blocks;
    }

    public enum Result {
        SUCCESS,
        NO_PERMISSION,
        INVALID_AMOUNT,
        NOT_ENOUGH_ITEMS,
        FULL,
        NO_SPACE,
        NO_CONTAINER,
        NO_PLAYER,
        DISABLED
    }
}
