package com.massivecraft.factions.bank;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Serializes bank movements per account. Two accounts are always locked in
 * lexicographic order, so opposite transfers cannot deadlock.
 *
 * @author MarcDoesntExists
 */
final class AccountLocks {

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    <T> T withLockedAccount(String accountId, Supplier<T> action) {
        ReentrantLock lock = lockFor(accountId);
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    <T> T withLockedAccounts(String first, String second, Supplier<T> action) {
        if (first.equals(second)) {
            return withLockedAccount(first, action);
        }
        String low = first.compareTo(second) <= 0 ? first : second;
        String high = low.equals(first) ? second : first;
        ReentrantLock lowLock = lockFor(low);
        ReentrantLock highLock = lockFor(high);
        lowLock.lock();
        try {
            highLock.lock();
            try {
                return action.get();
            } finally {
                highLock.unlock();
            }
        } finally {
            lowLock.unlock();
        }
    }

    private ReentrantLock lockFor(String accountId) {
        return this.locks.computeIfAbsent(accountId, key -> new ReentrantLock());
    }
}
