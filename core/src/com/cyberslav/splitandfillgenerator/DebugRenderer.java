package com.cyberslav.splitandfillgenerator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.ScreenUtils;
import com.cyberslav.splitandfillgenerator.component.*;

import java.util.Collection;

public class DebugRenderer
{
    // public
    public DebugRenderer()
    {
    }


    public void render(Collection<MapComponent> components, SpriteBatch batch)
    {
        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        ScreenUtils.clear(_backgroundColor);

        _renderer.setProjectionMatrix(batch.getProjectionMatrix());

        // draw grid
        if (_gridEnabled)
        {
            _mode = RenderMode.Line;
            _renderer.begin(ShapeRenderer.ShapeType.Line);
            drawGrid();
            _renderer.end();
        }

        // draw components
        _mode = RenderMode.Fill;
        _renderer.begin(ShapeRenderer.ShapeType.Filled);
        drawComponents(components, null);
        _renderer.end();

        _mode = RenderMode.Line;
        _renderer.begin(ShapeRenderer.ShapeType.Line);
        drawComponents(components, null);
        _renderer.end();

        _mode = RenderMode.Text;
        batch.begin();
        drawComponents(components, batch);
    }


    // private
    private void drawComponents(Collection<MapComponent> components, SpriteBatch batch)
    {
        if (components == null)
            return;

        // draw components
        for (MapComponent component : components)
        {
            if (component instanceof PlatformComponent)
            {
                drawRect(
                        ((PlatformComponent)component).getRectangle(),
                        _platformLineColor,
                        _platformFillColor);
            }
            else if (component instanceof JumpPadComponent)
            {
                drawJumpPud((JumpPadComponent)component);
            }
            else if (component instanceof SpawnRegionComponent)
            {
                if (_spawnRegionsEnabled)
                {
                    Rectangle rect = new Rectangle(((SpawnRegionComponent) component).getRectangle());
                    rect.setTop(rect.getTop() + 0.5 * WorldProperties.getInstance().get("GRID_STEP"));

                    drawRect(
                            rect,
                            _spawnRegionColor,
                            null,
                            1.0f);
                }
            }
            else if (component instanceof DebugRegionComponent)
            {
                if (_debugRegionsEnabled)
                {
                    DebugRegionComponent debugComponent = (DebugRegionComponent)component;
                    Rectangle rect = debugComponent.getRegion().getRect();

                    drawRect(
                            rect,
                            _debugRegionColor,
                            null,
                            2.0f);

                    if (_mode == RenderMode.Text)
                    {
                        _labelFont.setColor(_debugRegionColor);
                        _labelFont.draw(
                                batch,
                                debugComponent.getDebugString(),
                                (float)rect.getX() + 4.0f,
                                (float)rect.getY() + 8.0f);
                    }
                }
            }
        }
    }


    private void drawJumpPud(JumpPadComponent component)
    {
        if (_mode != RenderMode.Line)
            return;

        drawRect(
                component.getRectangle(),
                _jumpPudColor,
                null,
                2.0f);

        final float centerX = (float)component.getRectangle().getCenterX();
        final float startY = (float)component.getRectangle().getY();
        final float height = (float)component.getRectangle().getHeight();
        final float arrowSize = (float)component.getRectangle().getWidth() * 0.3f;

        _renderer.setColor(_jumpPudColor);
        _renderer.line(centerX, startY + height * 0.25f, centerX, startY + height * 0.75f);

        _renderer.line(
                centerX,
                startY + height * 0.25f,
                centerX - arrowSize,
                startY + height * 0.25f + arrowSize);

        _renderer.line(
                centerX,
                startY + height * 0.25f,
                centerX + arrowSize,
                startY + height * 0.25f + arrowSize);

    }


    private void drawGrid()
    {
        if (_mode != RenderMode.Line)
            return;

        final float step = (float)WorldProperties.getInstance().get("GRID_STEP");
        final float height = Gdx.graphics.getHeight();
        final float width = Gdx.graphics.getWidth();
        final int rowCount = (int)(height / step);
        final int columnCount = (int)(width / step);

        _renderer.setColor(_gridColor);

        for (int r = 0; r < rowCount; ++r)
            _renderer.line(0f, step * r, width, step * r);

        for (int c = 0; c < columnCount; ++c)
            _renderer.line(step * c, 0, step * c, height);
    }


    private void drawRect(
            Rectangle rect,
            Color lineColor,
            Color fillColor)
    {
        drawRect(rect, lineColor, fillColor, 0.f);
    }


    private void drawRect(
            Rectangle rect,
            Color lineColor,
            Color fillColor,
            float reserve)
    {
        if (fillColor != null && _mode == RenderMode.Fill)
        {
            _renderer.setColor(fillColor);
        }
        else if (lineColor != null && _mode == RenderMode.Line)
        {
            _renderer.setColor(lineColor);
        }
        else
            return;

        _renderer.rect(
                (float)rect.getX() + reserve,
                (float)rect.getY() + reserve,
                (float)rect.getWidth() - 2.0f * reserve,
                (float)rect.getHeight() - 2.0f * reserve);
    }


    // data
    private enum RenderMode
    {
        Fill, Line, Text
    }


    private final ShapeRenderer _renderer = new ShapeRenderer();
    private final BitmapFont _labelFont = new BitmapFont(true);
    private RenderMode _mode = RenderMode.Fill;
    private boolean _gridEnabled = false;
    private boolean _spawnRegionsEnabled = false;
    private boolean _debugRegionsEnabled = false;

    //.. cyan theme
    private final Color _backgroundColor = new Color(0.1f, 0.1f, 0.1f, 1.0f);
    private final Color _gridColor = new Color(0.12f, 0.12f, 0.12f, 1.0f);
    private final Color _platformLineColor = new Color(0.3f, 0.8f, 1.0f, 1.0f);
    private final Color _platformFillColor = new Color(0.3f, 0.8f, 1.0f, 0.3f);
    private final Color _jumpPudColor = Color.CORAL;
//    private final Color _jumpPudColor = Color.PURPLE;
//    private final Color _spawnRegionColor = Color.CORAL;
    private final Color _spawnRegionColor = Color.GRAY;
//    private final Color _debugRegionColor = Color.ORANGE;
    private final Color _debugRegionColor = new Color(0x96661bff);

    //.. gray theme
//    private final Color _backgroundColor = new Color(0x2b2b2bff);
//    private final Color _platformLineColor = Color.LIGHT_GRAY;
//    private final Color _platformFillColor = Color.GRAY;

    //.. white theme
//    private final Color _backgroundColor = Color.WHITE;
//    private final Color _platformLineColor = Color.DARK_GRAY;
//    private final Color _platformFillColor = Color.LIGHT_GRAY;
}
