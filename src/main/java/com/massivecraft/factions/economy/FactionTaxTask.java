package com.massivecraft.factions.economy;

import com.massivecraft.factions.Main;

/**
 * Scheduler entry point for the periodic faction tax.
 *
 * @author MarcDoesntExists
 */
public class FactionTaxTask implements Runnable {

    @Override
    public void run() {
        if (Main.main == null || Main.main.factionTaxService == null) {
            return;
        }
        Main.main.factionTaxService.run(false);
    }
}
