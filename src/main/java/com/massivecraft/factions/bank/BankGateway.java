package com.massivecraft.factions.bank;

/**
 * Money backend behind the faction bank. Production uses Vault; tests use an
 * in-memory implementation.
 *
 * @author MarcDoesntExists
 */
public interface BankGateway {

    boolean available();

    boolean hasAccount(String accountId);

    double balance(String accountId);

    /**
     * Credits the account and reports whether the backend accepted the move.
     */
    boolean deposit(String accountId, double amount);

    /**
     * Debits the account only when it holds enough money. Implementations must
     * never leave a negative balance behind.
     */
    boolean withdraw(String accountId, double amount);

    String format(double amount);
}
