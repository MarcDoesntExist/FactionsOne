package com.massivecraft.factions.integration.map;

import java.util.List;

/**
 * Provider-neutral web map renderer. Implementations must only be called on
 * the server thread.
 *
 * @author MarcDoesntExists
 */
public interface MapRenderer {
    String name();

    void render(List<MapClaim> claims);

    void close();
}
