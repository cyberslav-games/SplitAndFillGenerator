package com.cyberslav.splitandfillgenerator;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.cyberslav.splitandfillgenerator.component.MapComponent;

import java.util.Collection;


public class MainScreen implements Screen, InputProcessor
{
    // public
    public MainScreen()
    {
        Gdx.input.setInputProcessor(this);

        // create scene
        OrthographicCamera stageCamera = new OrthographicCamera(
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight());

        stageCamera.setToOrtho(
                true,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight());

        ScreenViewport viewport = new ScreenViewport(stageCamera);
        _stage = new Stage(viewport);
        _rendererActor = new RendererActor(_renderer);

        float margin = 10.0f;
        Table table = new Table();

        table.setFillParent(true);
        table.right().bottom();
        table.pad(margin, margin, margin, margin);

        Label.LabelStyle style = new Label.LabelStyle(new BitmapFont(true), null);
        Label testLabel = new Label("Some text", style);

        table.add(testLabel).right().padBottom(margin).row();

        _stage.addActor(_rendererActor);
        _stage.addActor(table);

        // generate level
        generate();
    }


    //.. Screen interface
    @Override public void show() {}


    @Override public void render(float delta)
    {
        _stage.act();
        _stage.draw();
    }


    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {}


    //.. InputProcessor
    @Override public boolean keyDown(int keycode)
    {
        switch (keycode)
        {
            case Input.Keys.R:
            {
                generate();
                return true;
            }
        }

        return false;
    }

    @Override public boolean keyUp(int keycode) { return false; }
    @Override public boolean keyTyped(char character) { return false; }
    @Override public boolean touchDown(int screenX, int screenY, int pointer, int button) { return false; }
    @Override public boolean touchUp(int screenX, int screenY, int pointer, int button) { return false; }
    @Override public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }
    @Override public boolean touchCancelled(int screenX, int screenY, int pointer, int button) { return false; }
    @Override public boolean mouseMoved(int screenX, int screenY) { return false; }
    @Override public boolean scrolled (float amountX, float amountY) { return false; }


    // private
    private void generate()
    {
        double roomWidth = toGrid(1000);
        double roomHeight = toGrid(600);
        double enterPos = roomHeight;
        double exitPos = roomWidth - WorldProperties.getInstance().get("H_WINDOW_DISPLACEMENT") * 2;
//        double exitPos = toGrid(250);
        double vWindowSize = WorldProperties.getInstance().get("V_WINDOW_SIZE");
        double hWindowSize = WorldProperties.getInstance().get("H_WINDOW_DISPLACEMENT") * 2;

        Rectangle rect = new Rectangle(
                2.0 * getStep(),
                2.0 * getStep(),
                roomWidth,
                roomHeight);

        DirectedRegion region = new DirectedRegion(
                rect,
                new DirectedPoint(rect, Point.Direction.Right, enterPos),
                new DirectedWindow(rect, Point.Direction.Up, exitPos - hWindowSize, exitPos + hWindowSize));
//                new DirectedWindow(rect, Point.Direction.Right, exitPos - windowSize, exitPos));

        DirectedWindow enterWindow = new DirectedWindow(
                rect,
                Point.Direction.Right,
                enterPos - vWindowSize,
                enterPos);

        _components = _generator.generateRegion(region, enterWindow);
        _rendererActor.setComponents(_components);
    }


    private double getStep()
    {
        return WorldProperties.getInstance().get("GRID_STEP");
    }


    private double toGrid(double value)
    {
        return WorldProperties.getInstance().bindToGrid(value);
    }


    // data
    private final SplitAndFillGenerator _generator = new SplitAndFillGenerator();
    private final DebugRenderer _renderer = new DebugRenderer();
    private final Stage _stage;
    private final RendererActor _rendererActor;

    private Collection<MapComponent> _components;
}
