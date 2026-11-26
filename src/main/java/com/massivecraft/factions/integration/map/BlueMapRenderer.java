package com.massivecraft.factions.integration.map;

import com.massivecraft.factions.Conf;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * BlueMap renderer loaded only when BlueMap is installed. All BlueMap classes
 * are accessed through reflection so the plugin still enables on servers
 * without BlueMap.
 *
 * @author MarcDoesntExists
 */
public final class BlueMapRenderer implements MapRenderer {
    private static final String SET_ID = "factionsone-claims";

    private final Object api;
    private final Method getWorld;
    private final Method getMaps;
    private final Method getMarkerSets;
    private final Method getMarkers;
    private final Method createRect;
    private final Method markerSetBuilder;
    private final Method markerSetLabel;
    private final Method markerSetToggleable;
    private final Method markerSetDefaultHidden;
    private final Method markerSetSorting;
    private final Method markerSetBuild;
    private final Method markerSetSetLabel;
    private final Method shapeMarkerBuilder;
    private final Method shapeMarkerLabel;
    private final Method shapeMarkerShape;
    private final Method shapeMarkerFill;
    private final Method shapeMarkerLine;
    private final Method shapeMarkerLineWidth;
    private final Method shapeMarkerBuild;
    private final Constructor<?> colorConstructor;
    private final Set<Object> trackedMaps =
            Collections.newSetFromMap(new IdentityHashMap<>());

    public BlueMapRenderer() throws ReflectiveOperationException {
        Class<?> apiClass = Class.forName("de.bluecolored.bluemap.api.BlueMapAPI");
        Optional<?> instance = (Optional<?>) apiClass.getMethod("getInstance").invoke(null);
        if (instance.isEmpty()) {
            throw new IllegalStateException("BlueMap API is not ready");
        }
        this.api = instance.get();
        this.getWorld = apiClass.getMethod("getWorld", Object.class);
        Class<?> worldClass = Class.forName("de.bluecolored.bluemap.api.BlueMapWorld");
        this.getMaps = worldClass.getMethod("getMaps");

        Class<?> mapClass = Class.forName("de.bluecolored.bluemap.api.BlueMapMap");
        this.getMarkerSets = mapClass.getMethod("getMarkerSets");
        Class<?> markerSetClass = Class.forName(
                "de.bluecolored.bluemap.api.markers.MarkerSet");
        this.getMarkers = markerSetClass.getMethod("getMarkers");
        this.markerSetSetLabel = markerSetClass.getMethod("setLabel", String.class);
        Class<?> markerSetBuilderClass = Class.forName(
                "de.bluecolored.bluemap.api.markers.MarkerSet$Builder");
        this.markerSetBuilder = markerSetClass.getMethod("builder");
        this.markerSetLabel = markerSetBuilderClass.getMethod("label", String.class);
        this.markerSetToggleable = markerSetBuilderClass.getMethod("toggleable", Boolean.class);
        this.markerSetDefaultHidden = markerSetBuilderClass.getMethod("defaultHidden",
                Boolean.class);
        this.markerSetSorting = markerSetBuilderClass.getMethod("sorting", Integer.class);
        this.markerSetBuild = markerSetBuilderClass.getMethod("build");

        Class<?> shapeClass = Class.forName("de.bluecolored.bluemap.api.math.Shape");
        this.createRect = shapeClass.getMethod("createRect", double.class, double.class,
                double.class, double.class);
        Class<?> shapeMarkerClass = Class.forName(
                "de.bluecolored.bluemap.api.markers.ShapeMarker");
        Class<?> shapeMarkerBuilderClass = Class.forName(
                "de.bluecolored.bluemap.api.markers.ShapeMarker$Builder");
        this.shapeMarkerBuilder = shapeMarkerClass.getMethod("builder");
        this.shapeMarkerLabel = shapeMarkerBuilderClass.getMethod("label", String.class);
        this.shapeMarkerShape = shapeMarkerBuilderClass.getMethod("shape", shapeClass,
                float.class);
        Class<?> colorClass = Class.forName("de.bluecolored.bluemap.api.math.Color");
        this.shapeMarkerFill = shapeMarkerBuilderClass.getMethod("fillColor", colorClass);
        this.shapeMarkerLine = shapeMarkerBuilderClass.getMethod("lineColor", colorClass);
        this.shapeMarkerLineWidth = shapeMarkerBuilderClass.getMethod("lineWidth", int.class);
        this.shapeMarkerBuild = shapeMarkerBuilderClass.getMethod("build");
        this.colorConstructor = colorClass.getConstructor(int.class, int.class, int.class,
                float.class);
    }

    @Override
    public String name() {
        return "BlueMap";
    }

    @Override
    public void render(List<MapClaim> claims) {
        try {
            clearTrackedMaps();
            if (claims == null || claims.isEmpty()) {
                return;
            }

            String label = Conf.mapLayerName == null || Conf.mapLayerName.isBlank()
                    ? "Factions" : Conf.mapLayerName;
            for (MapClaim claim : claims) {
                Optional<?> world = (Optional<?>) getWorld.invoke(api, claim.world());
                if (world.isEmpty()) {
                    continue;
                }
                for (Object map : (Collection<?>) getMaps.invoke(world.get())) {
                    renderOnMap(map, claim, label);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException failure) {
            throw new IllegalStateException("BlueMap render failed", failure);
        }
    }

    private void renderOnMap(Object map, MapClaim claim, String label)
            throws ReflectiveOperationException {
        @SuppressWarnings("unchecked")
        Map<String, Object> markerSets = (Map<String, Object>) getMarkerSets.invoke(map);
        Object markerSet = markerSets.get(SET_ID);
        if (markerSet == null) {
            markerSet = createMarkerSet(label);
            markerSets.put(SET_ID, markerSet);
        } else {
            markerSetSetLabel.invoke(markerSet, label);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> markers = (Map<String, Object>) getMarkers.invoke(markerSet);
        String id = claim.label().replaceAll("[^A-Za-z0-9_-]", "_") + "_"
                + Integer.toHexString(claim.world().hashCode()) + "_"
                + claim.minX() + "_" + claim.minZ();
        markers.put(id, createMarker(claim));
        trackedMaps.add(map);
    }

    private Object createMarkerSet(String label) throws ReflectiveOperationException {
        Object builder = markerSetBuilder.invoke(null);
        markerSetLabel.invoke(builder, label);
        markerSetToggleable.invoke(builder, Boolean.TRUE);
        markerSetDefaultHidden.invoke(builder, Boolean.FALSE);
        markerSetSorting.invoke(builder, 0);
        return markerSetBuild.invoke(builder);
    }

    private Object createMarker(MapClaim claim) throws ReflectiveOperationException {
        double minX = claim.minX() * 16.0D;
        double minZ = claim.minZ() * 16.0D;
        double maxX = (claim.maxX() + 1) * 16.0D;
        double maxZ = (claim.maxZ() + 1) * 16.0D;
        Object shape = createRect.invoke(null, minX, minZ, maxX, maxZ);

        int red = (claim.rgb() >> 16) & 0xFF;
        int green = (claim.rgb() >> 8) & 0xFF;
        int blue = claim.rgb() & 0xFF;
        Object lineColor = colorConstructor.newInstance(red, green, blue, 1.0F);
        Object fillColor = colorConstructor.newInstance(red, green, blue, 0.35F);

        Object builder = shapeMarkerBuilder.invoke(null);
        // BlueMap 5.29 validates the label inside the builder's build() method.
        shapeMarkerLabel.invoke(builder, claim.label());
        shapeMarkerShape.invoke(builder, shape, Conf.mapShapeY);
        shapeMarkerFill.invoke(builder, fillColor);
        shapeMarkerLine.invoke(builder, lineColor);
        shapeMarkerLineWidth.invoke(builder, 3);
        return shapeMarkerBuild.invoke(builder);
    }

    private void clearTrackedMaps() throws ReflectiveOperationException {
        for (Object map : trackedMaps) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> markerSets =
                        (Map<String, Object>) getMarkerSets.invoke(map);
                markerSets.remove(SET_ID);
            } catch (InvocationTargetException | IllegalAccessException ignored) {
                // The map can disappear during a BlueMap reload.
            }
        }
        trackedMaps.clear();
    }

    @Override
    public void close() {
        try {
            clearTrackedMaps();
        } catch (ReflectiveOperationException ignored) {
            // Nothing to release on servers where BlueMap is already gone.
        }
    }
}
