package com.torinwolff;

import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class PowerManagerScreen implements Screen {
    private final Stage stage;
    private final Skin skin;

    public static final String[] POWER_NAMES = {
        "Super Jump", "Speed Boost", "Double Life", "Invisibility", "Super Dodgeball"
    };

    // Only these can be doubled up
    private static final boolean[] CAN_BE_STACKED = {true, true, false, false, false};

    // Track how many times each power has been selected
    public static final int[] powerLevels = new int[POWER_NAMES.length];

    public static final boolean[] powerUps = new boolean[5];

    private final Label[] wheelLabels = new Label[3];
    private final int[] wheelIndices = new int[3];
    private final Random random = new Random();

    private boolean spinning = false;

    public PowerManagerScreen(Main game, GameClient client) {
        stage = new Stage(new ScreenViewport());
        skin = new Skin(Gdx.files.internal("assets/uiskin.json"));

        Table table = new Table();
        table.setFillParent(true);
        table.center();

        // Slot machine wheels
        Table wheelsTable = new Table();
        for (int i = 0; i < 3; i++) {
            wheelIndices[i] = random.nextInt(POWER_NAMES.length);
            wheelLabels[i] = new Label(getPowerLabel(wheelIndices[i]), skin);
            wheelLabels[i].setAlignment(Align.center);
            wheelLabels[i].setFontScale(2f);
            wheelsTable.add(wheelLabels[i]).width(200).height(80).pad(20);
        }
        table.add(wheelsTable).colspan(1).row();

        // Spin button
        TextButton spinButton = new TextButton("Spin!", skin);
        table.add(spinButton).width(200).height(60).padTop(40);
        TextButton backButton = new TextButton("Back", skin);
        backButton.setSize(150, 60);
        backButton.setPosition(Gdx.graphics.getWidth() / 2 - backButton.getWidth() / 2, 70);
        backButton.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen(game, client));
            }
        });

        spinButton.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                if (!spinning) {
                    spinning = true;
                    spinButton.setDisabled(true);
                    startSpin(spinButton);
                }
            }
        });

        stage.addActor(table);
        stage.addActor(backButton);
    }

    private void startSpin(TextButton spinButton) {
        final int[] spinTimes = {random.nextInt(20) + 20, random.nextInt(20) + 30, random.nextInt(20) + 40};
        final int[] counters = {0, 0, 0};

        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                boolean allStopped = true;
                for (int i = 0; i < 3; i++) {
                    if (counters[i] < spinTimes[i]) {
                        counters[i]++;
                        wheelIndices[i] = random.nextInt(POWER_NAMES.length);
                        wheelLabels[i].setText(getPowerLabel(wheelIndices[i]));
                        allStopped = false;
                    }
                }
                if (allStopped) {
                    this.cancel();
                    spinning = false;
                    spinButton.setDisabled(false);
                    // Reset power levels for this spin
                    for (int i = 0; i < powerLevels.length; i++) powerLevels[i] = 0;
                    // Count up the selected powers
                    for (int i = 0; i < 3; i++) {
                        powerLevels[wheelIndices[i]]++;
                    }
                    // Update labels to show stacking
                    for (int i = 0; i < 3; i++) {
                        int idx = wheelIndices[i];
                        int count = 0;
                        // Count how many times this power has appeared so far in this spin
                        for (int j = 0; j <= i; j++) {
                            if (wheelIndices[j] == idx) count++;
                        }
                        wheelLabels[i].setText(getPowerLabel(idx, count));
                    }
                    // Call the corresponding toggle methods for the selected powers
                    for (int i = 0; i < 3; i++) {
                        callToggle(wheelIndices[i]);
                    }
                }
            }
        }, 0, 0.08f); // 80ms between spins
    }

    private String getPowerLabel(int index) {
        return getPowerLabel(index, 1);
    }

    private String getPowerLabel(int index, int count) {
        if (CAN_BE_STACKED[index] && count > 1) {
            return POWER_NAMES[index] + " " + toRoman(count);
        } else {
            return POWER_NAMES[index];
        }
    }

    // Simple roman numeral converter for 1-10
    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
    private String toRoman(int num) {
        if (num >= 1 && num < ROMAN.length) return ROMAN[num];
        return Integer.toString(num);
    }

    private void callToggle(int index) {
        switch (index) {
            case 0: SuperJumpToggle(); break;
            case 1: SpeedBoostToggle(); break;
            case 2: DoubleLifeToggle(); break;
            case 3: InvisibilityToggle(); break;
            case 4: SuperDodgeballToggle(); break;
        }
    }

    public void SuperJumpToggle() {
        System.out.println("Super Jump toggled!");
        powerUps[0] = true;
    }
    public void SpeedBoostToggle() {
        System.out.println("Speed Boost toggled!");
        powerUps[1] = true;
    }
    public void DoubleLifeToggle() {
        System.out.println("Double Life toggled!");
        powerUps[2] = true;
    }
    public void InvisibilityToggle() {
        System.out.println("Invisibility toggled!");
        powerUps[3] = true;
    }
    public void SuperDodgeballToggle() {
        System.out.println("Super Dodgeball toggled!");
        powerUps[4] = true;
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
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