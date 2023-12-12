package com.cyberslav.splitandfillgenerator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.ScreenUtils;
import com.cyberslav.splitandfillgenerator.component.MapComponent;
import com.cyberslav.splitandfillgenerator.component.PlatformComponent;

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

//        float scale = 1.0f;
//        Matrix4 matrix = new Matrix4(batch.getProjectionMatrix());
//        matrix.scale(scale, scale, 1f);
//        _renderer.setProjectionMatrix(matrix);
        _renderer.setProjectionMatrix(batch.getProjectionMatrix());

        // draw grid
        _renderer.begin(ShapeRenderer.ShapeType.Line);
        drawGrid();
        _renderer.end();

        // draw components
        _renderer.begin(ShapeRenderer.ShapeType.Filled);
        drawComponents(components);
        _renderer.end();

        _renderer.begin(ShapeRenderer.ShapeType.Line);
        drawComponents(components);
        _renderer.end();

        batch.begin();
    }


    // private
    private void drawComponents(Collection<MapComponent> components)
    {
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
        }
    }


    private void drawGrid()
    {
        if (_renderer.getCurrentType() != ShapeRenderer.ShapeType.Line)
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


    private void drawRect(Rectangle rect, Color lineColor, Color fillColor)
    {
        if (fillColor != null && _renderer.getCurrentType() == ShapeRenderer.ShapeType.Filled)
        {
            _renderer.setColor(fillColor);
            _renderer.rect((float)rect.getX(), (float)rect.getY(), (float)rect.getWidth(), (float)rect.getHeight());
        }

        if (lineColor != null && _renderer.getCurrentType() == ShapeRenderer.ShapeType.Line)
        {
            _renderer.setColor(lineColor);
            _renderer.rect((float)rect.getX(), (float)rect.getY(), (float)rect.getWidth(), (float)rect.getHeight());
        }
    }


    // data
    private final ShapeRenderer _renderer = new ShapeRenderer();

    //.. cyan theme
    private final Color _backgroundColor = new Color(0.1f, 0.1f, 0.1f, 1.0f);
    private final Color _gridColor = new Color(0.15f, 0.15f, 0.15f, 1.0f);
    private final Color _platformLineColor = new Color(0.3f, 0.8f, 1.0f, 1.0f);
    private final Color _platformFillColor = new Color(0.3f, 0.8f, 1.0f, 0.3f);

    //.. gray theme
//    private final Color _backgroundColor = new Color(0x2b2b2bff);
//    private final Color _platformLineColor = Color.LIGHT_GRAY;
//    private final Color _platformFillColor = Color.GRAY;

    //.. white theme
//    private final Color _backgroundColor = Color.WHITE;
//    private final Color _platformLineColor = Color.DARK_GRAY;
//    private final Color _platformFillColor = Color.LIGHT_GRAY;
}
