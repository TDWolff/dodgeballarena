package com.torinwolff;

public class DodgeballState {
    public float x;
    public float y;
    public float width;
    public float height;
    public float velocityY; // Add vertical velocity
    public float velocityX;

    public int heldByPlayerId = -1; // -1 means not held
    public boolean isInAir = false;

    public int lastThrowerId = -1;
    public long lastThrownTimestamp = 0; // in milliseconds
    public long pickupAvailableTimestamp = 0; // Time (ms) when ball can next be picked up

    public DodgeballState() {}

    public DodgeballState(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.velocityY = 0;
        this.velocityX = 0;
    }
}