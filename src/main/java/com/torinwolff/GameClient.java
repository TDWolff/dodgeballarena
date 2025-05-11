package com.torinwolff;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;

public class GameClient {
    private Client client;
    private int playerId = -1;
    private final ConcurrentHashMap<Integer, PlayerState> worldState = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, String> playerUsernames = new ConcurrentHashMap<>();

    private boolean isConnected = false; // Add a flag to track connection status
    private boolean playerIdReceived = false; // Add this flag

    public boolean isReadyToSendUsername() {
        return isConnected && playerIdReceived;
    }

    public void sendThrowDodgeball(int playerId, int dodgeballIndex, float velocityX, float velocityY) {
        ThrowDodgeballMessage msg = new ThrowDodgeballMessage(playerId, dodgeballIndex, velocityX, velocityY);
        client.sendTCP(msg);
    }

    private final List<DodgeballState> dodgeballs = new ArrayList<>();

    public List<DodgeballState> getDodgeballs() {
        return dodgeballs;
    }

    public void sendPickupDodgeball(int playerId, int dodgeballIndex) {
        PickupDodgeballMessage msg = new PickupDodgeballMessage(playerId, dodgeballIndex);
        client.sendTCP(msg);
    }

    public void start() throws IOException {
        client = new Client();
        client.start();
    
        // Register classes for serialization
        Kryo kryo = client.getKryo();
        kryo.register(String.class); // Register String
        kryo.register(PlayerState.class); // Register PlayerState
        kryo.register(DodgeballState.class); // Register DodgeballState
        kryo.register(java.util.ArrayList.class); // If sending lists of dodgeballs
        kryo.register(ConcurrentHashMap.class); // Register ConcurrentHashMap
        kryo.register(PickupDodgeballMessage.class);
        kryo.register(ThrowDodgeballMessage.class);
        kryo.register(java.util.HashMap.class); // Ensure compatibility with HashMap if used
    
        // Connect to the server
        client.connect(5000, "localhost", 54555, 54777);
    
        // Add a listener to handle incoming messages
        client.addListener(new com.esotericsoftware.kryonet.Listener() {
            @Override
            public void disconnected(com.esotericsoftware.kryonet.Connection connection) {
                isConnected = false;
                System.out.println("Client disconnected from the server.");
            }
    
            @Override
            public void received(com.esotericsoftware.kryonet.Connection connection, Object object) {
                if (object instanceof ArrayList) {
                    ArrayList<?> list = (ArrayList<?>) object;
                    if (!list.isEmpty() && list.get(0) instanceof DodgeballState) {
                        synchronized (dodgeballs) {
                            dodgeballs.clear();
                            for (Object o : list) {
                                dodgeballs.add((DodgeballState) o);
                            }
                        }
                    }
                } else if (object instanceof ConcurrentHashMap) {
                    ConcurrentHashMap<?, ?> map = (ConcurrentHashMap<?, ?>) object;
                    if (!map.isEmpty() && map.values().iterator().next() instanceof PlayerState) {
                        @SuppressWarnings("unchecked")
                        ConcurrentHashMap<Integer, PlayerState> updatedState = (ConcurrentHashMap<Integer, PlayerState>) map;
                        synchronized (worldState) {
                            worldState.clear();
                            worldState.putAll(updatedState);
                        }
                    } else if (!map.isEmpty() && map.values().iterator().next() instanceof String) {
                        @SuppressWarnings("unchecked")
                        ConcurrentHashMap<Integer, String> updatedUsernames = (ConcurrentHashMap<Integer, String>) map;
                        synchronized (playerUsernames) {
                            playerUsernames.clear();
                            playerUsernames.putAll(updatedUsernames);
                        }
                    }
                }
                if (object instanceof String) {
                    String message = (String) object;
                    if (message.startsWith("PLAYER_ID:")) {
                        if (!isConnected) {
                            isConnected = true;
                        }
                        playerId = Integer.parseInt(message.substring("PLAYER_ID:".length()));
                        playerIdReceived = true;
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
        if (!isReadyToSendUsername()) {
            throw new IllegalStateException("Client is not ready to send username. Wait for connection and player ID.");
        }
        client.sendTCP("USERNAME:" + username);
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