package com.massivecraft.factions.config;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.struct.FFlag;
import com.massivecraft.factions.struct.Rel;
import com.massivecraft.factions.util.Color;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the YAML settings bridge.
 *
 * @author MarcDoesntExists
 */
class YamlConfigTest {

    @TempDir
    Path directory;

    @Test
    void generatesACompleteDefaultFile() throws IOException {
        YamlConfig.load(directory.toFile(), Logger.getLogger("test"));

        Path file = directory.resolve("config.yml");
        assertTrue(Files.isRegularFile(file));
        String yaml = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(yaml.contains("powerPlayerMax"));
        assertTrue(yaml.contains("factionFlagDefaults"));
        assertTrue(yaml.contains("claimValueSpawners"));
        assertTrue(Files.isRegularFile(directory.resolve("messages.yml")));
    }

    @Test
    void appliesOverridesOfEverySupportedShape() throws IOException {
        double power = Conf.powerPlayerMax;
        String prefix = Conf.prefixLeader;
        Set<String> worlds = new LinkedHashSet<>(Conf.worldsNoClaiming);
        Color memberColor = Conf.colorMember;
        Rel friendlyFire = Conf.friendlyFireFromRel;
        Map<FFlag, Boolean> flags = new LinkedHashMap<>(Conf.factionFlagDefaults);
        Map<String, String> formats = new LinkedHashMap<>(Conf.chatSingleFormats);
        Map<String, Double> spawners = new LinkedHashMap<>(Conf.claimValueSpawners);
        try {
            Files.writeString(directory.resolve("config.yml"), """
                    powerPlayerMax: 42.5
                    prefixLeader: "!!"
                    worldsNoClaiming:
                      - world_nether
                    colorMember: RED
                    friendlyFireFromRel: ALLY
                    factionFlagDefaults:
                      PVP: false
                    chatSingleFormats:
                      pl: " [%s]"
                    claimValueSpawners:
                      ZOMBIE: 900
                    """, StandardCharsets.UTF_8);

            YamlConfig.load(directory.toFile(), Logger.getLogger("test"));

            assertEquals(42.5, Conf.powerPlayerMax);
            assertEquals("!!", Conf.prefixLeader);
            assertEquals(Set.of("world_nether"), Conf.worldsNoClaiming);
            assertEquals(Color.RED, Conf.colorMember);
            assertEquals(Rel.ALLY, Conf.friendlyFireFromRel);
            assertFalse(Conf.factionFlagDefaults.get(FFlag.PVP));
            assertEquals(" [%s]", Conf.chatSingleFormats.get("pl"));
            assertEquals(900.0, Conf.claimValueSpawners.get("ZOMBIE"));
        } finally {
            Conf.powerPlayerMax = power;
            Conf.prefixLeader = prefix;
            Conf.worldsNoClaiming.clear();
            Conf.worldsNoClaiming.addAll(worlds);
            Conf.colorMember = memberColor;
            Conf.friendlyFireFromRel = friendlyFire;
            Conf.factionFlagDefaults.clear();
            Conf.factionFlagDefaults.putAll(flags);
            Conf.chatSingleFormats.clear();
            Conf.chatSingleFormats.putAll(formats);
            Conf.claimValueSpawners.clear();
            Conf.claimValueSpawners.putAll(spawners);
        }
    }
}
