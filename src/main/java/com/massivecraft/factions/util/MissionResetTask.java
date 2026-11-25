package com.massivecraft.factions.util;

import com.massivecraft.factions.Main;

/**
 * Scheduler entry point for expired daily and weekly missions.
 *
 * @author MarcDoesntExists
 */
public class MissionResetTask implements Runnable {

    @Override
    public void run() {
        if (Main.main == null || Main.main.featureService == null) {
            return;
        }
        Main.main.featureService.resetExpiredMissions(System.currentTimeMillis());
    }
}
