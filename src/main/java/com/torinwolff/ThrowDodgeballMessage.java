package com.torinwolff;

public class ThrowDodgeballMessage {
    public int playerId;
    public int dodgeballIndex;
    public float velocityX;
    public float velocityY;

    public ThrowDodgeballMessage() {}

    public ThrowDodgeballMessage(int playerId, int dodgeballIndex, float velocityX, float velocityY) {
        this.playerId = playerId;
        this.dodgeballIndex = dodgeballIndex;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
    }
}