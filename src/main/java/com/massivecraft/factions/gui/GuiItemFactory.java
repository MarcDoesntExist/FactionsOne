package com.massivecraft.factions.gui;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.massivecraft.factions.Main;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Builds menu icons from YAML. The optional `serialized` value is the Base64
 * form of {@link ItemStack#serializeAsBytes()}, so any current or future item
 * (components, books, banners, potions, heads...) can be used without adding a
 * special case for every material. The readable fields remain useful for small
 * server-owned buttons and for player heads.
 *
 * @author MarcDoesntExists
 */
public final class GuiItemFactory {

    private final Main plugin;

    public GuiItemFactory(Main plugin) {
        this.plugin = plugin;
    }

    private static String encodeTextureUrl(String url) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    public ItemStack create(ConfigurationSection section, Player viewer) {
        if (section == null) {
            return new ItemStack(Material.BARRIER);
        }

        ItemStack stack = this.deserialize(section.getString("serialized"));
        if (stack == null) {
            Material material = Material.matchMaterial(section.getString("material", "STONE"));
            stack = new ItemStack(material == null ? Material.STONE : material);
        }

        this.applyHead(stack, section.getConfigurationSection("head"));
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        String name = section.getString("name");
        if (name != null) {
            meta.displayName(this.parse(name, viewer));
        }

        List<String> lore = section.getStringList("lore");
        if (!lore.isEmpty()) {
            List<Component> lines = new ArrayList<Component>();
            for (String line : lore) {
                lines.add(this.parse(line, viewer));
            }
            meta.lore(lines);
        }

        this.applyEnchantments(meta, section.getConfigurationSection("enchants"));
        this.applyFlags(meta, section.getStringList("flags"));

        if (section.isSet("custom-model-data")) {
            var component = meta.getCustomModelDataComponent();
            component.setFloats(List.of((float) section.getInt("custom-model-data")));
            meta.setCustomModelDataComponent(component);
        }
        if (section.isString("item-model")) {
            NamespacedKey model = NamespacedKey.fromString(section.getString("item-model"));
            if (model != null) {
                meta.setItemModel(model);
            }
        }
        if (section.isBoolean("unbreakable")) {
            meta.setUnbreakable(section.getBoolean("unbreakable"));
        }
        if (section.isBoolean("glint")) {
            meta.setEnchantmentGlintOverride(section.getBoolean("glint"));
        }
        if (section.isBoolean("hide-tooltip")) {
            meta.setHideTooltip(section.getBoolean("hide-tooltip"));
        }
        if (section.isString("rarity")) {
            try {
                meta.setRarity(ItemRarity.valueOf(section.getString("rarity").toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                this.plugin.getLogger().warning("Unknown item rarity: " + section.getString("rarity"));
            }
        }

        stack.setItemMeta(meta);
        if (section.isSet("amount")) {
            stack.setAmount(Math.max(1, Math.min(99, section.getInt("amount", 1))));
        }
        return stack;
    }

    private ItemStack deserialize(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(encoded.trim()));
        } catch (RuntimeException exception) {
            this.plugin.getLogger().warning("Invalid serialized menu item: " + exception.getMessage());
            return null;
        }
    }

    private void applyHead(ItemStack stack, ConfigurationSection head) {
        if (head == null || stack.getType() != Material.PLAYER_HEAD) {
            return;
        }
        ItemMeta meta = stack.getItemMeta();
        if (!(meta instanceof SkullMeta skull)) {
            return;
        }

        String name = head.getString("name");
        String uuidText = head.getString("uuid");
        UUID uuid = null;
        if (uuidText != null) {
            try {
                uuid = UUID.fromString(uuidText);
            } catch (IllegalArgumentException exception) {
                this.plugin.getLogger().warning("Invalid menu head UUID: " + uuidText);
            }
        }

        PlayerProfile profile;
        if (uuid != null) {
            profile = Bukkit.createProfile(uuid, name);
        } else if (name != null && !name.isBlank()) {
            profile = Bukkit.createProfile(name);
        } else {
            profile = Bukkit.createProfile(UUID.randomUUID(), "menu");
        }

        String texture = head.getString("texture");
        if (texture != null && !texture.isBlank()) {
            String value = texture.startsWith("http")
                ? encodeTextureUrl(texture)
                : texture;
            String signature = head.getString("signature");
            profile.setProperty(signature == null
                ? new ProfileProperty("textures", value)
                : new ProfileProperty("textures", value, signature));
        }

        skull.setPlayerProfile(profile);
        stack.setItemMeta(skull);
    }

    private void applyEnchantments(ItemMeta meta, ConfigurationSection enchantments) {
        if (enchantments == null) {
            return;
        }
        for (String key : enchantments.getKeys(false)) {
            NamespacedKey namespacedKey = NamespacedKey.fromString(key.toLowerCase(Locale.ROOT));
            Enchantment enchantment = namespacedKey == null ? null
                : RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.ENCHANTMENT).get(namespacedKey);
            if (enchantment == null) {
                this.plugin.getLogger().warning("Unknown menu enchantment: " + key);
                continue;
            }
            meta.addEnchant(enchantment, Math.max(1, enchantments.getInt(key, 1)), true);
        }
    }

    private void applyFlags(ItemMeta meta, List<String> flags) {
        for (String flag : flags) {
            try {
                meta.addItemFlags(ItemFlag.valueOf(flag.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                this.plugin.getLogger().warning("Unknown menu item flag: " + flag);
            }
        }
    }

    private Component parse(String text, Player viewer) {
        return this.plugin.txt.parse(text, viewer);
    }
}
