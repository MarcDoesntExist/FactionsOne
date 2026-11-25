package com.massivecraft.factions.gui;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FeatureConfig;
import com.massivecraft.factions.FeatureService;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.struct.FPerm;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists missions and claims completed ones for faction points.
 *
 * @author MarcDoesntExists
 */
public final class MissionsMenu extends PagedMenu {

    private final Faction faction;

    public MissionsMenu(Player player, GuiConfig config) {
        this(player, config, factionOf(player));
    }

    private MissionsMenu(Player player, GuiConfig config, Faction faction) {
        super(player, config, config.title("menu.missions.title", "<aqua>Missions"),
                config.rows("menu.missions.rows", 6),
                clicker -> {
                    FPlayer fplayer = FPlayers.i.get(clicker);
                    return fplayer != null && fplayer.getFaction() == faction
                            && FPerm.MISSIONS.has(clicker, faction, false);
                });
        this.faction = faction;
        this.build();
    }

    private static Faction factionOf(Player player) {
        FPlayer fplayer = FPlayers.i.get(player);
        return fplayer == null ? null : fplayer.getFaction();
    }

    private void build() {
        FeatureService service = Main.main.featureService;
        List<MenuItem> entries = new ArrayList<MenuItem>();
        for (FeatureConfig.MissionDefinition mission : service.getConfig().missions()) {
            ItemStack icon = this.config.item("main.missions", this.getPlayer());
            if (icon.getType() == Material.AIR) {
                icon = new ItemStack(Material.WRITABLE_BOOK);
            }
            boolean done = this.faction.isMissionCompleted(mission.id());
            int progress = this.faction.getMissionProgress(mission.id());
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.displayName(Main.main.txt.parse("<aqua>%s", mission.name()));
                List<Component> lore = new ArrayList<Component>();
                lore.add(Main.main.txt.parse("<gray>%s", mission.description()));
                lore.add(Main.main.txt.parse("<gray>Progress: <white>%s/%s", progress,
                        mission.target()));
                lore.add(Main.main.txt.parse("<gray>Reward: <white>%s points",
                        mission.rewardPoints()));
                lore.add(Main.main.txt.parse(done ? "<g>Completed"
                        : progress >= mission.target() ? "<yellow>Click to claim"
                        : "<gray>Not ready"));
                meta.lore(lore);
                icon.setItemMeta(meta);
            }
            entries.add(new MenuItem(icon, clicker -> {
                FPlayer fplayer = FPlayers.i.get(clicker);
                FeatureService.Result result = service.claimMission(fplayer, this.faction,
                        mission.id());
                if (result == FeatureService.Result.SUCCESS) {
                    clicker.sendMessage(Main.main.txt.parse("<g>Mission reward claimed."));
                    new MissionsMenu(clicker, this.config).open();
                } else {
                    clicker.sendMessage(Main.main.txt.parse(FeatureService.message(result)));
                }
            }));
        }
        this.setEntries(entries);
    }
}
