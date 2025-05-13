package com.torinwolff;

public class DeathMessage {
    public int playerId;
    public String deadUsername;

    public DeathMessage() {}

    public DeathMessage(int playerId, String deadUsername) {
        this.playerId = playerId;
        this.deadUsername = deadUsername;
    }
}