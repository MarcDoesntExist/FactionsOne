package com.massivecraft.factions.gui;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.struct.FPerm;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Member list with one player head per faction member.
 *
 * @author MarcDoesntExists
 */
public final class MembersMenu extends PagedMenu {

    private final Main plugin;
    private final Faction faction;

    public MembersMenu(Player player, GuiConfig config) {
        this(player, config, factionOf(player));
    }

    private MembersMenu(Player player, GuiConfig config, Faction faction) {
        super(player, config, config.title("menu.members.title", "<aqua>Members"),
                config.rows("menu.members.rows", 6),
                clicker -> {
                    FPlayer fplayer = FPlayers.i.get(clicker);
                    return fplayer != null && fplayer.getFaction() == faction
                            && FPerm.MENU_MEMBERS.has(clicker, faction, false);
                });
        this.plugin = Main.main;
        this.faction = faction;
        this.build();
    }

    private static Faction factionOf(Player player) {
        FPlayer fplayer = FPlayers.i.get(player);
        return fplayer == null ? null : fplayer.getFaction();
    }

    private void build() {
        List<FPlayer> members = new ArrayList<FPlayer>(this.faction.getFPlayers());
        members.sort(Comparator.comparing((FPlayer member) -> member.getRole().ordinal())
                .thenComparing(FPlayer::getName, String.CASE_INSENSITIVE_ORDER));

        List<MenuItem> entries = new ArrayList<MenuItem>();
        for (FPlayer member : members) {
            entries.add(new MenuItem(this.head(member), clicker -> {
                if (FPerm.MENU_MEMBERS.has(clicker, this.faction, false)) {
                    new MemberActionsMenu(clicker, this.config, member).open();
                }
            }));
        }
        this.setEntries(entries);
    }

    private ItemStack head(FPlayer member) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        head.editMeta(SkullMeta.class, meta -> {
            UUID uuid = this.uuid(member.getId());
            String name = member.getName();
            PlayerProfile profile = uuid != null
                    ? Bukkit.createProfile(uuid, name)
                    : Bukkit.createProfile(name == null || name.isBlank()
                    ? UUID.randomUUID().toString() : name);
            meta.setPlayerProfile(profile);
            meta.displayName(this.plugin.txt.parse(
                    this.config.text("menu.members.entry-name", "<white>%s"), name));

            List<String> configured = this.config.list("menu.members.entry-lore");
            String[] values = {
                    member.getRole().toString(),
                    member.getPowerRounded() + "/" + member.getPowerMaxRounded(),
                    member.getPlayer() == null ? "offline" : "online"
            };
            List<Component> lines = new ArrayList<Component>();
            for (int i = 0; i < configured.size(); i++) {
                lines.add(this.plugin.txt.parse(configured.get(i), i < values.length ? values[i] : ""));
            }
            meta.lore(lines);
        });
        return head;
    }

    private UUID uuid(String id) {
        try {
            return id == null ? null : UUID.fromString(id);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
