package com.torinwolff;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;

public class GameClient {
    private Client client;
    private int playerId = -1;
    private final ConcurrentHashMap<Integer, PlayerState> worldState = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, String> playerUsernames = new ConcurrentHashMap<>();

    private boolean isConnected = false; // Add a flag to track connection status


    public void start() throws IOException {
        client = new Client();
        client.start();

        // Register classes for serialization
        Kryo kryo = client.getKryo();
        kryo.register(String.class); // Register String
        kryo.register(PlayerState.class); // Register PlayerState
        kryo.register(ConcurrentHashMap.class); // Register ConcurrentHashMap
        kryo.register(java.util.HashMap.class); // Ensure compatibility with HashMap if used

        // Connect to the server
        client.connect(5000, "35.163.100.41", 54555, 54777);

        // Add a listener to handle incoming messages
        client.addListener(new com.esotericsoftware.kryonet.Listener() {
            @Override
            public void connected(com.esotericsoftware.kryonet.Connection connection) {
                isConnected = true;
                System.out.println("Client connected to the server.");
            }

            @Override
            public void disconnected(com.esotericsoftware.kryonet.Connection connection) {
                isConnected = false;
                System.out.println("Client disconnected from the server.");
            }

            @Override
            public void received(com.esotericsoftware.kryonet.Connection connection, Object object) {
                if (object instanceof ConcurrentHashMap) {
                    // Determine if the message is a worldState or playerUsernames update
                    ConcurrentHashMap<?, ?> map = (ConcurrentHashMap<?, ?>) object;
            
                    if (!map.isEmpty() && map.values().iterator().next() instanceof PlayerState) {
                        // It's a worldState update
                        @SuppressWarnings("unchecked")
                        ConcurrentHashMap<Integer, PlayerState> updatedState = (ConcurrentHashMap<Integer, PlayerState>) map;
                        synchronized (worldState) {
                            worldState.clear();
                            worldState.putAll(updatedState);
                        }
                    } else if (!map.isEmpty() && map.values().iterator().next() instanceof String) {
                        // It's a playerUsernames update
                        @SuppressWarnings("unchecked")
                        ConcurrentHashMap<Integer, String> updatedUsernames = (ConcurrentHashMap<Integer, String>) map;
                        synchronized (playerUsernames) {
                            // Update the map incrementally
                            for (Integer id : updatedUsernames.keySet()) {
                                playerUsernames.put(id, updatedUsernames.get(id));
                            }
                        }
                    }
                } else if (object instanceof String) {
                    String message = (String) object;
                    if (message.startsWith("PLAYER_ID:")) {
                        playerId = Integer.parseInt(message.substring("PLAYER_ID:".length()));
                        System.out.println("Received Player ID from server: " + playerId);
                    }
                }
            }
        });
    }

    public int getPlayerId() {
        return playerId;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void sendPlayerState(float x, float y) {
        PlayerState playerState = new PlayerState(x, y);
        client.sendTCP(playerState);
    }

    public void sendUsername(String username) {
        if (client != null && client.isConnected()) {
            client.sendTCP("USERNAME:" + username);
        } else {
            throw new IllegalStateException("Client is not connected to the server.");
        }
    }

    public String getUsernameForPlayer(int id) {
        return playerUsernames.getOrDefault(id, "Unknown Player");
    }

    public ConcurrentHashMap<Integer, PlayerState> getWorldState() {
        return worldState;
    }

    public void stop() {
        if (client != null) {
            client.stop();
        }
    }
}