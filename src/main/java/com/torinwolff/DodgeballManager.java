package com.torinwolff;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

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
            // Apply gravity
            ball.velocityY += gravity * delta;
            // Update position
            ball.y += ball.velocityY * delta;
            // Collision with floor
            if (ball.y <= floorY) {
                ball.y = floorY;
                ball.velocityY = 0;
            }
        }
    }

    public void render(ShapeRenderer shapeRenderer) {
        shapeRenderer.setColor(1, 0, 0, 1); // Red
        for (DodgeballState ball : dodgeballs) {
            shapeRenderer.circle(ball.x + ball.width / 2, ball.y + ball.height / 2, ball.width / 2);
        }
    }
}