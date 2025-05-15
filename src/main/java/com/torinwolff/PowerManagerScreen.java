package com.torinwolff;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class PowerManagerScreen implements Screen {
    private final Main game;
    private final GameClient client;
    private final Stage stage;
    private final Skin skin;

    public PowerManagerScreen(Main game, GameClient client) {
        this.game = game;
        this.client = client;
        stage = new Stage();
        skin = new Skin(Gdx.files.internal("assets/uiskin.json"));
    }

    @Override
    public void show() {
        System.out.println("Power Manager Screen");
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 1, 0, 1); // Set background to green
        Gdx.gl.glClear(Gdx.gl.GL_COLOR_BUFFER_BIT);
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