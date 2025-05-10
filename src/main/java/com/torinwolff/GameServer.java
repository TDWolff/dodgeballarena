package com.torinwolff;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Server;

public class GameServer {
    private Server server;
    private final ConcurrentHashMap<Integer, PlayerState> worldState = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, String> playerUsernames = new ConcurrentHashMap<>();
    private int nextPlayerId = 1; // Counter for assigning unique player IDs

    private DodgeballManager dodgeballManager = new DodgeballManager();
    private Thread dodgeballSpawnerThread; // Track the spawner thread

    public void start() throws IOException {
        server = new Server();
        server.start();
        server.bind(54555, 54777);

        // Register classes for serialization
        Kryo kryo = server.getKryo();
        kryo.register(String.class);
        kryo.register(PlayerState.class);
        kryo.register(DodgeballState.class); // Register DodgeballState
        kryo.register(java.util.ArrayList.class); // If sending lists of dodgeballs
        kryo.register(ConcurrentHashMap.class);

        // Add a listener to handle incoming messages
        server.addListener(new com.esotericsoftware.kryonet.Listener() {
            @Override
            public void connected(com.esotericsoftware.kryonet.Connection connection) {
                // Assign a unique player ID
                int playerId = nextPlayerId++;
                connection.sendTCP("PLAYER_ID:" + playerId);
                System.out.println("Assigned Player ID " + playerId + " to connection " + connection.getID());

                if (server.getConnections().length == 1 && (dodgeballSpawnerThread == null || !dodgeballSpawnerThread.isAlive())) {
                    dodgeballSpawnerThread = new Thread(() -> {
                        while (server.getConnections().length > 0) {
                            try {
                                Thread.sleep(1000); // Spawn every second
                                dodgeballManager.spawnDodgeball(800, 400, 20, 20);
                                server.sendToAllTCP(dodgeballManager.getDodgeballs());
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    });
                    dodgeballSpawnerThread.start();
                }
            }

            @Override
            public void received(com.esotericsoftware.kryonet.Connection connection, Object object) {
                if (object instanceof String) {
                    String message = (String) object;
                    if (message.startsWith("USERNAME:")) {
                        // Extract the username
                        String username = message.substring("USERNAME:".length()).trim();
            
                        if (!username.matches("[a-zA-Z0-9]+")) {
                            System.err.println("Invalid username received: " + username);
                            connection.sendTCP("ERROR: Invalid username. Only letters and numbers are allowed.");
                            return;
                        }
            
                        String uniqueUsername = ensureUniqueUsername(username);
                        playerUsernames.put(connection.getID(), uniqueUsername);
            
                        System.out.println("Received username: " + uniqueUsername + " from connection " + connection.getID());
                        connection.sendTCP("USERNAME_ACCEPTED:" + uniqueUsername);
                    } else {
                        System.err.println("Unknown message format: " + message);
                    }
                } else if (object instanceof PlayerState) {
                    // Handle player state
                    PlayerState state = (PlayerState) object;
                    int playerId = connection.getID();
                    worldState.put(playerId, state);
            
                    // Broadcast the updated world state to all clients
                    server.sendToAllTCP(worldState);

                    server.sendToAllTCP(dodgeballManager.getDodgeballs());

                    // Broadcast the updated player usernames to all clients
                    ConcurrentHashMap<Integer, String> updatedUsernames = new ConcurrentHashMap<>(playerUsernames);
                    server.sendToAllTCP(updatedUsernames);
                }
            }

            @Override
            public void disconnected(com.esotericsoftware.kryonet.Connection connection) {
                int playerId = connection.getID();

                // Remove the player's state and username
                worldState.remove(playerId);
                playerUsernames.remove(playerId);

                System.out.println("Player " + playerId + " disconnected.");

                // Broadcast the updated world state to all clients
                server.sendToAllTCP(worldState);
            }
        });

        System.out.println("Server started on ports 54555 (TCP) and 54777 (UDP).");
    }

    public void stop() {
        if (server != null) {
            server.stop();
        }
    }

    private String ensureUniqueUsername(String username) {
        Random random = new Random();
        String uniqueUsername = username;
    
        // Check if the username already exists
        while (playerUsernames.containsValue(uniqueUsername)) {
            // Generate a random 3-digit number
            int randomNumber = 100 + random.nextInt(900); // Generates a number between 100 and 999
            uniqueUsername = username + randomNumber;
        }
    
        return uniqueUsername;
    }

    public static void main(String[] args) {
        try {
            GameServer server = new GameServer();
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start the server: " + e.getMessage());
        }
    }
}