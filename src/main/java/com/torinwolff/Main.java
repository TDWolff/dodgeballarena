package com.torinwolff;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;

public class Main extends Game {
    private GameClient gameClient;

    @Override
    public void create() {
        // Initialize the GameClient
        gameClient = new GameClient();

        // Set the initial screen to MainMenuScreen
        this.setScreen(new MainMenuScreen(this, gameClient));
    }

    @Override
    public void render() {
        // Clear the screen with a black background
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Delegate rendering to the current screen
        super.render();
    }

    @Override
    public void dispose() {
        // Dispose of the current screen
        if (getScreen() != null) {
            getScreen().dispose();
        }

        // Stop the GameClient
        if (gameClient != null) {
            gameClient.stop();
        }
    }

    public static void main(String[] args) {
        // Launch the game using LwjglApplication
        com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration config = new com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration();
        config.title = "Dodgeball Arena";
        config.width = 800; // Set the window width
        config.height = 600; // Set the window height
        new com.badlogic.gdx.backends.lwjgl.LwjglApplication(new Main(), config);
    }
}