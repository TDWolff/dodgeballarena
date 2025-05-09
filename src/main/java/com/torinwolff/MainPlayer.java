package com.torinwolff;

import com.badlogic.gdx.math.Rectangle;

public class MainPlayer {
    private float playerX; // X position of the cube
    private float playerY; // Y position of the cube
    private final float playerWidth = 50;
    private final float playerHeight = 50;
    private String connectionId; // Unique identifier for the player

    public void ClientLogic() {

        System.out.println("Client logic running for player with ID: " + connectionId);
    }

    public MainPlayer() {
        this.playerX = 375; // Start in the middle of the screen
        this.playerY = 50; // Start above the floor
    }

    // Method to get the bounding rectangle
    public Rectangle getBoundingRectangle() {
        return new Rectangle(playerX, playerY, playerWidth, playerHeight);
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

    public void setPlayerY(float playerY) {
        this.playerY = playerY;
    }

    public float getPlayerWidth() {
        return playerWidth;
    }

    public float getPlayerHeight() {
        return playerHeight;
    }
}