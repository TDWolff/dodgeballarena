package com.torinwolff;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

public class GameScreen implements Screen {
    private final Main game;
    private final GameClient client;
    private final OrthographicCamera camera;
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont font;
    private final SpriteBatch spriteBatch;
    private final MainPlayer player;
    private final String username;

    private static final float GRAVITY = -10.8f * 100;
    private static final float JUMP_VELOCITY = 500;
    private float playerVelocityY = 0;
    private boolean isJumping = false;

    private final DodgeballManager dodgeballManager = new DodgeballManager();

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
        System.out.println("Welcome " + username + " to the game!");

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
        Gdx.gl.glClearColor(0.95f, 0.95f, 0.95f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

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
            playerVelocityY = JUMP_VELOCITY;
            isJumping = true;
        }

        // Horizontal movement
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.A)) {
            player.setPlayerX(player.getPlayerX() - 200 * delta);
        }
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.D)) {
            player.setPlayerX(player.getPlayerX() + 200 * delta);
        }

        List<DodgeballState> receivedDodgeballs = client.getDodgeballs();
        synchronized (receivedDodgeballs) {
            dodgeballManager.getDodgeballs().clear();
            dodgeballManager.getDodgeballs().addAll(receivedDodgeballs);
        }
    
        // Render dodgeballs
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        dodgeballManager.render(shapeRenderer);
        shapeRenderer.end();

        // Send player state
        client.sendPlayerState(player.getPlayerX(), player.getPlayerY());

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

                // ...existing code...
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
                // ...existing code...
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
        shapeRenderer.dispose();
        font.dispose();
        spriteBatch.dispose();
    }
}
