package com.massivecraft.factions;

import com.massivecraft.factions.struct.Permission;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Guards the plugin.yml contract: every permission node the code can query has
 * to be declared, and every node referenced from a {@code children} list has to
 * exist. An undeclared node silently falls back to the server default (op-only),
 * so without this check the only symptom is a command that does not work for
 * the players who were supposed to have it.
 *
 * @author MarcDoesntExists
 */
class PluginYamlContractTest {

    private static YamlConfiguration plugin;
    private static ConfigurationSection permissions;
    private static Set<String> declaredNodes;

    @BeforeAll
    static void loadPluginYaml() {
        InputStream stream = PluginYamlContractTest.class.getResourceAsStream("/plugin.yml");
        assertNotNull(stream, "plugin.yml is missing from the test classpath");
        // Permission nodes contain dots, which Bukkit otherwise treats as a
        // nested path; disable the path separator so keys stay literal.
        plugin = new YamlConfiguration();
        plugin.options().pathSeparator('\u0000');
        try {
            plugin.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException | InvalidConfigurationException ex) {
            fail("plugin.yml could not be parsed: " + ex.getMessage());
        }
        permissions = plugin.getConfigurationSection("permissions");
        assertNotNull(permissions, "plugin.yml declares no permissions section");
        declaredNodes = new LinkedHashSet<>(permissions.getKeys(false));
    }

    @Test
    void pluginDescriptorIsComplete() {
        assertEquals("Factions", plugin.getString("name"));
        assertEquals(Main.class.getName(), plugin.getString("main"));
        assertNotNull(plugin.getString("api-version"), "api-version is required");
        assertTrue(plugin.getString("api-version").matches("\\d+\\.\\d+"),
                "api-version must look like 26.3, was " + plugin.getString("api-version"));
        assertNotNull(plugin.getString("version"));
        assertTrue(plugin.getStringList("authors").contains("MarcDoesntExists"));
    }

    @Test
    void baseCommandIsDeclared() {
        ConfigurationSection commands = plugin.getConfigurationSection("commands");
        assertNotNull(commands, "plugin.yml declares no commands section");
        ConfigurationSection command = commands.getConfigurationSection("factions");
        assertNotNull(command, "the factions command is not declared");
        assertTrue(command.getStringList("aliases").contains("f"),
                "the factions command should keep the /f alias");
    }

    @Test
    void everyPermissionNodeUsedByCodeIsDeclared() {
        List<String> missing = new ArrayList<>();
        for (Permission permission : Permission.values()) {
            if (!declaredNodes.contains(permission.node)) {
                missing.add(permission.node);
            }
        }
        assertEquals(List.of(), missing,
                "Permission constants without a plugin.yml declaration");
    }

    @Test
    void everyPermissionChildIsDeclared() {
        List<String> dangling = new ArrayList<>();
        for (String parent : declaredNodes) {
            ConfigurationSection section = permissions.getConfigurationSection(parent);
            ConfigurationSection children = section == null ? null : section.getConfigurationSection("children");
            if (children == null) {
                continue;
            }
            for (String child : children.getKeys(false)) {
                if (!declaredNodes.contains(child)) {
                    dangling.add(parent + " -> " + child);
                }
            }
        }
        assertEquals(List.of(), dangling,
                "children entries pointing at undeclared permission nodes");
    }
}
