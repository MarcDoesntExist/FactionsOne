package com.massivecraft.factions.integration;

import com.massivecraft.factions.*;
import com.massivecraft.factions.bank.BankAccess;
import com.massivecraft.factions.bank.BankNotifier;
import com.massivecraft.factions.bank.BankService;
import com.massivecraft.factions.bank.VaultBankGateway;
import com.massivecraft.factions.iface.EconomyParticipator;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;


/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class Econ {
    private static Economy econ = null;

    public static void setup() {
        if (isSetup())
            return;

        String integrationFail =
                "Economy integration is " + (Conf.econEnabled ? "enabled, but" : "disabled, and")
                        + " the plugin \"Vault\" ";

        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            Main.main.log(integrationFail + "is not installed.");
            return;
        }

        RegisteredServiceProvider<Economy> rsp =
                Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            Main.main.log(integrationFail + "is not hooked into an economy plugin.");
            return;
        }
        econ = rsp.getProvider();

        // Every movement goes through the atomic bank service from here on.
        BankService.install(new VaultBankGateway(econ), Econ::shouldBeUsed,
                Econ::modifyUniverseMoney, Main.main.bankJournal);

        Main.main.log("Economy integration through Vault plugin successful.");

        if (!Conf.econEnabled)
            Main.main.log(
                    "NOTE: Economy is disabled. You can enable it with the command: f config econEnabled true");

        oldMoneyDoTransfer();
    }

    public static boolean shouldBeUsed() {
        return Conf.econEnabled && econ != null && econ.isEnabled();
    }

    public static boolean isSetup() {
        return econ != null;
    }


    public static void modifyUniverseMoney(double delta) {
        if (!shouldBeUsed())
            return;

        String account = Conf.econUniverseAccount;
        if (account == null || account.isEmpty())
            return;

        BankService.get().modifyUniverseMoney(account, delta);
    }

    public static void sendBalanceInfo(FPlayer to, EconomyParticipator about) {
        if (!shouldBeUsed()) {
            Main.main
                    .log(Level.WARNING, "Vault does not appear to be hooked into an economy plugin.");
            return;
        }
        to.msg("<a>%s's<i> balance is <h>%s<i>.", about.describeTo(to, true),
                BankService.get().format(BankService.get().balance(about)));
    }

    public static boolean canIControllYou(EconomyParticipator i, EconomyParticipator you) {
        return BankAccess.canControl(i, you);
    }

    public static boolean transferMoney(EconomyParticipator invoker, EconomyParticipator from,
                                        EconomyParticipator to, double amount) {
        return transferMoney(invoker, from, to, amount, true);
    }

    public static boolean transferMoney(EconomyParticipator invoker, EconomyParticipator from,
                                        EconomyParticipator to, double amount, boolean notify) {
        return BankService.get().transfer(invoker, from, to, amount, notify)
                == BankService.Result.SUCCESS;
    }

    public static Set<FPlayer> getFplayers(EconomyParticipator ep) {
        return BankNotifier.recipients(ep);
    }

    public static void sendTransferInfo(EconomyParticipator invoker, EconomyParticipator from,
                                        EconomyParticipator to, double amount) {
        BankNotifier.sendTransferInfo(invoker, from, to, moneyString(amount));
    }

    public static boolean hasAtLeast(EconomyParticipator ep, double delta, String toDoThis) {
        return BankService.get().hasAtLeast(ep, delta, toDoThis);
    }

    public static boolean modifyMoney(EconomyParticipator ep, double delta, String toDoThis,
                                      String forDoingThis) {
        return BankService.get().modifyMoney(ep, delta, toDoThis, forDoingThis);
    }

    // format money string based on server's set currency type, like "24 gold" or "$24.50"
    public static String moneyString(double amount) {
        return BankService.get().format(amount);
    }

    public static void oldMoneyDoTransfer() {
        if (!shouldBeUsed())
            return;

        for (Faction faction : Factions.i.get()) {
            if (faction.money > 0) {
                if (Econ.deposit(faction.getAccountId(), faction.money)) {
                    faction.money = 0;
                }
            }
        }
    }

    // calculate the cost for claiming land
    public static double calculateClaimCost(int ownedLand, boolean takingFromAnotherFaction) {
        if (!shouldBeUsed()) {
            return 0d;
        }

        // basic claim cost, plus land inflation cost, minus the potential bonus given for claiming from another faction
        return Conf.econCostClaimWilderness + (Conf.econCostClaimWilderness
                * Conf.econClaimAdditionalMultiplier * ownedLand) - (takingFromAnotherFaction ?
                Conf.econCostClaimFromFactionBonus :
                0);
    }

    // calculate refund amount for unclaiming land
    public static double calculateClaimRefund(int ownedLand) {
        return calculateClaimCost(ownedLand - 1, false) * Conf.econClaimRefundMultiplier;
    }

    // calculate value of all owned land
    public static double calculateTotalLandValue(int ownedLand) {
        double amount = 0;
        for (int x = 0; x < ownedLand; x++) {
            amount += calculateClaimCost(x, false);
        }
        return amount;
    }

    // calculate refund amount for all owned land
    public static double calculateTotalLandRefund(int ownedLand) {
        return calculateTotalLandValue(ownedLand) * Conf.econClaimRefundMultiplier;
    }


    // -------------------------------------------- //
    // Standard account management methods
    // -------------------------------------------- //

    public static boolean hasAccount(String name) {
        if (!shouldBeUsed())
            return false;
        return BankService.get().hasAccount(name);
    }

    public static double getBalance(String account) {
        return BankService.get().balance(account);
    }

    public static boolean setBalance(String account, double amount) {
        return BankService.get().setAccountBalance(account, amount);
    }

    public static boolean modifyBalance(String account, double amount) {
        if (amount < 0) {
            return BankService.get().debitAccount(account, -amount);
        }
        return BankService.get().creditAccount(account, amount);
    }

    public static boolean deposit(String account, double amount) {
        return BankService.get().creditAccount(account, amount);
    }

    public static boolean withdraw(String account, double amount) {
        return BankService.get().debitAccount(account, amount);
    }

    public static boolean isUUID(String uuid) {
        try {
            UUID.fromString(uuid);
        } catch (IllegalArgumentException ex) {
            return false;
        }

        return true;
    }
}
