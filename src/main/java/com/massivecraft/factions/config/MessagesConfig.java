package com.massivecraft.factions.config;

import com.massivecraft.factions.zcore.Lang;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Generates and applies messages.yml over the centralized {@link Lang} messages.
 * Missing keys keep the compiled defaults, so existing files survive upgrades.
 *
 * @author MarcDoesntExists
 */
public final class MessagesConfig {

    private static final String FILE_NAME = "messages.yml";
    private static final String HEADER = """
        # FactionsOne messages.
        # Values use the legacy <i>, <h>, <g>, <b> tags and %s placeholders.
        # Missing keys keep the built-in default from Lang.java.
        """;

    private MessagesConfig() {
    }

    static void load(File dataFolder, Logger logger) {
        File file = new File(dataFolder, FILE_NAME);
        if (!file.isFile()) {
            save(dataFolder, logger);
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (Field field : messageFields()) {
            if (!yaml.contains(field.getName())) {
                continue;
            }
            String value = yaml.getString(field.getName());
            if (value == null) {
                continue;
            }
            try {
                field.set(null, value);
            } catch (IllegalAccessException exception) {
                logger.warning("Could not apply " + FILE_NAME + " field "
                    + field.getName() + ": " + exception.getMessage());
            }
        }
    }

    private static void save(File dataFolder, Logger logger) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Field field : messageFields()) {
            try {
                Object value = field.get(null);
                if (value != null) {
                    yaml.set(field.getName(), value);
                }
            } catch (IllegalAccessException exception) {
                logger.warning("Could not write " + FILE_NAME + " field "
                    + field.getName() + ": " + exception.getMessage());
            }
        }
        File file = new File(dataFolder, FILE_NAME);
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        try {
            Files.writeString(file.toPath(), HEADER + yaml.saveToString(),
                StandardCharsets.UTF_8);
        } catch (IOException exception) {
            logger.warning("Could not write " + FILE_NAME + ": " + exception.getMessage());
        }
    }

    private static List<Field> messageFields() {
        List<Field> fields = new ArrayList<>();
        for (Field field : Lang.class.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)
                && !Modifier.isFinal(modifiers) && field.getType() == String.class
                && !field.isSynthetic()) {
                fields.add(field);
            }
        }
        fields.sort(Comparator.comparing(Field::getName));
        return fields;
    }
}
