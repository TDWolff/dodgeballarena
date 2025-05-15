package com.torinwolff;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

public class PowerManagerScreen implements Screen {
    private final Main game;
    private final GameClient client;
    private final Stage stage;
    private final Skin skin;
    private int GL_COLOR_BUFFER_BIT = 16384;

    public static final boolean[] powerUps = new boolean[5];
    // powers 0-4: 0 = Super Jump, 1 = Speed Boost, 2 = Double Life, 3 = Invisibility, 4 = Super Dodgeball

    public PowerManagerScreen(Main game, GameClient client) {
        this.game = game;
        this.client = client;
        stage = new Stage();
        skin = new Skin(Gdx.files.internal("assets/uiskin.json"));
    }

    public void SuperJumpToggle() {
        System.out.println("Super Jump toggled!");
        powerUps[0] = true;
    }

    @Override
    public void show() {
        System.out.println("Power Manager Screen");
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 1, 0, 1); // Set background to green
        Gdx.gl.glClear(GL_COLOR_BUFFER_BIT);

        TextButton superJump = new TextButton("Super Jump", skin);
        superJump.setPosition(100, 100);
        superJump.setSize(200, 50);
        superJump.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                System.out.println("Super Jump activated!");
                SuperJumpToggle();
                game.setScreen(new MainMenuScreen(game, client));
            }
        });
        stage.addActor(superJump);
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