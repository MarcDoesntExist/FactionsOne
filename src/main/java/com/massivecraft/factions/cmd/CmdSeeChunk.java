package com.massivecraft.factions.cmd;

import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.util.VisualizeUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class CmdSeeChunk extends FCommand {
    /**
     * How far above and below the player the corner pillars are drawn. Only the
     * part of the column the player can actually see is worth sending.
     */
    private static final int VERTICAL_RANGE = 32;

    public CmdSeeChunk() {
        super();
        this.aliases.add("sc");
        this.aliases.add("seechunk");

        this.permission = Permission.SEE_CHUNK.node;
        this.disableOnLock = false;

        senderMustBePlayer = true;
        senderMustBeMember = false;
        senderMustBeOfficer = false;
        senderMustBeLeader = false;
    }

    @Override
    public void perform() {
        Location meLoc = me.getLocation();
        // Which chunk are we standing in ATM?
        // This bit shifting is something like divide by 16 :Main
        int chunkX = meLoc.getBlockX() >> 4;
        int chunkZ = meLoc.getBlockZ() >> 4;

        World world = me.getWorld();
        int centerY = meLoc.getBlockY();
        int minY = Math.max(world.getMinHeight(), centerY - VERTICAL_RANGE);
        int maxY = Math.min(world.getMaxHeight() - 1, centerY + VERTICAL_RANGE);

        // Get the pillars for that chunk
        int blockX;
        int blockZ;

        blockX = chunkX * 16;
        blockZ = chunkZ * 16;
        showPillar(me, world, blockX, blockZ, minY, maxY);

        blockX = chunkX * 16 + 15;
        blockZ = chunkZ * 16;
        showPillar(me, world, blockX, blockZ, minY, maxY);

        blockX = chunkX * 16;
        blockZ = chunkZ * 16 + 15;
        showPillar(me, world, blockX, blockZ, minY, maxY);

        blockX = chunkX * 16 + 15;
        blockZ = chunkZ * 16 + 15;
        showPillar(me, world, blockX, blockZ, minY, maxY);

        // VisualizeUtil sends the batch automatically at the end of the tick.
    }

    public void showPillar(Player player, World world, int blockX, int blockZ, int minY, int maxY) {
        for (int blockY = minY; blockY <= maxY; blockY++) {
            Location loc = new Location(world, blockX, blockY, blockZ);
            if (loc.getBlock().getType() != Material.AIR)
                continue;
            Material type = blockY % 5 == 0 ? Material.GLOWSTONE : Material.GLASS;
            VisualizeUtil.addLocation(player, loc, type);
        }
    }

}
