package com.massivecraft.factions;

import com.massivecraft.factions.util.LazyLocation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A named faction warp with an optional password hash.
 *
 * @author MarcDoesntExists
 */
public final class FactionWarp {

    private String name;
    private LazyLocation location;
    private String passwordHash;

    public FactionWarp() {
        // Gson constructor.
    }

    public FactionWarp(String name, LazyLocation location, String passwordHash) {
        this.name = name;
        this.location = location;
        this.passwordHash = passwordHash;
    }

    public static String hash(String password) {
        if (password == null || password.isBlank()) {
            return null;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public String getName() {
        return this.name;
    }

    public LazyLocation getLocation() {
        return this.location;
    }

    public String getPasswordHash() {
        return this.passwordHash;
    }

    public boolean hasPassword() {
        return this.passwordHash != null && !this.passwordHash.isBlank();
    }

    public boolean matches(String password) {
        if (!this.hasPassword()) {
            return true;
        }
        if (password == null) {
            return false;
        }
        byte[] expected = this.passwordHash.getBytes(StandardCharsets.UTF_8);
        byte[] actual = hash(password).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }
}
