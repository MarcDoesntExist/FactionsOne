package com.massivecraft.factions.zcore.persist;

import com.google.gson.Gson;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Main;
import com.massivecraft.factions.zcore.util.DiscUtil;
import com.massivecraft.factions.zcore.util.TextUtil;
import com.massivecraft.factions.zcore.util.UUIDFetcher;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public abstract class EntityCollection<E extends Entity> {
    // -------------------------------------------- //
    // FIELDS
    // -------------------------------------------- //

    private static final Pattern UUID_PATTERN = Pattern.compile(
        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    private static final Pattern LEGACY_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_]{2,16}");
    // These must be instantiated in order to allow for different configuration (orders, comparators etc)
    private final Collection<E> entities;
    // This ugly crap is necessary due to java type erasure
    private final Class<E> entityClass;
    // Guards the one-time background conversion of legacy player names to UUIDs.
    private final AtomicBoolean uuidMigrationScheduled = new AtomicBoolean(false);
    protected Map<String, E> id2entity;
    // If the entities are creative they will create a new instance if a non existent id was requested
    private boolean creative;
    // This is the auto increment for the primary key "id"
    private int nextId;
    // Info on how to persist
    private Gson gson;
    private File file;

    public EntityCollection(Class<E> entityClass, Collection<E> entities, Map<String, E> id2entity,
                            File file, Gson gson, boolean creative) {
        this.entityClass = entityClass;
        this.entities = entities;
        this.id2entity = id2entity;
        this.file = file;
        this.gson = gson;
        this.creative = creative;
        this.nextId = 1;

        EM.setEntitiesCollectionForEntityClass(this.entityClass, this);
    }

    public EntityCollection(Class<E> entityClass, Collection<E> entities, Map<String, E> id2entity,
                            File file, Gson gson) {
        this(entityClass, entities, id2entity, file, gson, false);
    }

    public boolean isCreative() {
        return creative;
    }

    public void setCreative(boolean creative) {
        this.creative = creative;
    }

    public abstract Type getMapType(); // This is special stuff for GSON.

    public Gson getGson() {
        return gson;
    }

    // -------------------------------------------- //
    // CONSTRUCTORS
    // -------------------------------------------- //

    public void setGson(Gson gson) {
        this.gson = gson;
    }

    public File getFile() {
        return file;
    }

    // -------------------------------------------- //
    // GET
    // -------------------------------------------- //

    public void setFile(File file) {
        this.file = file;
    }

    public Collection<E> get() {
        return entities;
    }

    public Map<String, E> getMap() {
        return this.id2entity;
    }

    public E get(String id) {
        if (this.creative)
            return this.getCreative(id);
        return id2entity.get(id);
    }

    public E getCreative(String id) {
        E e = id2entity.get(id);
        if (e != null)
            return e;
        return this.create(id);
    }

    public boolean exists(String id) {
        if (id == null)
            return false;
        return id2entity.get(id) != null;
    }

    // -------------------------------------------- //
    // CREATE
    // -------------------------------------------- //

    public E getBestIdMatch(String pattern) {
        String id = TextUtil.getBestStartWithCI(this.id2entity.keySet(), pattern);
        if (id == null)
            return null;
        return this.id2entity.get(id);
    }

    public synchronized E create() {
        return this.create(this.getNextId());
    }

    // -------------------------------------------- //
    // ATTACH AND DETACH
    // -------------------------------------------- //

    public synchronized E create(String id) {
        if (!this.isIdFree(id))
            return null;

        E e = null;
        try {
            e = this.entityClass.getDeclaredConstructor().newInstance();
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }

        e.setId(id);
        this.entities.add(e);
        this.id2entity.put(e.getId(), e);
        this.updateNextIdForId(id);
        return e;
    }

    public void attach(E entity) {
        if (entity.getId() != null)
            return;
        entity.setId(this.getNextId());
        this.entities.add(entity);
        this.id2entity.put(entity.getId(), entity);
    }

    public void detach(E entity) {
        entity.preDetach();
        this.entities.remove(entity);
        this.id2entity.remove(entity.getId());
        entity.postDetach();
    }

    public void detach(String id) {
        E entity = this.id2entity.get(id);
        if (entity == null)
            return;
        this.detach(entity);
    }

    public boolean attached(E entity) {
        return this.entities.contains(entity);
    }

    // -------------------------------------------- //
    // DISC
    // -------------------------------------------- //

    public boolean detached(E entity) {
        return !this.attached(entity);
    }

    public boolean saveToDisc() {
        Map<String, E> entitiesThatShouldBeSaved = new HashMap<>();
        for (E entity : this.entities) {
            if (entity.shouldBeSaved()) {
                entitiesThatShouldBeSaved.put(entity.getId(), entity);
            }
        }

        try {
            // Serialize on the calling thread so the snapshot is consistent, then let the
            // background queue perform the disk I/O.
            DiscUtil.writeAsync(this.file, this.gson.toJson(entitiesThatShouldBeSaved));
            return true;
        } catch (RuntimeException ex) {
            Bukkit.getLogger().log(Level.WARNING,
                "[Factions] Could not queue a save for \"" + this.file + "\": " + ex.getMessage());
            return false;
        }
    }

    public boolean loadFromDisc() {
        // Make sure a queued save cannot be overtaken by a reload.
        DiscUtil.flushAsync(5000L);
        Map<String, E> id2entity = this.loadCore();
        if (id2entity == null)
            return false;
        this.entities.clear();
        this.entities.addAll(id2entity.values());
        this.id2entity.clear();
        this.id2entity.putAll(id2entity);
        this.fillIds();
        return true;
    }

    @SuppressWarnings("unchecked")
    private Map<String, E> loadCore() {
        if (!this.file.exists()) {
            return new HashMap<>();
        }

        String content = DiscUtil.readCatch(this.file);
        if (content == null) {
            return null;
        }

        Type type = this.getMapType();
        if (type.toString().contains("FPlayer")) {
            Map<String, FPlayer> data;
            try {
                data = this.gson.fromJson(content, type);
            } catch (Exception ex) {
                return this.handleCorrupt(ex);
            }
            if (data == null) {
                return new HashMap<>();
            }
            // Convert any leftover player names in this file
            List<String> list = new ArrayList<>();
            List<String> invalidNames = new ArrayList<>();
            for (String value : new ArrayList<>(data.keySet())) {
                if (UUID_PATTERN.matcher(value).matches()) {
                    continue;
                }
                if (LEGACY_NAME_PATTERN.matcher(value).matches()) {
                    list.add(value);
                } else {
                    // Not even a valid player name: drop it instead of keeping garbage.
                    invalidNames.add(value);
                    data.remove(value);
                }
            }
            if (!invalidNames.isEmpty()) {
                Bukkit.getLogger().log(Level.INFO, "[Factions] Removed " + invalidNames.size()
                    + " invalid player entries while loading: " + String.join(", ", invalidNames));
            }
            if (!list.isEmpty()) {
                // Never block the server start on the Mojang API. The entries stay keyed
                // by name until the conversion finishes; a player who joins meanwhile is
                // migrated immediately with his real UUID (see #migrateEntityId).
                Bukkit.getLogger().log(Level.INFO, "[Factions] Converting " + list.size()
                    + " old player names to UUID in the background.");
                this.scheduleUuidMigration(list, content);
            }
            return (Map<String, E>) data;
        }
        try {
            return this.gson.fromJson(content, type);
        } catch (Exception ex) {
            return this.handleCorrupt(ex);
        }
    }

    private Map<String, E> handleCorrupt(Exception ex) {
        Bukkit.getLogger().log(Level.WARNING,
            "JSON error encountered loading \"" + this.file + "\": " + ex.getLocalizedMessage());

        // Back up the bad file so the user can recover it; never replace it with empty state.
        File backup = new File(this.file.getPath() + "_bad");
        if (backup.exists()) {
            backup.delete();
        }
        Bukkit.getLogger().log(Level.WARNING, "Backing up copy of bad file to: " + backup);
        this.file.renameTo(backup);
        return null;
    }

    /**
     * Backs up the file and asks Mojang for the UUIDs on a background thread, then
     * re-keys the live collection on the main thread. A failed lookup keeps the
     * name-keyed entries usable instead of deleting data.
     */
    private void scheduleUuidMigration(final List<String> names, final String originalContent) {
        if (!this.uuidMigrationScheduled.compareAndSet(false, true)) {
            return;
        }

        final File backup = new File(this.file.getParentFile(), this.file.getName() + ".old");
        Bukkit.getScheduler().runTaskAsynchronously(Main.main, () -> {
            try {
                DiscUtil.write(backup, originalContent);
            } catch (IOException ex) {
                Bukkit.getLogger().log(Level.WARNING, "[Factions] Could not back up " + this.file
                    + " to " + backup + "; legacy player names were left untouched: "
                    + ex.getMessage());
                this.uuidMigrationScheduled.set(false);
                return;
            }
            Bukkit.getLogger().log(Level.INFO,
                "[Factions] Backed up old player data at " + backup.getAbsolutePath());

            Map<String, UUID> response;
            try {
                response = new UUIDFetcher(names).call();
            } catch (Exception ex) {
                Bukkit.getLogger().log(Level.WARNING,
                    "[Factions] Could not convert old player names to UUID now: " + ex.getMessage());
                this.uuidMigrationScheduled.set(false);
                return;
            }

            if (!Main.main.isEnabled()) {
                // Keep the name-keyed data; the conversion is retried on the next start.
                this.uuidMigrationScheduled.set(false);
                return;
            }

            Bukkit.getScheduler().runTask(Main.main, () -> {
                try {
                    this.applyUuidMigration(response);
                } finally {
                    this.uuidMigrationScheduled.set(false);
                }
            });
        });
    }

    private void applyUuidMigration(Map<String, UUID> response) {
        List<String> removed = new ArrayList<>();
        for (String key : new ArrayList<>(this.id2entity.keySet())) {
            if (UUID_PATTERN.matcher(key).matches()) {
                continue;
            }
            UUID uuid = response.get(key);
            E entity = this.id2entity.remove(key);
            if (uuid == null || entity == null) {
                if (entity != null) {
                    this.entities.remove(entity);
                }
                removed.add(key);
                continue;
            }
            String newId = uuid.toString();
            if (this.id2entity.containsKey(newId)) {
                // The player joined while the conversion was running and already got a
                // fresh profile: keep that one instead of ending up with two objects.
                this.entities.remove(entity);
                continue;
            }
            entity.setId(newId);
            this.id2entity.put(newId, entity);
        }

        if (!removed.isEmpty()) {
            Bukkit.getLogger().log(Level.INFO,
                "[Factions] Removed player entries without a valid UUID: "
                    + String.join(", ", removed));
        }
        this.saveToDisc();
        Bukkit.getLogger().log(Level.INFO, "[Factions] Done converting old player names to UUID.");
    }

    /**
     * Re-keys a single legacy name entry when its owner joins before the background
     * conversion finished. The real UUID from the Player is authoritative, so no
     * Mojang request is needed here.
     */
    protected void migrateEntityId(E entity, String newId) {
        if (entity == null || newId == null) {
            return;
        }
        String oldId = entity.getId();
        if (newId.equals(oldId)) {
            return;
        }
        if (oldId != null) {
            this.id2entity.remove(oldId);
        }
        entity.setId(newId);
        this.id2entity.put(newId, entity);
        Bukkit.getLogger().log(Level.INFO,
            "[Factions] Moved legacy player data from \"" + oldId + "\" to " + newId);
        this.saveToDisc();
    }

    // -------------------------------------------- //
    // ID MANAGEMENT
    // -------------------------------------------- //

    public String getNextId() {
        while (!isIdFree(this.nextId)) {
            this.nextId += 1;
        }
        return Integer.toString(this.nextId);
    }

    public boolean isIdFree(String id) {
        return !this.id2entity.containsKey(id);
    }

    public boolean isIdFree(int id) {
        return this.isIdFree(Integer.toString(id));
    }

    protected synchronized void fillIds() {
        this.nextId = 1;
        for (Entry<String, E> entry : this.id2entity.entrySet()) {
            String id = entry.getKey();
            E entity = entry.getValue();
            entity.id = id;
            this.updateNextIdForId(id);
        }
    }

    protected synchronized void updateNextIdForId(int id) {
        if (this.nextId < id) {
            this.nextId = id + 1;
        }
    }

    protected void updateNextIdForId(String id) {
        try {
            int idAsInt = Integer.parseInt(id);
            this.updateNextIdForId(idAsInt);
        } catch (Exception ignored) {
        }
    }
}
