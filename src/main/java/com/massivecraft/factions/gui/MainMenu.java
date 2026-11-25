package com.massivecraft.factions.gui;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.struct.FPerm;
import org.bukkit.entity.Player;

/**
 * The /f menu hub. Feature buttons open the same permission-checked menus as
 * their commands; relationships, territory and money remain informational.
 *
 * @author MarcDoesntExists
 */
public final class MainMenu extends Menu {

    private final GuiConfig config;

    public MainMenu(Player player, GuiConfig config) {
        super(player, config.title("menu.main.title", "<gradient:#ffd166:#ef476f>Factions</gradient>"),
                config.rows("menu.main.rows", 5), ignored -> true);
        this.config = config;

        this.setItem(this.slot("info", 4), config.item("main.info", player), this::openInfo);
        this.setItem(this.slot("members", 10), config.item("main.members", player), this::openMembers);
        this.setItem(this.slot("perms", 11), config.item("main.perms", player), this::openPerms);
        this.setItem(this.slot("relations", 12), config.item("main.relations", player), this::openRelations);
        this.setItem(this.slot("territory", 13), config.item("main.territory", player), this::openTerritory);
        this.setItem(this.slot("bank", 14), config.item("main.bank", player), this::openBank);
        this.setItem(this.slot("warps", 15), config.item("main.warps", player), this::openWarps);
        this.setItem(this.slot("upgrades", 16), config.item("main.upgrades", player), this::openUpgrades);
        this.setItem(this.slot("missions", 19), config.item("main.missions", player), this::openMissions);
        this.setItem(this.slot("shop", 20), config.item("main.shop", player), this::openShop);
        this.setItem(this.slot("logs", 21), config.item("main.logs", player), this::openLogs);
        this.setItem(this.slot("close", 22), config.item("main.close", player), Player::closeInventory);
    }

    private int slot(String key, int fallback) {
        return this.config.slot("menu.main.slots." + key, fallback);
    }

    private void openInfo(Player player) {
        new FactionInfoMenu(player, this.config).open();
    }

    private void openMembers(Player player) {
        FPlayer fplayer = FPlayers.i.get(player);
        Faction faction = fplayer == null ? null : fplayer.getFaction();
        if (faction == null || !FPerm.MENU_MEMBERS.has(player, faction, true)) {
            return;
        }
        new MembersMenu(player, this.config).open();
    }

    private void openPerms(Player player) {
        FPlayer fplayer = FPlayers.i.get(player);
        Faction faction = fplayer == null ? null : fplayer.getFaction();
        if (faction == null || !FPerm.MENU_PERMS.has(player, faction, true)) {
            return;
        }
        new PermsMenu(player, this.config).open();
    }

    private void openRelations(Player player) {
        new InfoMenu(player, this.config, "relations").open();
    }

    private void openTerritory(Player player) {
        new InfoMenu(player, this.config, "territory").open();
    }

    private void openBank(Player player) {
        new InfoMenu(player, this.config, "bank").open();
    }

    private void openWarps(Player player) {
        Faction faction = factionOf(player);
        if (faction == null || !FPerm.WARP.has(player, faction, true)) {
            return;
        }
        new WarpsMenu(player, this.config).open();
    }

    private void openUpgrades(Player player) {
        Faction faction = factionOf(player);
        if (faction == null || !FPerm.UPGRADE.has(player, faction, true)) {
            return;
        }
        new UpgradesMenu(player, this.config).open();
    }

    private void openMissions(Player player) {
        Faction faction = factionOf(player);
        if (faction == null || !FPerm.MISSIONS.has(player, faction, true)) {
            return;
        }
        new MissionsMenu(player, this.config).open();
    }

    private void openShop(Player player) {
        Faction faction = factionOf(player);
        if (faction == null || !FPerm.SHOP.has(player, faction, true)) {
            return;
        }
        new ShopMenu(player, this.config).open();
    }

    private void openLogs(Player player) {
        Faction faction = factionOf(player);
        if (faction == null || !FPerm.LOGS.has(player, faction, true)) {
            return;
        }
        new LogsMenu(player, this.config).open();
    }

    private static Faction factionOf(Player player) {
        FPlayer fplayer = FPlayers.i.get(player);
        return fplayer == null ? null : fplayer.getFaction();
    }
}
