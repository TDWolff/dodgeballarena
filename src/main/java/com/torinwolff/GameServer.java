package com.torinwolff;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Server;

public class GameServer {
    private Server server;
    private final ConcurrentHashMap<Integer, PlayerState> worldState = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, String> playerUsernames = new ConcurrentHashMap<>();
    private final java.util.Set<Integer> deadPlayers = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
    private int nextPlayerId = 1; // Counter for assigning unique player IDs

    private DodgeballManager dodgeballManager = new DodgeballManager();
    private Thread dodgeballSpawnerThread; // Track the spawner thread

    public void start() throws IOException {
        server = new Server();
        server.start();
        server.bind(54555, 54777);

        try (InputStream is = getClass().getClassLoader().getResourceAsStream("init.txt")) {
            if (is != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            } else {
                System.out.println("init.txt not found in resources.");
            }
        }

        // Register classes for serialization
        Kryo kryo = server.getKryo();
        kryo.register(String.class);
        kryo.register(PlayerState.class);
        kryo.register(DodgeballState.class);
        kryo.register(java.util.ArrayList.class);
        kryo.register(ConcurrentHashMap.class);
        kryo.register(PickupDodgeballMessage.class);
        kryo.register(ThrowDodgeballMessage.class);
        kryo.register(DeathMessage.class);
        kryo.register(DoubleLifeRequestMessage.class);

        dodgeballManager.setFloorY(110);

        server.addListener(new com.esotericsoftware.kryonet.Listener() {
            @Override
            public void connected(com.esotericsoftware.kryonet.Connection connection) {
                // Assign a unique player ID
                int playerId = connection.getID();
                connection.sendTCP("PLAYER_ID:" + playerId);
                System.out.println("Assigned Player ID " + playerId + " to connection " + connection.getID());

                if (server.getConnections().length == 1 && (dodgeballSpawnerThread == null || !dodgeballSpawnerThread.isAlive())) {
                    dodgeballSpawnerThread = new Thread(() -> {
                        long lastTime = System.currentTimeMillis();
                        float spawnTimer = 0f;
                        while (server.getConnections().length > 0) {
                            try {
                                Thread.sleep(16); // ~60 FPS update rate
                                long now = System.currentTimeMillis();
                                float delta = (now - lastTime) / 1000f;
                                lastTime = now;
                        
                                dodgeballManager.update(delta);
                        
                                // Spawn a new dodgeball every 1 second
                                spawnTimer += delta;
                                if (spawnTimer >= 1f) {
                                    dodgeballManager.spawnDodgeball(800, 500, 32, 32); // Example values: mapWidth=800, y=500, width=32, height=32
                                    spawnTimer = 0f;
                                }
                        
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
                    
                        // Check for duplicate username
                        if (playerUsernames.containsValue(username)) {
                            System.err.println("Duplicate username attempted: " + username);
                            connection.sendTCP("ERROR: Username already taken. Please choose another.");
                            return;
                        }
                    
                        playerUsernames.put(connection.getID(), username);
                    
                        System.out.println("Received username: " + username + " from connection " + connection.getID());
                        connection.sendTCP("USERNAME_ACCEPTED:" + username);
                    } else {
                        System.err.println("Unknown message format: " + message);
                    }
                } else if (object instanceof PlayerState) {
                    // Handle player state
                    PlayerState state = (PlayerState) object;
                    int playerId = connection.getID();
                    PlayerState serverState = worldState.get(playerId);
                    if (serverState != null) {
                        serverState.x = state.x;
                        serverState.y = state.y;
                    } else {
                        state.doubleLife = false;
                        state.isAlive = true;
                        worldState.put(playerId, state);
                    }

                    for (DodgeballState ball : dodgeballManager.getDodgeballs()) {
                        if (ball.heldByPlayerId == playerId) {
                            float ballWidth = ball.width;
                            float playerWidth = 50;  // Use your actual player width
                            float playerHeight = 50; // Use your actual player height
                            ball.x = state.x + (playerWidth - ballWidth) / 2f;
                            ball.y = state.y + playerHeight + 10;
                        }
                        if (ball.heldByPlayerId == -1 && ball.isInAir) {
                            long now = System.currentTimeMillis();
                                                        for (Map.Entry<Integer, PlayerState> entry : worldState.entrySet()) {
                                int otherPlayerId = entry.getKey();
                                PlayerState player = entry.getValue();
                            
                                if (otherPlayerId == ball.lastThrowerId && now - ball.lastThrownTimestamp < 300) {
                                    continue;
                                }
                                if (System.currentTimeMillis() < player.invulnerableUntil) {
                                    continue; // Player is invulnerable, skip
                                }

                                if (ball.x < player.x + 50 && ball.x + ball.width > player.x &&
                                    ball.y < player.y + 50 && ball.y + ball.height > player.y) {
                                    System.out.println("Ball hit player: " + otherPlayerId);
                                    System.out.println("doubleLife for player " + otherPlayerId + ": " + player.doubleLife);
                                    if (player.doubleLife) {
                                        System.out.println(player.doubleLife);
                                        player.doubleLife = false;
                                        player.invulnerableUntil = System.currentTimeMillis() + 700; // 1s invuln
                                        ball.isInAir = false;
                                        ball.pickupAvailableTimestamp = System.currentTimeMillis() + 750;
                                        break;
                                    } else {
                                        ball.isInAir = false;
                                        if (player.isAlive) {
                                            player.isAlive = false;
                                            deadPlayers.add(otherPlayerId);
                                            System.out.println("Player " + otherPlayerId + " has died.");
                                        }
                                    
                                        // Count alive players
                                        long aliveCount = worldState.values().stream().filter(p -> p.isAlive).count();
                                        String deadUsername = playerUsernames.get(otherPlayerId);
                                        if (aliveCount == 1) {
                                            server.sendToAllTCP(new DeathMessage(otherPlayerId, deadUsername));
                                        } else {
                                            server.sendToTCP(otherPlayerId, new DeathMessage(otherPlayerId, deadUsername));
                                        }
                                    
                                        ball.pickupAvailableTimestamp = System.currentTimeMillis() + 750;
                                        break;
                                    }
                                }
                            }
                        }
                    }
            
                    // Broadcast the updated world state to all clients
                    server.sendToAllTCP(worldState);

                    server.sendToAllTCP(dodgeballManager.getDodgeballs());

                    // Broadcast the updated player usernames to all clients
                    ConcurrentHashMap<Integer, String> updatedUsernames = new ConcurrentHashMap<>(playerUsernames);
                    server.sendToAllTCP(updatedUsernames);
                }
                if (object instanceof PickupDodgeballMessage) {
                    PickupDodgeballMessage msg = (PickupDodgeballMessage) object;
                    DodgeballState ball = dodgeballManager.getDodgeballs().get(msg.dodgeballIndex);
                    if (ball.heldByPlayerId == -1 && !ball.isInAir && System.currentTimeMillis() >= ball.pickupAvailableTimestamp) {
                        // Ensure player doesn't already hold a ball
                        boolean alreadyHolding = false;
                        for (DodgeballState b : dodgeballManager.getDodgeballs()) {
                            if (b.heldByPlayerId == msg.playerId) {
                                alreadyHolding = true;
                                break;
                            }
                        }
                        if (!alreadyHolding) {
                            ball.heldByPlayerId = msg.playerId;
                
                            // Position the dodgeball above the player's head
                            PlayerState holder = worldState.get(msg.playerId);
                            if (holder != null) {
                                float ballWidth = ball.width;
                                float playerWidth = 50;
                                float playerHeight = 50;
                                // Center the ball horizontally above the player, and a bit above their head
                                ball.x = holder.x + (playerWidth - ballWidth) / 2f;
                                ball.y = holder.y + playerHeight + 10; // 10 pixels above head
                            } else {
                                System.err.println("Player not found for ID: " + msg.playerId);
                            }
                
                            // Broadcast updated dodgeballs to all clients
                            server.sendToAllTCP(dodgeballManager.getDodgeballs());
                        }
                    }
                }
                if (object instanceof ThrowDodgeballMessage) {
                    ThrowDodgeballMessage msg = (ThrowDodgeballMessage) object;
                    DodgeballState ball = dodgeballManager.getDodgeballs().get(msg.dodgeballIndex);
                    if (ball.heldByPlayerId == msg.playerId) {
                        ball.heldByPlayerId = -1;
                        ball.isInAir = true;
                        ball.velocityY = msg.velocityY;
                        ball.velocityX = msg.velocityX;
                        ball.lastThrowerId = msg.playerId;
                        ball.lastThrownTimestamp = System.currentTimeMillis();
                        server.sendToAllTCP(dodgeballManager.getDodgeballs());
                    }
                }
                if (object instanceof DoubleLifeRequestMessage) {
                    DoubleLifeRequestMessage msg = (DoubleLifeRequestMessage) object;
                    PlayerState player = worldState.get(msg.playerId);
                    System.out.println("Double life request from player: " + msg.playerId);
                    if (player != null) {
                        player.doubleLife = true;
                        System.out.println("Double life granted to player: " + msg.playerId);
                    }
                }
            }

            @Override
            public void disconnected(com.esotericsoftware.kryonet.Connection connection) {
                int playerId = connection.getID();
                for (DodgeballState ball : dodgeballManager.getDodgeballs()) {
                    if (ball.heldByPlayerId == playerId) {
                        ball.heldByPlayerId = -1;
                        ball.velocityY = 0;
                    }
                }
            
                worldState.remove(playerId);
                playerUsernames.remove(playerId);
                deadPlayers.remove(playerId);
            
                // Broadcast updated dodgeballs to all clients
                server.sendToAllTCP(dodgeballManager.getDodgeballs());
            
                if (server.getConnections().length == 0) {
                    worldState.clear();
                    playerUsernames.clear();
                    deadPlayers.clear();
                    dodgeballManager.getDodgeballs().clear();
                    nextPlayerId = 1; // <-- Add this line to reset player ID assignment
                    System.out.println("All players disconnected. Game state reset.");
                }
            }
        });

        System.out.println("Server started on ports 54555 (TCP) and 54777 (UDP).");
    }

    public void stop() {
        if (server != null) {
            server.stop();
        }
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