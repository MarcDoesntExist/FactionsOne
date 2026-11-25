package com.massivecraft.factions;

import com.google.gson.reflect.TypeToken;
import com.massivecraft.factions.iface.RelationParticipator;
import com.massivecraft.factions.struct.TerritoryAccess;
import com.massivecraft.factions.util.AsciiCompass;
import com.massivecraft.factions.zcore.util.DiscUtil;
import com.massivecraft.factions.util.Color;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.io.File;
import java.lang.reflect.Type;
import java.util.*;
import java.util.Map.Entry;


/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class Board {
    private static final transient File file = new File(Main.main.getDataFolder(), "board.json");
    public static transient HashMap<FLocation, TerritoryAccess> flocationIds =
            new HashMap<FLocation, TerritoryAccess>();

    //----------------------------------------------//
    // Get and Set
    //----------------------------------------------//
    public static String getIdAt(FLocation flocation) {
        if (!flocationIds.containsKey(flocation))
            return "0";

        return flocationIds.get(flocation).getHostFactionID();
    }

    public static TerritoryAccess getTerritoryAccessAt(FLocation flocation) {
        if (!flocationIds.containsKey(flocation)) {
            return new TerritoryAccess("0");
        }
        return flocationIds.get(flocation);
    }

    public static Faction getFactionAt(FLocation flocation) {
        return Factions.i.get(getIdAt(flocation));
    }

    public static Faction getFactionAt(Location location) {
        return getFactionAt(new FLocation(location));
    }

    public static Faction getFactionAt(Block block) {
        return getFactionAt(new FLocation(block));
    }

    public static void setIdAt(String id, FLocation flocation) {
        if ("0".equals(id)) {
            removeAt(flocation);
            return;
        }

        flocationIds.put(flocation, new TerritoryAccess(id));
    }

    public static void setFactionAt(Faction faction, FLocation flocation) {
        setIdAt(faction.getId(), flocation);
    }

    public static void removeAt(FLocation flocation) {
        flocationIds.remove(flocation);
    }

    public static void unclaimAll(String factionId) {
        Iterator<Entry<FLocation, TerritoryAccess>> iter = flocationIds.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<FLocation, TerritoryAccess> entry = iter.next();
            if (entry.getValue().getHostFactionID().equals(factionId)) {
                iter.remove();
            }
        }
    }

    // Is this coord NOT completely surrounded by coords claimed by the same faction?
    // Simpler: Is there any nearby coord with a faction other than the faction here?
    public static boolean isBorderLocation(FLocation flocation) {
        Faction faction = getFactionAt(flocation);
        FLocation a = flocation.getRelative(1, 0);
        FLocation b = flocation.getRelative(-1, 0);
        FLocation c = flocation.getRelative(0, 1);
        FLocation d = flocation.getRelative(0, -1);
        return faction != getFactionAt(a) || faction != getFactionAt(b) || faction != getFactionAt(
                c) || faction != getFactionAt(d);
    }

    // Is this coord connected to any coord claimed by the specified faction?
    public static boolean isConnectedLocation(FLocation flocation, Faction faction) {
        FLocation a = flocation.getRelative(1, 0);
        FLocation b = flocation.getRelative(-1, 0);
        FLocation c = flocation.getRelative(0, 1);
        FLocation d = flocation.getRelative(0, -1);
        return faction == getFactionAt(a) || faction == getFactionAt(b) || faction == getFactionAt(
                c) || faction == getFactionAt(d);
    }


    //----------------------------------------------//
    // Cleaner. Remove orphaned foreign keys
    //----------------------------------------------//

    public static void clean() {
        Iterator<Entry<FLocation, TerritoryAccess>> iter = flocationIds.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<FLocation, TerritoryAccess> entry = iter.next();
            if (!Factions.i.exists(entry.getValue().getHostFactionID())) {
                Main.main.log(
                        "Board cleaner removed " + entry.getValue().getHostFactionID() + " from "
                                + entry.getKey());
                iter.remove();
            }
        }
    }

    //----------------------------------------------//
    // Coord count
    //----------------------------------------------//

    public static int getFactionCoordCount(String factionId) {
        int ret = 0;
        for (TerritoryAccess thatFactionId : flocationIds.values()) {
            if (thatFactionId.getHostFactionID().equals(factionId)) {
                ret += 1;
            }
        }
        return ret;
    }

    public static int getFactionCoordCount(Faction faction) {
        return getFactionCoordCount(faction.getId());
    }

    public static int getFactionCoordCountInWorld(Faction faction, String worldName) {
        String factionId = faction.getId();
        int ret = 0;
        Iterator<Entry<FLocation, TerritoryAccess>> iter = flocationIds.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<FLocation, TerritoryAccess> entry = iter.next();
            if (entry.getValue().getHostFactionID().equals(factionId) && entry.getKey()
                    .getWorldName().equals(worldName)) {
                ret += 1;
            }
        }
        return ret;
    }

    //----------------------------------------------//
    // Map generation
    //----------------------------------------------//

    /**
     * The map is relative to a coord and a faction
     * north is in the direction of decreasing x
     * east is in the direction of decreasing z
     */
    public static ArrayList<Component> getMap(RelationParticipator observer, FLocation flocation,
                                              double inDegrees) {
        ArrayList<Component> ret = new ArrayList<Component>();
        Faction factionLoc = getFactionAt(flocation);
        Component header = Component.text("(" + flocation.getCoordString() + ") ")
                .append(factionLoc.getTag(observer));
        ret.add(Main.main.txt.titleize(header));

        int halfWidth = Conf.mapWidth / 2;
        int halfHeight = Conf.mapHeight / 2;
        FLocation topLeft = flocation.getRelative(-halfWidth, -halfHeight);
        int width = halfWidth * 2 + 1;
        int height = halfHeight * 2 + 1;

        //Make room for the list of tags
        height--;


        Map<Faction, Character> fList = new HashMap<Faction, Character>();
        ArrayList<String> rows = new ArrayList<String>();

        // For each row
        for (int dz = 0; dz < height; dz++) {
            rows.add(getMapRow(observer, topLeft.getRelative(0, dz), width,
                    dz == halfHeight, fList));
        }

        // Get the compass
        ArrayList<String> asciiCompass =
                AsciiCompass.getAsciiCompass(inDegrees, Color.RED, Color.GOLD.toString());

        // Add the compass to the first map rows
        rows.set(0, asciiCompass.get(0) + rows.get(0).substring(3 * 3));
        rows.set(1, asciiCompass.get(1) + rows.get(1).substring(3 * 3));
        rows.set(2, asciiCompass.get(2) + rows.get(2).substring(3 * 3));

        for (String row : rows) {
            ret.add(Main.main.txt.parse(row));
        }

        Component legend = Component.empty();
        for (Faction keyfaction : fList.keySet()) {
            legend = legend
                    .append(Component.text(fList.get(keyfaction) + ": ",
                            Style.style(keyfaction.getColorTo(observer).toTextColor())))
                    .append(keyfaction.getTag(observer))
                    .append(Component.text(" "));
        }
        ret.add(legend);

        return ret;
    }


    /**
     * Draws one row of the map. Only the center row passes {@code centerRow},
     * so the player marker is not repeated on every line.
     */
    private static String getMapRow(RelationParticipator observer, FLocation rowStart, int width,
                                    boolean centerRow, Map<Faction, Character> factionChars) {
        int centerColumn = width / 2;
        StringBuilder row = new StringBuilder();
        for (int dx = 0; dx < width; dx++) {
            if (centerRow && dx == centerColumn) {
                row.append(Color.AQUA).append('+');
                continue;
            }

            Faction factionHere = getFactionAt(rowStart.getRelative(dx, 0));
            if (factionHere.isNone()) {
                row.append(Color.GRAY).append('-');
                continue;
            }

            Character fchar = factionChars.get(factionHere);
            if (fchar == null) {
                if (factionChars.size() >= Conf.mapKeyChrs.length) {
                    fchar = '?';
                } else {
                    fchar = Conf.mapKeyChrs[factionChars.size()];
                    factionChars.put(factionHere, fchar);
                }
            }
            row.append(factionHere.getColorTo(observer)).append(fchar);
        }
        return row.toString();
    }

    /**
     * One-line version of the map for the action bar: only the row the player
     * is standing on is built. Prefixed by the chunk coordinates and faction tag.
     */
    public static Component getActionBarMap(RelationParticipator observer, FLocation flocation) {
        int halfWidth = Conf.mapWidth / 2;
        int width = halfWidth * 2 + 1;
        FLocation rowStart = flocation.getRelative(-halfWidth, 0);
        String row = getMapRow(observer, rowStart, width, true,
                new HashMap<Faction, Character>());
        return Component.text("(" + flocation.getCoordString() + ") ",
                        Style.style(NamedTextColor.GRAY))
                .append(getFactionAt(flocation).getTag(observer))
                .append(Component.text(" "))
                .append(Main.main.txt.parse(row));
    }

    // -------------------------------------------- //
    // Persistance
    // -------------------------------------------- //

    public static Map<String, Map<String, TerritoryAccess>> dumpAsSaveFormat() {
        Map<String, Map<String, TerritoryAccess>> worldCoordIds =
                new HashMap<String, Map<String, TerritoryAccess>>();

        String worldName, coords;
        TerritoryAccess data;

        for (Entry<FLocation, TerritoryAccess> entry : flocationIds.entrySet()) {
            worldName = entry.getKey().getWorldName();
            coords = entry.getKey().getCoordString();
            data = entry.getValue();
            if (!worldCoordIds.containsKey(worldName)) {
                worldCoordIds.put(worldName, new TreeMap<String, TerritoryAccess>());
            }

            worldCoordIds.get(worldName).put(coords, data);
        }

        return worldCoordIds;
    }

    public static void loadFromSaveFormat(Map<String, Map<String, TerritoryAccess>> worldCoordIds) {
        flocationIds.clear();

        String worldName;
        String[] coords;
        int x, z;
        TerritoryAccess data;

        for (Entry<String, Map<String, TerritoryAccess>> entry : worldCoordIds.entrySet()) {
            worldName = entry.getKey();
            for (Entry<String, TerritoryAccess> entry2 : entry.getValue().entrySet()) {
                coords = entry2.getKey().trim().split("[,\\s]+");
                x = Integer.parseInt(coords[0]);
                z = Integer.parseInt(coords[1]);
                data = entry2.getValue();
                flocationIds.put(new FLocation(worldName, x, z), data);
            }
        }
    }

    public static boolean save() {
        //Factions.log("Saving board to disk");

        try {
            // Serialize now, write on the background save queue.
            DiscUtil.writeAsync(file, Main.main.gson.toJson(dumpAsSaveFormat()));
        } catch (Exception e) {
            e.printStackTrace();
            Main.main.log("Failed to save the board to disk.");
            return false;
        }

        return true;
    }

    public static boolean load() {
        Main.main.log("Loading board from disk");

        if (!file.exists()) {
            Main.main.log("No board to load from disk. Creating new file.");
            // First creation is synchronous so the file is there for the rest of startup.
            if (!DiscUtil.writeCatch(file, Main.main.gson.toJson(dumpAsSaveFormat()))) {
                Main.main.log("Failed to save the board to disk.");
                return false;
            }
            return true;
        }

        try {
            Type type = new TypeToken<Map<String, Map<String, TerritoryAccess>>>() {
            }.getType();
            Map<String, Map<String, TerritoryAccess>> worldCoordIds =
                    Main.main.gson.fromJson(DiscUtil.read(file), type);
            loadFromSaveFormat(worldCoordIds);
        } catch (Exception e) {
            e.printStackTrace();
            Main.main.log("Failed to load the board from disk.");
            return false;
        }

        return true;
    }
}



















