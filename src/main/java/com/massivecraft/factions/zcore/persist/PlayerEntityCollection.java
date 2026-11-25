package com.massivecraft.factions.zcore.persist;

import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The PlayerEntityCollection is an EntityCollection with the extra features
 * a player skin usually requires.
 * <p>
 * This entity collection is not only creative. It even creates the instance for the player
 * when the player logs in to the server.
 * <p>
 * This way we can be sure that PlayerEntityCollection.get() will contain
 * all entities in PlayerEntityCollection.getOnline()
 *
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public abstract class PlayerEntityCollection<E extends Entity> extends EntityCollection<E> {
    public PlayerEntityCollection(Class<E> entityClass, Collection<E> entities,
                                  Map<String, E> id2entity, File file, Gson gson) {
        super(entityClass, entities, id2entity, file, gson, true);
    }

    public E get(OfflinePlayer player) {
        return this.getForPlayer(player);
    }

    public E get(Player player) {
        return this.getForPlayer(player);
    }

    /**
     * A file written by an old version may still be keyed by player name while the
     * background UUID conversion runs. Resolve those entries and migrate the one
     * that just came online, so no second profile is created for the player.
     */
    private E getForPlayer(OfflinePlayer player) {
        String uuid = player.getUniqueId().toString();
        E direct = this.id2entity.get(uuid);
        if (direct != null) {
            return direct;
        }

        String name = player.getName();
        if (name != null) {
            E legacy = this.id2entity.get(name);
            if (legacy != null) {
                this.migrateEntityId(legacy, uuid);
                return legacy;
            }
        }

        // Creative collections create the entry on demand; legacy ones return null.
        return super.get(uuid);
    }

    public Set<E> getOnline() {
        Set<E> entities = new HashSet<E>();
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            entities.add(this.get(player));
        }
        return entities;
    }
}
