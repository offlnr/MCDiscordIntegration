package com.example.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class SimpleDiscordRPC {
    private RandomAccessFile pipe;
    private final String clientId;

    public SimpleDiscordRPC(String clientId) {
        this.clientId = clientId;
    }

    public boolean connect() {
        for (int i = 0; i < 10; i++) {
            try {
                pipe = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw");
                
                // Send Handshake (Opcode 0)
                JsonObject handshake = new JsonObject();
                handshake.addProperty("v", 1);
                handshake.addProperty("client_id", clientId);
                send(0, handshake.toString());
                
                // Wait for response (Opcode 0)
                read();
                return true;
            } catch (Exception e) {
                pipe = null;
            }
        }
        return false;
    }

    public void updatePresence(String details, String state, String largeImageKey, String largeImageText, long startTimestamp) {
        if (pipe == null) return;
        
        JsonObject activity = new JsonObject();
        if (details != null && !details.isEmpty()) activity.addProperty("details", details);
        if (state != null && !state.isEmpty()) activity.addProperty("state", state);
        
        JsonObject timestamps = new JsonObject();
        timestamps.addProperty("start", startTimestamp);
        activity.add("timestamps", timestamps);

        JsonObject assets = new JsonObject();
        if (largeImageKey != null) assets.addProperty("large_image", largeImageKey);
        if (largeImageText != null) assets.addProperty("large_text", largeImageText);
        activity.add("assets", assets);

        JsonObject args = new JsonObject();
        args.addProperty("pid", ProcessHandle.current().pid());
        args.add("activity", activity);

        JsonObject payload = new JsonObject();
        payload.addProperty("cmd", "SET_ACTIVITY");
        payload.add("args", args);
        payload.addProperty("nonce", "12345");

        try {
            send(1, payload.toString());
            read(); // read response
        } catch (Exception e) {
            pipe = null; // Disconnected
        }
    }

    public void close() {
        if (pipe != null) {
            try {
                pipe.close();
            } catch (Exception e) {}
            pipe = null;
        }
    }

    private void send(int opcode, String payloadString) throws Exception {
        byte[] payload = payloadString.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(8 + payload.length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(opcode);
        buffer.putInt(payload.length);
        buffer.put(payload);
        pipe.write(buffer.array());
    }

    private String read() throws Exception {
        byte[] header = new byte[8];
        pipe.readFully(header);
        ByteBuffer buffer = ByteBuffer.wrap(header);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int opcode = buffer.getInt();
        int length = buffer.getInt();
        
        byte[] data = new byte[length];
        pipe.readFully(data);
        return new String(data, StandardCharsets.UTF_8);
    }
}
