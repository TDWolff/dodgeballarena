package com.torinwolff;

import java.util.concurrent.ConcurrentHashMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class GameScreen implements Screen {
    private final Main game;
    private final GameClient client;
    private final OrthographicCamera camera;
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont font;
    private final SpriteBatch spriteBatch;
    private final MainPlayer player;
    private final String username;

    public GameScreen(Main game, String username, GameClient client) {
        this.game = game;
        this.username = username;
        this.client = client;

        // Initialize camera and rendering tools
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);

        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        spriteBatch = new SpriteBatch();

        // Initialize the player
        player = new MainPlayer();
        player.setPlayerX(375); // Default starting position
    }

    @Override
    public void show() {
        System.out.println("Welcome " + username + " to the game!");
    
        int retryCount = 0;
        while (!client.isConnected() && retryCount < 5) {
            System.err.println("Client is not connected. Retrying...");
            try {
                Thread.sleep(500); // Wait for 500ms before retrying
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            retryCount++;
        }
    
        if (!client.isConnected()) {
            System.err.println("Failed to connect to the server after multiple attempts.");
            // Optionally, transition back to the main menu or show an error message
        } else {
            System.out.println("Client successfully connected!");
        }
    }

    @Override
    public void render(float delta) {
        // Clear the screen with a black background
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    
        // Update the camera
        camera.update();
    
        // Handle input for horizontal movement
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.A)) {
            player.setPlayerX(player.getPlayerX() - 200 * delta); // Move left
        }
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.D)) {
            player.setPlayerX(player.getPlayerX() + 200 * delta); // Move right
        }
    
        // Send the player's position to the server
        client.sendPlayerState(player.getPlayerX(), player.getPlayerY());
    
        // Render all players in the shared world state
        ConcurrentHashMap<Integer, PlayerState> worldState = client.getWorldState();
        if (worldState != null) { // Check if worldState is null
            synchronized (worldState) { // Synchronize access to avoid concurrent modification issues
                // Render player rectangles
                shapeRenderer.setProjectionMatrix(camera.combined);
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                for (PlayerState state : worldState.values()) {
                    if (state != null) { // Check if PlayerState is null
                        shapeRenderer.setColor(0, 0, 1, 1); // Blue color for all players
                        shapeRenderer.rect(state.x, state.y, player.getPlayerWidth(), player.getPlayerHeight());
                    }
                }
                shapeRenderer.end();
    
                // Render player names
                spriteBatch.setProjectionMatrix(camera.combined);
                spriteBatch.begin();
                for (Integer id : worldState.keySet()) {
                    PlayerState state = worldState.get(id);
                    if (state != null) {
                        String displayName = client.getUsernameForPlayer(id); // Get the username for the player
                        if (displayName != null) {
                            font.draw(spriteBatch, displayName, state.x + player.getPlayerWidth() / 4, state.y + player.getPlayerHeight() + 20);
                        }
                    }
                }
                spriteBatch.end();
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        camera.setToOrtho(false, width, height);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        font.dispose();
        spriteBatch.dispose();
    }
}