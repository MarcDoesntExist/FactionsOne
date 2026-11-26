package com.massivecraft.factions;

import com.google.gson.reflect.TypeToken;
import com.massivecraft.factions.zcore.persist.PlayerEntityCollection;

import java.io.File;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class FPlayers extends PlayerEntityCollection<FPlayer> {
    public static FPlayers i = new FPlayers();

    Main main = Main.main;

    private FPlayers() {
        super(FPlayer.class, new CopyOnWriteArrayList<FPlayer>(),
            new ConcurrentSkipListMap<String, FPlayer>(String.CASE_INSENSITIVE_ORDER),
            new File(Main.main.getDataFolder(), "players.json"), Main.main.gson);

        this.setCreative(true);
    }

    @Override
    public Type getMapType() {
        return new TypeToken<Map<String, FPlayer>>() {
        }.getType();
    }

    public void clean() {
        for (FPlayer fplayer : this.get()) {
            if (!Factions.i.exists(fplayer.getFactionId())) {
                main.log("Reset faction data (invalid faction) for player " + fplayer.getName());
                fplayer.resetFactionData(false);
            }
        }
    }
}
