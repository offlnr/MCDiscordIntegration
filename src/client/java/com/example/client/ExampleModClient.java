package com.example.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleModClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("discordrpcmod");
    private static final String CLIENT_ID = "1549864179509366844";
    private SimpleDiscordRPC rpc;
    private long lastTick = 0;
    private long startTimestamp;
    private boolean isConnected = false;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Simple Discord RPC...");
        
        rpc = new SimpleDiscordRPC(CLIENT_ID);
        startTimestamp = System.currentTimeMillis();

        new Thread(() -> {
            LOGGER.info("Connecting to Discord...");
            if (rpc.connect()) {
                isConnected = true;
                LOGGER.info("Discord RPC connected successfully!");
            } else {
                LOGGER.error("Could not connect to Discord RPC pipe.");
            }
        }).start();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (isConnected && lastTick++ % 100 == 0) {
                updatePresence(client);
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (isConnected) updatePresence(client);
        });
    }

    private void updatePresence(Minecraft client) {
        String details = "Jugando en Multijugador";
        String state = "";
        
        if (client.level == null) {
            details = "En el menú principal";
        } else if (client.hasSingleplayerServer()) {
            details = "Jugando en Solitario";
            state = "";
        } else {
            ServerData serverData = client.getCurrentServer();
            if (serverData != null) {
                details = "Jugando en " + serverData.name;
                state = "Multijugador";
            }
        }

        rpc.updatePresence(details, state, "logo", "Minecraft 26.2", startTimestamp);
    }
}