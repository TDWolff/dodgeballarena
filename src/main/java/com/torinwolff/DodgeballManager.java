package com.torinwolff;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DodgeballManager {
    private final List<DodgeballState> dodgeballs = new ArrayList<>();
    private final Random random = new Random();

    private float gravity = -500f; // Default gravity for dodgeballs (pixels/sec^2)
    private float floorY = 0f;     // Y position of the floor

    public void setGravity(float gravity) {
        this.gravity = gravity;
    }

    public void setFloorY(float floorY) {
        this.floorY = floorY;
    }

    public boolean tryPickupDodgeball(float playerX, float playerY, float playerWidth, float playerHeight, int playerId) {
        for (DodgeballState ball : dodgeballs) {
            if (ball.heldByPlayerId == -1 && intersects(playerX, playerY, playerWidth, playerHeight, ball)) {
                ball.heldByPlayerId = playerId;
                return true;
            }
        }
        return false;
    }
    
    private boolean intersects(float x, float y, float w, float h, DodgeballState ball) {
        return x < ball.x + ball.width && x + w > ball.x &&
               y < ball.y + ball.height && y + h > ball.y;
    }

    public List<DodgeballState> getDodgeballs() {
        return dodgeballs;
    }

    public void spawnDodgeball(float mapWidth, float y, float width, float height) {
        if (dodgeballs.size() >= 10) {
            return;
        }
        float x = random.nextFloat() * (mapWidth - width);
        dodgeballs.add(new DodgeballState(x, y, width, height));
    }

    public void update(float delta) {
        for (DodgeballState ball : dodgeballs) {
            if (ball.heldByPlayerId == -1) {
                if (ball.isInAir) {
                    ball.velocityY += gravity * delta;
                    ball.x += ball.velocityX * delta;
                    ball.y += ball.velocityY * delta;
                    if (ball.y <= floorY) {
                        ball.y = floorY;
                        ball.velocityY = 0;
                        ball.velocityX = 0;
                        ball.isInAir = false;
                        ball.pickupAvailableTimestamp = System.currentTimeMillis() + 750;
                    }
                } else {
                    // Ball is on ground
                    if (ball.y > floorY) {
                        ball.velocityY += gravity * delta;
                        ball.y += ball.velocityY * delta;
                        if (ball.y <= floorY) {
                            ball.y = floorY;
                            ball.velocityY = 0;
                        }
                    }
                }
            } else { // Ball is held by a player
                ball.velocityY = 0;
                ball.velocityX = 0;
                ball.isInAir = false;
            }
        }
    }
}