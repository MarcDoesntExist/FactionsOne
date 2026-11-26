package com.massivecraft.factions.config;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.struct.FFlag;
import com.massivecraft.factions.struct.FPerm;
import com.massivecraft.factions.struct.Rel;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Generates and applies config.yml over the legacy conf.json settings.
 * conf.json remains the fallback and is never deleted.
 *
 * @author MarcDoesntExists
 */
public final class YamlConfig {

    private static final String FILE_NAME = "config.yml";
    private static boolean generatedOnLoad;
    private static final String HEADER = """
        # FactionsOne YAML configuration.
        # Values here override conf.json at startup and on /f reload conf.
        # Remove a key to fall back to the value loaded from conf.json.
        """;

    private YamlConfig() {
    }

    public static void load(Main plugin) {
        load(plugin.getDataFolder(), plugin.getLogger());
    }

    static void load(File dataFolder, Logger logger) {
        generatedOnLoad = false;
        try {
            File file = new File(dataFolder, FILE_NAME);
            if (!file.isFile()) {
                save(dataFolder, logger);
                generatedOnLoad = true;
            }
            if (file.isFile()) {
                apply(YamlConfiguration.loadConfiguration(file), logger);
            }
        } catch (RuntimeException exception) {
            logger.warning("Could not apply " + FILE_NAME + ": " + exception.getMessage());
        } finally {
            MessagesConfig.load(dataFolder, logger);
        }
    }

    /**
     * @return true when this load created config.yml for the first time
     */
    public static boolean wasGeneratedOnLoad() {
        return generatedOnLoad;
    }

    public static void save(Main plugin) {
        save(plugin.getDataFolder(), plugin.getLogger());
    }

    static void save(File dataFolder, Logger logger) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Field field : managedFields()) {
            try {
                Object value = field.get(null);
                if (value != null) {
                    write(yaml, field.getName(), value);
                }
            } catch (IllegalAccessException exception) {
                logger.warning("Could not write config.yml field " + field.getName()
                    + ": " + exception.getMessage());
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

    private static void apply(ConfigurationSection yaml, Logger logger) {
        for (Field field : managedFields()) {
            if (!yaml.contains(field.getName())) {
                continue;
            }
            try {
                Object value = read(field, yaml);
                if (value != null) {
                    field.set(null, value);
                }
            } catch (RuntimeException | IllegalAccessException exception) {
                logger.warning("Invalid " + FILE_NAME + " value for " + field.getName()
                    + ": " + exception.getMessage());
            }
        }
    }

    private static List<Field> managedFields() {
        List<Field> fields = new ArrayList<>();
        for (Field field : Conf.class.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (!Modifier.isPublic(modifiers) || !Modifier.isStatic(modifiers)
                || Modifier.isTransient(modifiers) || Modifier.isFinal(modifiers)
                || field.isSynthetic() || field.getName().endsWith("Migrated")
                || !isSupported(field.getType())) {
                continue;
            }
            fields.add(field);
        }
        fields.sort(Comparator.comparing(Field::getName));
        return fields;
    }

    private static boolean isSupported(Class<?> type) {
        return type.isPrimitive() || type == String.class || type.isEnum()
            || List.class.isAssignableFrom(type) || Set.class.isAssignableFrom(type)
            || Map.class.isAssignableFrom(type);
    }

    private static void write(YamlConfiguration yaml, String path, Object value) {
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                write(yaml, path + "." + name(entry.getKey()), entry.getValue());
            }
        } else if (value instanceof Set<?> set) {
            yaml.set(path, names(set));
        } else if (value instanceof List<?> list) {
            yaml.set(path, names(list));
        } else if (value instanceof Enum<?> enumValue) {
            yaml.set(path, enumValue.name());
        } else {
            yaml.set(path, value);
        }
    }

    private static Object read(Field field, ConfigurationSection yaml) {
        String name = field.getName();
        Class<?> type = field.getType();
        if (type == boolean.class) {
            return yaml.getBoolean(name);
        }
        if (type == int.class) {
            return yaml.getInt(name);
        }
        if (type == long.class) {
            return yaml.getLong(name);
        }
        if (type == double.class) {
            return yaml.getDouble(name);
        }
        if (type == float.class) {
            return (float) yaml.getDouble(name);
        }
        if (type == String.class) {
            return yaml.getString(name, "");
        }
        if (type.isEnum()) {
            return enumValue(type, yaml.getString(name));
        }
        if (List.class.isAssignableFrom(type)) {
            return new ArrayList<>(yaml.getStringList(name));
        }
        if (Set.class.isAssignableFrom(type)) {
            return new LinkedHashSet<>(yaml.getStringList(name));
        }
        if (Map.class.isAssignableFrom(type)) {
            return readMap(name, yaml);
        }
        return null;
    }

    private static Object readMap(String name, ConfigurationSection yaml) {
        ConfigurationSection section = yaml.getConfigurationSection(name);
        if (section == null) {
            return null;
        }
        switch (name) {
            case "factionFlagDefaults":
            case "factionFlagIsChangeable": {
                Map<FFlag, Boolean> result = new LinkedHashMap<>();
                for (String key : section.getKeys(false)) {
                    FFlag flag = FFlag.parse(key);
                    if (flag == null) {
                        throw new IllegalArgumentException("unknown flag " + key);
                    }
                    result.put(flag, section.getBoolean(key));
                }
                return result;
            }
            case "factionPermDefaults": {
                Map<FPerm, Set<Rel>> result = new LinkedHashMap<>();
                for (String key : section.getKeys(false)) {
                    FPerm perm = FPerm.parse(key);
                    if (perm == null) {
                        throw new IllegalArgumentException("unknown permission " + key);
                    }
                    Set<Rel> relations = new LinkedHashSet<>();
                    for (String relationName : section.getStringList(key)) {
                        Rel relation = Rel.parse(relationName);
                        if (relation == null) {
                            throw new IllegalArgumentException("unknown relation " + relationName);
                        }
                        relations.add(relation);
                    }
                    result.put(perm, relations);
                }
                return result;
            }
            case "chatSingleFormats": {
                Map<String, String> result = new LinkedHashMap<>();
                for (String key : section.getKeys(false)) {
                    result.put(key, section.getString(key, ""));
                }
                return result;
            }
            case "claimValueSpawners":
            case "claimValueContainers":
            case "claimValueItems": {
                Map<String, Double> result = new LinkedHashMap<>();
                for (String key : section.getKeys(false)) {
                    result.put(key.toUpperCase(Locale.ROOT), section.getDouble(key));
                }
                return result;
            }
            default:
                return null;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object enumValue(Class<?> type, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Enum.valueOf((Class<? extends Enum>) type,
            raw.trim().toUpperCase(Locale.ROOT));
    }

    private static String name(Object value) {
        return value instanceof Enum<?> enumValue
            ? enumValue.name() : String.valueOf(value);
    }

    private static List<String> names(Collection<?> values) {
        List<String> result = new ArrayList<>(values.size());
        for (Object value : values) {
            result.add(name(value));
        }
        return result;
    }
}
