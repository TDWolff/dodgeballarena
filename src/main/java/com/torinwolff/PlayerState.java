package com.torinwolff;

public class PlayerState {
    public float x;
    public float y;
    public boolean isAlive = true;

    public PlayerState() {
    }

    // Constructor with parameters
    public PlayerState(float x, float y, boolean isAlive) {
        this.x = x;
        this.y = y;
        this.isAlive = isAlive;
    }
}