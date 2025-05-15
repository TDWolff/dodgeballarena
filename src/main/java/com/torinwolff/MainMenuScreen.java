package com.torinwolff;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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
    private static final Texture background = new Texture(Gdx.files.internal("assets/background.png"));

    public String acceptedUsername;

    public MainMenuScreen(Main game, GameClient client) {
        this.game = game;
        this.client = client;
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // Load the default skin
        skin = new Skin(Gdx.files.internal("assets/uiskin.json"));

        // Create a TextField for username input
        TextField usernameField = new TextField("", skin);
        usernameField.setMessageText("Enter your username"); // Always set the message text
        if (client.mainMenuUsername != null && !client.mainMenuUsername.isEmpty()) {
            usernameField.setText(client.mainMenuUsername);
        }

        TextButton powerManagerField = new TextButton("Power Manager", skin);

        powerManagerField.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                client.mainMenuUsername = usernameField.getText();
                game.setScreen(new PowerManagerScreen(game, client));
            }
        });

        // Create a "Play" button
        TextButton playButton = new TextButton("Play", skin);

        debugLabel = new Label("", skin);
        debugLabel.setVisible(false);

        playButton.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                stage.setKeyboardFocus(null);
                stage.unfocusAll();   
                String username = usernameField.getText();
                if (!username.isEmpty()) {
                    new Thread(() -> {
                        try {
                            // Attempt to connect the client to the server
                            System.out.println("Connecting to the server...");
                            client.start();

                            while (!client.isReadyToSendUsername()) {
                                Thread.sleep(100); // Wait for 100ms before checking again
                            }
        
                            // Once connected, send the username
                            System.out.println("Connected to the server. Sending username...");
                            client.sendUsername(username);
        
                            Gdx.app.postRunnable(() -> {
                                try {
                                    game.setScreen(new GameScreen(game, username, client));
                                } catch (Exception e) {
                                    System.err.println("Failed to switch to GameScreen: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            });                        
                        } catch (Exception e) {
                            System.err.println("Failed to connect to the server: " + e.getMessage());
                            Gdx.app.postRunnable(() -> {
                                debugLabel.setText("Failed to connect to the server. Please try a different username and try again.");
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
        table.add(powerManagerField).width(200).height(50).padBottom(20); // Add padding below the TextField
        table.row(); // Move to the next row
        table.add(playButton).width(200).height(50);
        table.row(); // Move to the next row
        table.add(debugLabel).padTop(20);

        // Add the table to the stage
        stage.addActor(table);
    }

    @Override
    public void show() {}

    private SpriteBatch spriteBatch = new SpriteBatch();
    
    private float bgOffsetX = 0;
    private float bgOffsetY = 0;
    private float bgVelX = 60f; // pixels per second
    private float bgVelY = 40f; // pixels per second
    
    @Override
    public void render(float delta) {
        // Update background offset
        bgOffsetX += bgVelX * delta;
        bgOffsetY += bgVelY * delta;
    
        int viewportWidth = Gdx.graphics.getWidth();
        int viewportHeight = Gdx.graphics.getHeight();
    
        // Use scale = 1.0f for 1:1, or <1.0f to "zoom out"
        float scale = 2.0f;
        int regionWidth = (int)(viewportWidth * scale);
        int regionHeight = (int)(viewportHeight * scale);
    
        // Clamp regionWidth/regionHeight to not exceed texture size
        if (regionWidth > background.getWidth()) regionWidth = background.getWidth();
        if (regionHeight > background.getHeight()) regionHeight = background.getHeight();
    
        // Clamp and bounce
        float maxOffsetX = background.getWidth() - regionWidth;
        float maxOffsetY = background.getHeight() - regionHeight;
    
        // Start at top-left of the image
        if (bgOffsetX == 0 && bgOffsetY == 0) {
            bgOffsetX = 0;
            bgOffsetY = 0;
        }
    
        // Bounce logic
        if (bgOffsetX < 0) {
            bgOffsetX = 0;
            bgVelX = Math.abs(bgVelX);
        } else if (bgOffsetX > maxOffsetX) {
            bgOffsetX = maxOffsetX;
            bgVelX = -Math.abs(bgVelX);
        }
    
        if (bgOffsetY < 0) {
            bgOffsetY = 0;
            bgVelY = Math.abs(bgVelY);
        } else if (bgOffsetY > maxOffsetY) {
            bgOffsetY = maxOffsetY;
            bgVelY = -Math.abs(bgVelY);
        }
    
        int regionX = (int)Math.min(bgOffsetX, maxOffsetX);
        int regionY = (int)Math.min(bgOffsetY, maxOffsetY);
    
        spriteBatch.begin();
        spriteBatch.draw(
            background,
            0, 0, // draw at (0,0) in the window
            viewportWidth, viewportHeight, // size to draw on screen
            regionX, regionY,              // region x, y in texture
            regionWidth, regionHeight,     // region width, height in texture
            false, false // flipX, flipY
        );
        spriteBatch.end();
        // Draw the UI
        stage.act(delta);
        stage.draw();
    }
    
    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        spriteBatch.dispose();
        background.dispose();
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
}