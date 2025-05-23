package com.torinwolff;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;

public class GameScreen implements Screen {
    private final Main game;
    private final GameClient client;
    private final OrthographicCamera camera;
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont font;
    private final SpriteBatch spriteBatch;
    private final MainPlayer player;
    private final String username;
    public String displayName;

    private static final float GRAVITY = -10.8f * 100;
    private static final float JUMP_VELOCITY = 500;
    private float playerVelocityY = 0;
    private boolean isJumping = false;
    boolean hasSent = false;

    private final DodgeballManager dodgeballManager = new DodgeballManager();    
    private boolean[] powerUps = PowerManagerScreen.powerUps;
    private Texture dodgeballTexture;

    private final Rectangle platform = new Rectangle(0, -90, 800, 200);

    public GameScreen(Main game, String username, GameClient client) {
        this.game = game;
        this.username = username;
        this.client = client;

        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);

        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        spriteBatch = new SpriteBatch();

        player = new MainPlayer();
        player.setPlayerX(375); // Starting X position
    }

    @Override
    public void show() {
        displayName = client.getMyUsername();
        System.out.println("Player ID: " + client.getPlayerId());
        System.out.println("Welcome " + displayName + " to the game!");
        dodgeballTexture = new Texture(Gdx.files.internal("assets/dodgeball.png"));

        applyCustomCursor();

        Gdx.input.setInputProcessor(new com.badlogic.gdx.InputAdapter() {
            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                applyCustomCursor();
                return false;
            }
        });

        int retryCount = 0;
        while (!client.isConnected() && retryCount < 5) {
            System.err.println("Client is not connected. Retrying...");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            retryCount++;
        }

        if (!client.isConnected()) {
            System.err.println("Failed to connect to the server after multiple attempts.");
        } else {
            System.out.println("Client successfully connected!");
        }
    }

    private void applyCustomCursor() {
        Pixmap originalPixmap = new Pixmap(Gdx.files.internal("assets/crosshair.png"));
        int scaledWidth = originalPixmap.getWidth() / 2;
        int scaledHeight = originalPixmap.getHeight() / 2;
        Pixmap scaledPixmap = new Pixmap(scaledWidth, scaledHeight, originalPixmap.getFormat());

        scaledPixmap.drawPixmap(originalPixmap,
                0, 0, originalPixmap.getWidth(), originalPixmap.getHeight(),
                0, 0, scaledWidth, scaledHeight);

        Cursor cursor = Gdx.graphics.newCursor(scaledPixmap, scaledWidth / 2, scaledHeight / 2);
        Gdx.graphics.setCursor(cursor);

        originalPixmap.dispose();
        scaledPixmap.dispose();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.86f, 0.86f, 0.86f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        spriteBatch.begin();
        for (DodgeballState ball : dodgeballManager.getDodgeballs()) {
            spriteBatch.draw(dodgeballTexture, ball.x, ball.y, ball.width, ball.height);
        }
        spriteBatch.end();

        if (client.isDead || client.isSpectating) {
            // Remove player from world state
            ConcurrentHashMap<Integer, PlayerState> worldState = client.getWorldState();
            int playerId = client.getPlayerId();
            if (worldState != null) {
                worldState.remove(playerId);
            }
            List<DodgeballState> balls = client.getDodgeballs();
            synchronized (balls) {
                for (DodgeballState ball : balls) {
                    if (ball.heldByPlayerId == playerId) {
                        ball.heldByPlayerId = -1;
                    }
                }
            }
            Gdx.app.postRunnable(() -> {
                try {
                    game.setScreen(new DeathScreen(game, displayName, client, client.deadUsername));
                } catch (Exception e) {
                    System.err.println("Failed to switch to DeathScreen: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            client.isDead = false;
            client.isSpectating = false;
            client.stop();
            return; // Stop further rendering
        }

        camera.update();

        // Draw platform and player
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.7f, 0.2f, 1); // Platform - green
        shapeRenderer.rect(platform.x, platform.y, platform.width, platform.height);
        shapeRenderer.setColor(0.1f, 0.3f, 1f, 1); // Player - blue
        Rectangle playerBounds = player.getBoundingRectangle();
        shapeRenderer.rect(playerBounds.x, playerBounds.y, playerBounds.width, playerBounds.height);
        shapeRenderer.end();

        // Apply gravity
        playerVelocityY += GRAVITY * delta;

        // Collision
        handleCollisions(delta);

        // Jump
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.SPACE) && !isJumping) {
            switch (PowerManagerScreen.powerLevels[0]) {
                case 3:
                    playerVelocityY = JUMP_VELOCITY * 1.9f;
                    isJumping = true;
                    break;
                case 2:
                    playerVelocityY = JUMP_VELOCITY * 1.6f;
                    isJumping = true;
                    break;
                case 1:
                    playerVelocityY = JUMP_VELOCITY * 1.3f;
                    isJumping = true;
                    break;
                default:
                    playerVelocityY = JUMP_VELOCITY;
                    isJumping = true;
                    break;
            }
        }

        // Horizontal movement
        float speedMultiplier;
        switch (PowerManagerScreen.powerLevels[1]) {
            case 3:
                speedMultiplier = 1.9f;
                break;
            case 2:
                speedMultiplier = 1.6f;
                break;
            case 1:
                speedMultiplier = 1.3f;
                break;
            default:
                speedMultiplier = 1.0f;
                break;
        }
        float moveSpeed = 200 * speedMultiplier;
        
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.A)) {
            player.setPlayerX(player.getPlayerX() - moveSpeed * delta);
        }
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.D)) {
            player.setPlayerX(player.getPlayerX() + moveSpeed * delta);
        }

        List<DodgeballState> receivedDodgeballs = client.getDodgeballs();
        synchronized (receivedDodgeballs) {
            dodgeballManager.getDodgeballs().clear();
            dodgeballManager.getDodgeballs().addAll(receivedDodgeballs);
        }

        int playerId = client.getPlayerId();
        boolean alreadyHolding = false;
        for (DodgeballState ball : dodgeballManager.getDodgeballs()) {
            if (ball.heldByPlayerId == playerId) {
                alreadyHolding = true;
                break;
            }
        }

        if (!alreadyHolding) {
            Rectangle playerRect = player.getBoundingRectangle();
            List<DodgeballState> balls = dodgeballManager.getDodgeballs();
            for (int i = 0; i < balls.size(); i++) {
                DodgeballState ball = balls.get(i);
                if (ball.heldByPlayerId == -1 &&
                    playerRect.overlaps(new Rectangle(ball.x, ball.y, ball.width, ball.height))) {
                    client.sendPickupDodgeball(playerId, i);
                    break;
                }
            }
        }

        List<DodgeballState> balls = dodgeballManager.getDodgeballs();

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            // Find held ball
            for (int i = 0; i < balls.size(); i++) {
                DodgeballState ball = balls.get(i);
                if (ball.heldByPlayerId == playerId) {
                    // Get cursor world coordinates
                    Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
                    camera.unproject(mouse);
        
                    float playerCenterX = player.getPlayerX() + 50 / 2f;
                    float playerCenterY = player.getPlayerY() + 50 / 2f;
                    float dx = mouse.x - playerCenterX;
                    float dy = mouse.y - playerCenterY;
        
                    float magnitude = (float)Math.sqrt(dx*dx + dy*dy);
                    float speed = 800f; // Tune this value for throw power
                    float vx = dx / magnitude * speed;
                    float vy = dy / magnitude * speed;
        
                    client.sendThrowDodgeball(playerId, i, vx, vy);
                    break;
                }
            }
        }

        // Render dodgeballs
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        shapeRenderer.end();

        // Send player state
        client.sendPlayerState(player.getPlayerX(), player.getPlayerY());

        if (!hasSent && powerUps[2]) {
            client.sendDoubleLifeRequest(playerId);
            hasSent = true;
        }


        // Render other players
        ConcurrentHashMap<Integer, PlayerState> worldState = client.getWorldState();
        if (worldState != null) {
            synchronized (worldState) {
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                for (PlayerState state : worldState.values()) {
                    if (state != null) {
                        shapeRenderer.setColor(0, 0, 1, 1); // Blue for other players
                        shapeRenderer.rect(state.x, state.y, player.getPlayerWidth(), player.getPlayerHeight());
                    }
                }
                shapeRenderer.end();
                spriteBatch.setProjectionMatrix(camera.combined);
                spriteBatch.begin();
                font.setColor(0, 0, 0, 1);
                for (Integer id : worldState.keySet()) {
                    PlayerState state = worldState.get(id);
                    if (state != null) {
                        String displayName = client.getUsernameForPlayer(id);
                        if (displayName != null) {
                            font.draw(spriteBatch, displayName,
                                    state.x + player.getPlayerWidth() / 4,
                                    state.y + player.getPlayerHeight() + 20);
                        }
                    }
                }
                spriteBatch.end();
            }
        }
    }

    private void handleCollisions(float delta) {
        Rectangle playerBounds = player.getBoundingRectangle();
    
        // Predict the new Y position
        float predictedY = player.getPlayerY() + playerVelocityY * delta;
        Rectangle predictedBounds = new Rectangle(playerBounds.x, predictedY, playerBounds.width, playerBounds.height);
    
        if (platform.overlaps(predictedBounds) && playerVelocityY < 0) {
            // Land smoothly on platform
            player.setPlayerY(platform.y + platform.height);
            playerVelocityY = 0;
            isJumping = false;
        } else {
            // No collision, apply gravity
            player.setPlayerY(predictedY);
        }
    }    

    @Override
    public void resize(int width, int height) {
        camera.setToOrtho(false, width, height);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {
        applyCustomCursor();
    }

    @Override
    public void hide() {
        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
    }

    @Override
    public void dispose() {
        if (dodgeballTexture != null) {
            dodgeballTexture.dispose();
        }
        shapeRenderer.dispose();
        font.dispose();
        spriteBatch.dispose();
    }
}
