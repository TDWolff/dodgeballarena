package com.torinwolff;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class DeathScreen  implements Screen {
    private final Main game;
    private final GameClient client;
    private final OrthographicCamera camera;
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont font;
    private final SpriteBatch spriteBatch;
    private final String username;
    
    private final String deadUsername;

    public DeathScreen(Main game, String username, GameClient client, String deadUsername) {
        this.game = game;
        this.username = username;
        this.client = client;
        this.deadUsername = deadUsername;
    
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
    
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        spriteBatch = new SpriteBatch();
    }

    @Override
    public void show() {
        // Called when this screen becomes the current screen
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 1, 0, 1); // Green color
        shapeRenderer.rect(0, 0, 800, 600);
        shapeRenderer.end();
    }

    @Override
    public void render(float delta) {
        camera.update();
        spriteBatch.setProjectionMatrix(camera.combined);
    
        spriteBatch.begin();
        if (username.equals(deadUsername)) {
            font.draw(spriteBatch, "You have died! Press R to go back to main menu.", 300, 300);
            // handle logic for when player presses R, send them to main menu
            if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                game.setScreen(new MainMenuScreen(game, client));
            }
        } else {
            font.draw(spriteBatch, "You have won! Press R to go back to main menu.", 300, 300);
            if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                game.setScreen(new MainMenuScreen(game, client));
            }
        }
        spriteBatch.end();
    }
    
    @Override
    public void resize(int width, int height) {
        camera.setToOrtho(false, width, height);
    }

    @Override
    public void pause() {
        // Called when the game is paused
    }

    @Override
    public void resume() {
        // Called when the game is resumed
    }

    @Override
    public void hide() {
        // Called when this screen is no longer the current screen
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        font.dispose();
        spriteBatch.dispose();
    }
}