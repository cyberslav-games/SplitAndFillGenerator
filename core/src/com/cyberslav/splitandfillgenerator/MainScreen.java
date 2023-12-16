package com.cyberslav.splitandfillgenerator;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.cyberslav.splitandfillgenerator.generator.MapGeneratorException;
import com.cyberslav.splitandfillgenerator.generator.WorldProperties;
import com.cyberslav.splitandfillgenerator.generator.component.MapComponent;
import com.cyberslav.splitandfillgenerator.generator.SplitAndFillGenerator;
import com.cyberslav.splitandfillgenerator.generator.utils.*;

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
        //.. outer table
        final Table outerTable = new Table();
        outerTable.setFillParent(true);
        outerTable.right().top();
        outerTable.pad(_margin, _margin, _margin, _margin);
        _stage.addActor(outerTable);

        //.. generate button
        final TextButton generateButton = new TextButton("Generate", _skin);
        generateButton.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                generate();
            }
        });
        outerTable.add(generateButton).row();

        //.. scroll
        final ScrollPane pane = new ScrollPane(null, _skin);
        outerTable.add(pane).row();

        //.. inner table
        final Table table = new Table();
        table.right().top();
        pane.setActor(table);

        //.. renderer settings
        final Table renderTable = addSectionTable(table, "Renderer");

        addCheckBox(renderTable, "grid", false, new CheckListener() {
            @Override public void changed(boolean isChecked) {
                _renderer.setGridEnabled(isChecked);
            }
        });

        addCheckBox(renderTable, "spawn regions", false, new CheckListener() {
            @Override public void changed(boolean isChecked) {
                _renderer.setSpawnEnabled(isChecked);
            }
        });

        addCheckBox(renderTable, "strategies", false, new CheckListener() {
            @Override public void changed(boolean isChecked) {
                _renderer.setDebugEnabled(isChecked);
            }
        });

        addCheckBox(renderTable, "path", false, new CheckListener() {
            @Override public void changed(boolean isChecked) {
                _renderer.setPathEnabled(isChecked);
            }
        });

        //.. room parameters
        final int height = 600;
        final int width = 900;
        final Point.Direction enterDir = Point.Direction.Right;
        final double enterPos = 1.0;
        final Point.Direction exitDir = Point.Direction.Up;
        final double exitStart = 0.75;
        final double exitEnd = 1.0;

        final Table roomTable = addSectionTable(table, "Room");

        _widthField = addIntField(roomTable, "width", width);
        _heightField = addIntField(roomTable, "height", height);
        _enterDirBox = addDirectionBox(roomTable, "enter dir", enterDir);
        _enterPosSlider = addSlider(roomTable, "enter pos", enterPos);

        //.. .. exit
        _exitDirBox = addDirectionBox(roomTable, "exit dir", exitDir);
        _exitStartSlider = addSlider(roomTable, "exit start", exitStart);
        _exitSizeSlider = addSlider(roomTable, "exit size", exitEnd);

        //.. common parameters
        final Table paramsTable = addSectionTable(table, "Generator");

        _gridStepField = addIntField(paramsTable, "grid step", (int)get("GRID_STEP"));
        _cutRateField = addDoubleField(paramsTable, "cut rate", get("CUT_RATE"));
        _splitRateField = addDoubleField(paramsTable, "split rate", get("SPLIT_DEVIATION_RATE"));
        _minSquareField = addDoubleField(paramsTable, "min square", get("MIN_SPLIT_SQUARE") / getStep());

        //.. strategies
        final Table strategyTable = addSectionTable(table, "Strategies");
        _pyramidCheckBox = addCheckBox(strategyTable, "Pyramid", true, null);
        _gridCheckBox = addCheckBox(strategyTable, "Grid", true, null);
        _jumpPadCheckBox = addCheckBox(strategyTable, "Jump Pad", true, null);

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
    private Table addSectionTable(Table parentTable, String name)
    {
        final Table table = new Table();
        table.right().top();
        parentTable.add(new Label(name, _skin)).padTop(0.5f * _margin).left().row();
        parentTable.add(table).padLeft(_margin).left().row();

        return table;
    }


    private Slider addSlider(Table parentTable, String name, double value)
    {
        Slider slider = new Slider(0.0f, 1.0f, 0.01f, false, _skin);
        slider.setValue((float)value);

        addWidget(parentTable, name, slider);

        return slider;
    }


    private CheckBox addCheckBox(Table parentTable, String name, boolean value, final CheckListener listener)
    {
        final CheckBox checkBox = new CheckBox(name, _skin);
        checkBox.setChecked(value);
        parentTable.add(checkBox).left().row();

        if (listener != null)
            checkBox.addListener(new ChangeListener()
            {
                @Override public void changed(ChangeEvent event, Actor actor)
                {
                    listener.changed(checkBox.isChecked());
                }
            });

        return checkBox;
    }


    private TextField addIntField(Table parentTable, String name, int value)
    {
        TextField field = new TextField(Integer.toString(value), _skin);
        field.setTextFieldFilter(_intFilter);

        addWidget(parentTable, name, field);

        return field;
    }


    private TextField addDoubleField(Table parentTable, String name, double value)
    {
        TextField field = new TextField(Double.toString(value), _skin);
        field.setTextFieldFilter(_doubleFilter);

        addWidget(parentTable, name, field);

        return field;
    }


    private void addWidget(Table parentTable, String name, Widget widget)
    {
        parentTable.add(new Label(name, _skin)).left();
        parentTable.add(widget).padLeft(0.5f * _margin).left().row();
    }


    private SelectBox<Point.Direction> addDirectionBox(Table parentTable, String name, Point.Direction dir)
    {
        SelectBox<Point.Direction> box = new SelectBox<>(_skin);
        box.setItems(Point.Direction.Right, Point.Direction.Down, Point.Direction.Left, Point.Direction.Up);
        box.setSelected(dir);

        addWidget(parentTable, name, box);

        return box;
    }


    private void generate()
    {
        // generator settings
        WorldProperties.getInstance().update(Double.parseDouble(_gridStepField.getText()));
        set("CUT_RATE", Double.parseDouble(_cutRateField.getText()));
        set("SPLIT_DEVIATION_RATE", Double.parseDouble(_splitRateField.getText()));
        set("MIN_SPLIT_SQUARE", getStep() * Double.parseDouble(_minSquareField.getText()));

        final SplitAndFillGenerator generator = new SplitAndFillGenerator();

        // strategies
        generator.setStrategyEnabled(SplitAndFillGenerator.StrategyId.Pyramid, _pyramidCheckBox.isChecked());
        generator.setStrategyEnabled(SplitAndFillGenerator.StrategyId.Grid, _gridCheckBox.isChecked());
        generator.setStrategyEnabled(SplitAndFillGenerator.StrategyId.JumpPad, _jumpPadCheckBox.isChecked());

        // room params
        final double roomWidth = toGrid(Integer.parseInt(_widthField.getText()));
        final double roomHeight = toGrid(Integer.parseInt(_heightField.getText()));
        final double hWindowSize = get("H_WINDOW_DISPLACEMENT") + get("PLAYER_WIDTH");
        final double vWindowSize = get("V_WINDOW_SIZE");

        final Point.Direction enterDir = _enterDirBox.getSelected();
        final Point.Direction exitDir = _exitDirBox.getSelected();

        final boolean enterIsVertical = Point.isHorizontalDirection(enterDir);
        final boolean exitIsVertical = Point.isHorizontalDirection(exitDir);
        final double enterSideSize = enterIsVertical ? roomHeight : roomWidth;
        final double exitSideSize = exitIsVertical ? roomHeight : roomWidth;
        final double enterSize = enterIsVertical ? vWindowSize : hWindowSize;
        final double minExitSize = exitIsVertical ? vWindowSize : hWindowSize;

        final double enterPos = toGrid(
                enterSize + (enterSideSize - enterSize) * _enterPosSlider.getValue());
        final double exitStart = toGrid(
                (exitSideSize - minExitSize) * _exitStartSlider.getValue());
        final double exitSize = toGrid(
                minExitSize + (exitSideSize - (exitStart + minExitSize)) * _exitSizeSlider.getValue());

        Rectangle rect = new Rectangle(
                toGrid(_margin * 2),
                toGrid(_margin * 2),
                roomWidth,
                roomHeight);

        DirectedRegion region = new DirectedRegion(
                rect,
                new DirectedPoint(rect, enterDir, enterPos),
                new DirectedWindow(rect, exitDir, exitStart, exitStart + exitSize));

        DirectedWindow enterWindow = new DirectedWindow(
                rect,
                Point.Direction.Right,
                enterPos - get("V_WINDOW_SIZE"),
                enterPos);

        // generate
        _components = null;

        if (generator.canGenerateRegion(region))
        {
            for (int tryNum = 0; tryNum < 8 && _components == null; ++tryNum)
            {
                try
                {
                    _components = generator.generateRegion(region, enterWindow);
                }
                catch (MapGeneratorException ignored) {
                }
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

    abstract private class CheckListener
    {
        abstract public void changed(boolean isChecked);
    }

    private final Skin _skin = new Skin(Gdx.files.internal("uiskin.json"));
    private final float _margin = 20.0f;
    private final TextField _gridStepField;
    private final TextField _cutRateField;
    private final TextField _splitRateField;
    private final TextField _minSquareField;
    private final TextField _widthField;
    private final TextField _heightField;
    private final SelectBox<Point.Direction> _enterDirBox;
    private final SelectBox<Point.Direction> _exitDirBox;
    private final Slider _enterPosSlider;
    private final Slider _exitStartSlider;
    private final Slider _exitSizeSlider;
    private final CheckBox _pyramidCheckBox;
    private final CheckBox _gridCheckBox;
    private final CheckBox _jumpPadCheckBox;
}
