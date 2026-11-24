package com.massivecraft.factions.zcore.persist;

import com.massivecraft.factions.zcore.MPlugin;

/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class SaveTask implements Runnable {
    static private boolean running = false;

    MPlugin p;

    public SaveTask(MPlugin p) {
        this.p = p;
    }

    public void run() {
        if (!p.getAutoSave() || running)
            return;
        running = true;
        p.preAutoSave();
        EM.saveAllToDisc();
        p.postAutoSave();
        running = false;
    }
}
