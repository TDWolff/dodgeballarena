package com.torinwolff;

public class PickupDodgeballMessage {
    public int playerId;
    public int dodgeballIndex; // or a unique ID if you have one

    public PickupDodgeballMessage() {}

    public PickupDodgeballMessage(int playerId, int dodgeballIndex) {
        this.playerId = playerId;
        this.dodgeballIndex = dodgeballIndex;
    }
}