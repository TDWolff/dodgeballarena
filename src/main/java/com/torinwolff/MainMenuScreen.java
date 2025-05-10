package com.torinwolff;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class MainMenuScreen implements Screen {
    private final Main game;
    private final GameClient client;
    private Stage stage;
    private Skin skin;
    private Label debugLabel;

    public MainMenuScreen(Main game, GameClient client) {
        this.game = game;
        this.client = client;
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // Load the default skin
        skin = new Skin(Gdx.files.internal("assets/uiskin.json"));

        // Create a TextField for username input
        TextField usernameField = new TextField("", skin);
        usernameField.setMessageText("Enter your username");

        // Create a "Play" button
        TextButton playButton = new TextButton("Play", skin);

        debugLabel = new Label("", skin);
        debugLabel.setVisible(false);

        playButton.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                stage.unfocusAll();
                String username = usernameField.getText();
                if (!username.isEmpty()) {
                    new Thread(() -> {
                        try {
                            // Attempt to connect the client to the server
                            System.out.println("Connecting to the server...");
                            client.start(); // Start the client and connect to the server
        
                            // Wait for the client to connect
                            while (!client.isConnected()) {
                                Thread.sleep(100); // Wait for 100ms before checking again
                            }
        
                            // Once connected, send the username
                            System.out.println("Connected to the server. Sending username...");
                            client.sendUsername(username);
        
                            Gdx.app.postRunnable(() -> {
                                try {
                                    System.out.println("Switching to GameScreen...");
                                    game.setScreen(new GameScreen(game, username, client));
                                    dispose();
                                    System.out.println("GameScreen set successfully.");
                                } catch (Exception e) {
                                    System.err.println("Failed to switch to GameScreen: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            });                        
                        } catch (Exception e) {
                            System.err.println("Failed to connect to the server: " + e.getMessage());
                            Gdx.app.postRunnable(() -> {
                                debugLabel.setText("Failed to connect to the server.");
                                debugLabel.setVisible(true);
                            });
                        }
                    }).start();
                } else {
                    // Handle empty username case
                    System.out.println("Please enter a valid username.");
                    Gdx.app.postRunnable(() -> {
                        debugLabel.setText("Please enter a valid username.");
                        debugLabel.setVisible(true);
                    });
                }
            }
        });

        // Set up a Table layout to center the elements
        Table table = new Table();
        table.setFillParent(true);
        table.center();

        // Add the TextField and Play button to the table
        table.add(usernameField).width(300).height(50).padBottom(20); // Add padding below the TextField
        table.row(); // Move to the next row
        table.add(playButton).width(200).height(50);
        table.row(); // Move to the next row
        table.add(debugLabel).padTop(20);

        // Add the table to the stage
        stage.addActor(table);
    }

    @Override
    public void show() {}

    @Override
    public void render(float delta) {
        // Update and draw the stage
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}