package com.massivecraft.factions.zcore.util;

import com.google.gson.*;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * @author evilmidget38
 */

public class UUIDFetcher implements Callable<Map<String, UUID>> {
    private static final int PROFILES_PER_REQUEST = 100;
    private static final int TIMEOUT_MILLIS = 5000;
    private static final String PROFILE_URL = "https://api.mojang.com/profiles/minecraft";
    private final Gson gson = new Gson();
    private final List<String> names;
    private final boolean rateLimiting;

    public UUIDFetcher(List<String> names, boolean rateLimiting) {
        this.names = List.copyOf(names);
        this.rateLimiting = rateLimiting;
    }

    public UUIDFetcher(List<String> names) {
        this(names, true);
    }

    private static HttpURLConnection createConnection() throws Exception {
        HttpURLConnection connection =
                (HttpURLConnection) URI.create(PROFILE_URL).toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setConnectTimeout(TIMEOUT_MILLIS);
        connection.setReadTimeout(TIMEOUT_MILLIS);
        return connection;
    }

    private static UUID getUUID(String id) {
        return UUID.fromString(
                id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16) + "-" + id
                        .substring(16, 20) + "-" + id.substring(20, 32));
    }

    public static byte[] toBytes(UUID uuid) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
        byteBuffer.putLong(uuid.getMostSignificantBits());
        byteBuffer.putLong(uuid.getLeastSignificantBits());
        return byteBuffer.array();
    }

    public static UUID fromBytes(byte[] array) {
        if (array.length != 16) {
            throw new IllegalArgumentException("Illegal byte array length: " + array.length);
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(array);
        long mostSignificant = byteBuffer.getLong();
        long leastSignificant = byteBuffer.getLong();
        return new UUID(mostSignificant, leastSignificant);
    }

    public static UUID getUUIDOf(String name) throws Exception {
        return new UUIDFetcher(Arrays.asList(name)).call().get(name);
    }

    public Map<String, UUID> call() throws Exception {
        Map<String, UUID> uuidMap = new HashMap<String, UUID>();
        int requests = (int) Math.ceil((double) names.size() / PROFILES_PER_REQUEST);
        for (int i = 0; i < requests; i++) {
            HttpURLConnection connection = createConnection();
            try {
                String body = gson
                        .toJson(names.subList(i * PROFILES_PER_REQUEST,
                                Math.min((i + 1) * PROFILES_PER_REQUEST, names.size())));
                writeBody(connection, body);

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new IllegalStateException(
                            "Mojang profile lookup failed with HTTP " + responseCode);
                }

                JsonArray array;
                try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(),
                        StandardCharsets.UTF_8)) {
                    JsonElement parsed = JsonParser.parseReader(reader);
                    if (!parsed.isJsonArray())
                        return uuidMap;
                    array = parsed.getAsJsonArray();
                }

                for (JsonElement element : array) {
                    JsonObject profile = element.getAsJsonObject();
                    String id = profile.get("id").getAsString();
                    String name = profile.get("name").getAsString();
                    uuidMap.put(name, getUUID(id));
                }
            } finally {
                connection.disconnect();
            }

            if (rateLimiting && i != requests - 1) {
                Thread.sleep(100L);
            }
        }
        return uuidMap;
    }

    private void writeBody(HttpURLConnection connection, String body) throws Exception {
        try (OutputStream stream = connection.getOutputStream()) {
            stream.write(body.getBytes(StandardCharsets.UTF_8));
            stream.flush();
        }
    }
}
