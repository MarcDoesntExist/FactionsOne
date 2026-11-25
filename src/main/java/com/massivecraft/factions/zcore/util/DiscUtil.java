package com.massivecraft.factions.zcore.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class DiscUtil {
    // -------------------------------------------- //
    // CONSTANTS
    // -------------------------------------------- //

    private static final SaveQueue ASYNC_SAVES = new SaveQueue();

    // -------------------------------------------- //
    // BYTE
    // -------------------------------------------- //

    public static byte[] readBytes(File file) throws IOException {
        int length = (int) file.length();
        byte[] output = new byte[length];
        try (InputStream in = new FileInputStream(file)) {
            int offset = 0;
            // normally it should be able to read the entire file with just a single iteration below, but it depends on the whims of the FileInputStream
            while (offset < length) {
                int read = in.read(output, offset, (length - offset));
                if (read < 0)
                    break;
                offset += read;
            }
        }
        return output;
    }

    /**
     * Writes the bytes to a temporary file next to the target and then moves it
     * into place atomically. A crash can therefore leave either the old file or
     * the new one, never a truncated JSON document.
     */
    public static void writeBytes(File file, byte[] bytes) throws IOException {
        Path target = file.toPath();
        Path parent = target.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temp = Files.createTempFile(parent, file.getName() + ".", ".tmp");
        try {
            Files.write(temp, bytes);
            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException | UnsupportedOperationException ex) {
                // Some file systems cannot move atomically; fall back to a replace.
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    // -------------------------------------------- //
    // ASYNC
    // -------------------------------------------- //

    /**
     * Queues a write on the background save thread. Repeated calls for the same
     * file before it is written keep only the latest content.
     */
    public static void writeAsync(File file, String content) {
        ASYNC_SAVES.submit(file, content);
    }

    /**
     * Waits for queued writes to reach the disk.
     */
    public static void flushAsync(long timeoutMillis) {
        ASYNC_SAVES.flush(timeoutMillis);
    }

    /**
     * Flushes and stops the background save thread.
     */
    public static void shutdownAsync(long timeoutMillis) {
        ASYNC_SAVES.shutdown(timeoutMillis);
    }

    // -------------------------------------------- //
    // STRING
    // -------------------------------------------- //

    public static void write(File file, String content) throws IOException {
        writeBytes(file, utf8(content));
    }

    public static String read(File file) throws IOException {
        return utf8(readBytes(file));
    }

    // -------------------------------------------- //
    // CATCH
    // -------------------------------------------- //

    public static boolean writeCatch(File file, String content) {
        try {
            write(file, content);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String readCatch(File file) {
        try {
            return read(file);
        } catch (IOException e) {
            return null;
        }
    }

    // -------------------------------------------- //
    // FILE DELETION
    // -------------------------------------------- //

    public static boolean deleteRecursive(File path) throws FileNotFoundException {
        if (!path.exists())
            throw new FileNotFoundException(path.getAbsolutePath());
        boolean ret = true;
        if (path.isDirectory()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File f : files) {
                    ret = ret && deleteRecursive(f);
                }
            }
        }
        return ret && path.delete();
    }

    // -------------------------------------------- //
    // UTF8 ENCODE AND DECODE
    // -------------------------------------------- //

    public static byte[] utf8(String string) {
        return string.getBytes(StandardCharsets.UTF_8);
    }

    public static String utf8(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

}
