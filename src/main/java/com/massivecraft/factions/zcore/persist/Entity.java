package com.massivecraft.factions.zcore.persist;

/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public abstract class Entity {
    protected transient String id = null;

    public Entity() {

    }

    public String getId() {
        return id;
    }

    protected void setId(String id) {
        this.id = id;
    }

    public boolean shouldBeSaved() {
        return true;
    }

    // -------------------------------------------- //
    // ATTACH AND DETACH
    // -------------------------------------------- //

    public void attach() {
        EM.attach(this);
    }

    public void detach() {
        EM.detach(this);
    }

    public boolean attached() {
        return EM.attached(this);
    }

    public boolean detached() {
        return EM.detached(this);
    }

    // -------------------------------------------- //
    // EVENTS
    // -------------------------------------------- //

    public void preDetach() {

    }

    public void postDetach() {

    }

}
