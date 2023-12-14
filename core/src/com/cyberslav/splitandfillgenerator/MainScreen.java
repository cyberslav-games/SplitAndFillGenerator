package com.cyberslav.splitandfillgenerator;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.cyberslav.splitandfillgenerator.component.MapComponent;

import java.util.Collection;


public class MainScreen implements Screen, InputProcessor
{
    // public
    public MainScreen()
    {
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(this);
        Gdx.input.setInputProcessor(multiplexer);

        // create scene
        OrthographicCamera stageCamera = new OrthographicCamera(
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight());

        stageCamera.setToOrtho(
                false,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight());

        ScreenViewport viewport = new ScreenViewport(stageCamera);
        _stage = new Stage(viewport);
        multiplexer.addProcessor(_stage);

        // create renderer actor
        _rendererActor = new RendererActor(_renderer);
        _stage.addActor(_rendererActor);

        // create UI
        final float margin = 20.0f;
        final Skin skin = new Skin(Gdx.files.internal("uiskin.json"));

        //.. outer table
        final Table outerTable = new Table();
        outerTable.setFillParent(true);
        outerTable.right().top();
        outerTable.pad(margin, margin, margin, margin);
        _stage.addActor(outerTable);

        //.. generate button
        final TextButton generateButton = new TextButton("Generate", skin);
        generateButton.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                generate();
            }
        });
        outerTable.add(generateButton).row();

        //.. scroll
        final ScrollPane pane = new ScrollPane(null, skin);
        outerTable.add(pane).row();

        //.. inner table
        final Table table = new Table();
        table.right().top();
        pane.setActor(table);

        //.. renderer settings
        table.add(new Label("Renderer", skin)).padTop(0.5f * margin).left().row();

        final CheckBox gridCheckBox = new CheckBox("grid", skin);
        table.add(gridCheckBox).padLeft(margin).left().row();
        gridCheckBox.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                _renderer.setGridEnabled(gridCheckBox.isChecked());
            }
        });

        final CheckBox spawnRegionsCheckBox = new CheckBox("spawn regions", skin);
        table.add(spawnRegionsCheckBox).padLeft(margin).left().row();
        spawnRegionsCheckBox.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                _renderer.setSpawnEnabled(spawnRegionsCheckBox.isChecked());
            }
        });

        final CheckBox debugRegionsCheckBox = new CheckBox("debug regions", skin);
        table.add(debugRegionsCheckBox).padLeft(margin).left().row();
        debugRegionsCheckBox.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                _renderer.setDebugEnabled(debugRegionsCheckBox.isChecked());
            }
        });

        final CheckBox pathCheckBox = new CheckBox("path", skin);
        table.add(pathCheckBox).padLeft(margin).left().row();
        pathCheckBox.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                _renderer.setPathEnabled(pathCheckBox.isChecked());
            }
        });

        //.. common parameters
        table.add(new Label("Common", skin)).padTop(0.5f * margin).left().row();
        final Table paramsTable = new Table();
        paramsTable.right().top();
        table.add(paramsTable).padLeft(margin).left().row();

        //.. .. grid step
        _gridStepField = new TextField(Integer.toString((int)get("GRID_STEP")), skin);
        _gridStepField.setTextFieldFilter(_intFilter);
        paramsTable.add(new Label("grid step", skin)).left();
        paramsTable.add(_gridStepField).padLeft(margin).row();

        //.. .. width
        _widthField = new TextField(Integer.toString(_defaultWidth), skin);
        _widthField.setTextFieldFilter(_intFilter);
        paramsTable.add(new Label("width", skin)).left();
        paramsTable.add(_widthField).padLeft(margin).row();

        //.. .. height
        _heightField = new TextField(Integer.toString(_defaultHeight), skin);
        _heightField.setTextFieldFilter(_intFilter);
        paramsTable.add(new Label("height", skin)).left();
        paramsTable.add(_heightField).padLeft(margin).row();

        //.. .. cut rate
        _cutRateField = new TextField(Double.toString(get("CUT_RATE")), skin);
        _cutRateField.setTextFieldFilter(_doubleFilter);
        paramsTable.add(new Label("cut rate", skin)).left();
        paramsTable.add(_cutRateField).padLeft(margin).row();

        //.. .. split rate
        _splitRateField = new TextField(Double.toString(get("SPLIT_DEVIATION_RATE")), skin);
        _splitRateField.setTextFieldFilter(_doubleFilter);
        paramsTable.add(new Label("split rate", skin)).left();
        paramsTable.add(_splitRateField).padLeft(margin).row();

        //.. .. min square
        _minSquareField = new TextField(Double.toString(get("MIN_SPLIT_SQUARE") / getStep()), skin);
        _minSquareField.setTextFieldFilter(_doubleFilter);
        paramsTable.add(new Label("min square", skin)).left();
        paramsTable.add(_minSquareField).padLeft(margin).row();

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


    @Override public void resize(int width, int height)
    {
        _stage.getViewport().update(
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight(),
                true);

        _rendererActor.updateSize();
    }


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
        WorldProperties.getInstance().update(Double.parseDouble(_gridStepField.getText()));
        set("CUT_RATE", Double.parseDouble(_cutRateField.getText()));
        set("SPLIT_DEVIATION_RATE", Double.parseDouble(_splitRateField.getText()));
        set("MIN_SPLIT_SQUARE", getStep() * Double.parseDouble(_minSquareField.getText()));

        final double roomWidth = toGrid(Integer.parseInt(_widthField.getText()));
        final double roomHeight = toGrid(Integer.parseInt(_heightField.getText()));
        final double enterPos = roomHeight;
        final double exitPos = roomWidth - WorldProperties.getInstance().get("H_WINDOW_DISPLACEMENT") * 2;
//        double exitPos = toGrid(250);
        final double vWindowSize = WorldProperties.getInstance().get("V_WINDOW_SIZE");
        final double hWindowSize = WorldProperties.getInstance().get("H_WINDOW_DISPLACEMENT") * 2;

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

        final SplitAndFillGenerator generator = new SplitAndFillGenerator();
        _components = null;

        for (int tryNum = 0; tryNum < 8 && _components == null; ++tryNum)
        {
            try
            {
                _components = generator.generateRegion(region, enterWindow);
            }
            catch (MapGeneratorException ignored)
            {
            }
        }

        _rendererActor.setComponents(_components);
    }


    private double getStep()
    {
        return get("GRID_STEP");
    }


    private double toGrid(double value)
    {
        return WorldProperties.getInstance().bindToGrid(value);
    }


    private double get(String name)
    {
        return WorldProperties.getInstance().get(name);
    }


    private void set(String name, double value)
    {
        WorldProperties.getInstance().set(name, value);
    }


    // data
    private final DebugRenderer _renderer = new DebugRenderer();
    private final Stage _stage;
    private final RendererActor _rendererActor;
    private final int _defaultHeight = 600;
    private final int _defaultWidth = 900;
    private Collection<MapComponent> _components;

    private final TextField.TextFieldFilter _intFilter =  new TextField.TextFieldFilter() {
        @Override public boolean acceptChar(TextField textField, char c) {
            return Character.isDigit(c);
        }
    };

    private final TextField.TextFieldFilter _doubleFilter =  new TextField.TextFieldFilter() {
        @Override public boolean acceptChar(TextField textField, char c) {
            return Character.isDigit(c) || c == '.';
        }
    };

    private final TextField _gridStepField;
    private final TextField _widthField;
    private final TextField _heightField;
    private final TextField _cutRateField;
    private final TextField _splitRateField;
    private final TextField _minSquareField;
}
