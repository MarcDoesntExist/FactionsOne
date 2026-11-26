package com.massivecraft.factions.gui;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FeatureConfig;
import com.massivecraft.factions.FeatureService;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.struct.FPerm;
import com.massivecraft.factions.zcore.Lang;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists shop offers and buys the clicked one with faction points.
 *
 * @author MarcDoesntExists
 */
public final class ShopMenu extends PagedMenu {

    private final Faction faction;

    public ShopMenu(Player player, GuiConfig config) {
        this(player, config, factionOf(player));
    }

    private ShopMenu(Player player, GuiConfig config, Faction faction) {
        super(player, config, config.title("menu.shop.title", Lang.shopTitle),
            config.rows("menu.shop.rows", 6),
            clicker -> {
                FPlayer fplayer = FPlayers.i.get(clicker);
                return fplayer != null && fplayer.getFaction() == faction
                    && FPerm.SHOP.has(clicker, faction, false);
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
        for (FeatureConfig.ShopOffer offer : service.getConfig().shopOffers()) {
            Material material = Material.matchMaterial(offer.material());
            ItemStack icon = new ItemStack(material == null || material.isAir()
                ? Material.CHEST : material);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.displayName(Main.main.txt.parse(Lang.shopItemName, offer.name()));
                List<Component> lore = new ArrayList<Component>();
                if (offer.type() == FeatureConfig.ShopOfferType.PERK) {
                    lore.add(Main.main.txt.parse(Lang.shopLorePerk,
                        offer.perk().name().toLowerCase(java.util.Locale.ROOT)));
                    lore.add(Main.main.txt.parse(Lang.shopLorePerkAmount,
                        String.format(java.util.Locale.ROOT, "%.2f", offer.perkAmount())));
                    lore.add(Main.main.txt.parse(Lang.shopLoreDuration,
                        offer.durationMinutes()));
                } else {
                    lore.add(Main.main.txt.parse(Lang.shopLoreAmount, offer.amount()));
                }
                lore.add(Main.main.txt.parse(Lang.shopLoreCost, offer.cost()));
                for (String line : offer.lore()) {
                    lore.add(Main.main.txt.parse(line));
                }
                lore.add(Main.main.txt.parse(Lang.shopLoreClick));
                meta.lore(lore);
                icon.setItemMeta(meta);
            }
            entries.add(new MenuItem(icon, clicker -> {
                FPlayer fplayer = FPlayers.i.get(clicker);
                FeatureService.Result result = service.buyShopOffer(fplayer, this.faction,
                    offer.id());
                if (result == FeatureService.Result.SUCCESS) {
                    clicker.sendMessage(Main.main.txt.parse(Lang.shopPurchase));
                } else {
                    clicker.sendMessage(Main.main.txt.parse(FeatureService.message(result)));
                }
            }));
        }
        this.setEntries(entries);
    }
}
