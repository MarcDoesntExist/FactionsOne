package com.massivecraft.factions.bank;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

/**
 * Vault backed {@link BankGateway}. Accounts are created lazily so faction
 * bank reads never mutate the provider.
 *
 * @author MarcDoesntExists
 */
public final class VaultBankGateway implements BankGateway {

    private static final double EPSILON = 1.0e-6;

    private final Economy economy;

    public VaultBankGateway(Economy economy) {
        this.economy = economy;
    }

    private static OfflinePlayer resolve(String accountId) {
        if (accountId == null) {
            return null;
        }
        try {
            return Bukkit.getOfflinePlayer(UUID.fromString(accountId));
        } catch (IllegalArgumentException legacyNameKey) {
            return Bukkit.getOfflinePlayer(accountId);
        }
    }

    @Override
    public boolean available() {
        return this.economy != null && this.economy.isEnabled();
    }

    @Override
    public boolean hasAccount(String accountId) {
        OfflinePlayer account = resolve(accountId);
        return available() && account != null && this.economy.hasAccount(account);
    }

    @Override
    public double balance(String accountId) {
        OfflinePlayer account = resolve(accountId);
        if (!available() || account == null) {
            return 0.0;
        }
        return this.economy.getBalance(account);
    }

    @Override
    public boolean deposit(String accountId, double amount) {
        OfflinePlayer account = resolve(accountId);
        if (!available() || account == null || amount <= 0.0) {
            return false;
        }
        if (!this.economy.hasAccount(account)) {
            this.economy.createPlayerAccount(account);
        }
        return this.economy.depositPlayer(account, amount).transactionSuccess();
    }

    @Override
    public boolean withdraw(String accountId, double amount) {
        OfflinePlayer account = resolve(accountId);
        if (!available() || account == null || amount <= 0.0 || !this.economy.hasAccount(account)) {
            return false;
        }
        if (!this.economy.has(account, amount - EPSILON)) {
            return false;
        }

        EconomyResponse response = this.economy.withdrawPlayer(account, amount);
        if (!response.transactionSuccess()) {
            return false;
        }

        // Some providers accept a withdrawal even when the balance is too low.
        // Put the shortfall back so the bank can never create money.
        if (response.balance < -EPSILON) {
            this.economy.depositPlayer(account, -response.balance);
            return false;
        }
        return true;
    }

    @Override
    public String format(double amount) {
        return available() ? this.economy.format(amount) : String.format("%.2f", amount);
    }
}
