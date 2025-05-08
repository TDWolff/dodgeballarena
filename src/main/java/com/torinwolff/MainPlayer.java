package com.torinwolff;

public class MainPlayer {
    private float playerX; // X position of the cube
    private final float playerY = 100; // Y position of the cube
    private final float playerWidth = 50;
    private final float playerHeight = 50;
    private String connectionId; // Unique identifier for the player

    public void ClientLogic() {

        System.out.println("Client logic running for player with ID: " + connectionId);
    }

    public MainPlayer() {
        this.playerX = 375; // Start in the middle of the screen
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public float getPlayerX() {
        return playerX;
    }

    public void setPlayerX(float playerX) {
        this.playerX = playerX;
    }

    public float getPlayerY() {
        return playerY;
    }

    public float getPlayerWidth() {
        return playerWidth;
    }

    public float getPlayerHeight() {
        return playerHeight;
    }
}