package com.massivecraft.factions.util;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.Main;

/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class EconLandRewardTask implements Runnable {

    double rate;

    public EconLandRewardTask() {
        this.rate = Conf.econLandRewardTaskRunsEveryXMinutes;
    }

    @Override
    public void run() {
        Factions.i.econLandRewardRoutine();
        // maybe setting has been changed? if so, restart task at new rate
        if (this.rate != Conf.econLandRewardTaskRunsEveryXMinutes)
            Main.main.startEconLandRewardTask(true);
    }

}
