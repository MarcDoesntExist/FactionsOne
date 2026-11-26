package com.massivecraft.factions.config;

import com.massivecraft.factions.zcore.Lang;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the YAML message bridge.
 *
 * @author MarcDoesntExists
 */
class MessagesConfigTest {

    @TempDir
    Path directory;

    @Test
    void generatesDefaultsAndAppliesOverrides() throws IOException {
        String original = Lang.permForbidden;
        try {
            MessagesConfig.load(directory.toFile(), Logger.getLogger("test"));

            Path file = directory.resolve("messages.yml");
            assertTrue(Files.isRegularFile(file));
            assertTrue(Files.readString(file, StandardCharsets.UTF_8)
                    .contains("placeholderWilderness"));

            Files.writeString(file, "permForbidden: \"<b>Custom\"\n",
                    StandardCharsets.UTF_8);
            MessagesConfig.load(directory.toFile(), Logger.getLogger("test"));

            assertEquals("<b>Custom", Lang.permForbidden);
        } finally {
            Lang.permForbidden = original;
        }
    }
}
