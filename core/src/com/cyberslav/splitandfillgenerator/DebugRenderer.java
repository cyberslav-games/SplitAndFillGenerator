package com.cyberslav.splitandfillgenerator;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
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

        Matrix4 matrix = new Matrix4(batch.getProjectionMatrix());
        matrix.scale(0.1f, 0.1f, 1f);
        _renderer.setProjectionMatrix(matrix);
//        _renderer.setProjectionMatrix(batch.getProjectionMatrix());

        for (MapComponent component : components)
        {
            if (component instanceof PlatformComponent)
            {
                drawRect(((PlatformComponent)component).getRectangle(), _platformLineColor, _platformFillColor);
            }
        }

        batch.begin();
    }


    // private
    private void drawRect(Rectangle rect, Color lineColor, Color fillColor)
    {
        if (fillColor != null)
        {
            _renderer.begin(ShapeRenderer.ShapeType.Filled);
            _renderer.setColor(fillColor);
            _renderer.rect((float)rect.getX(), (float)rect.getY(), (float)rect.getWidth(), (float)rect.getHeight());
            _renderer.end();
        }

        if (lineColor != null)
        {
            _renderer.begin(ShapeRenderer.ShapeType.Line);
            _renderer.setColor(lineColor);
            _renderer.rect((float)rect.getX(), (float)rect.getY(), (float)rect.getWidth(), (float)rect.getHeight());
            _renderer.end();
        }
    }


    // data
    private final ShapeRenderer _renderer = new ShapeRenderer();
    private final Color _platformLineColor = Color.DARK_GRAY;
    private final Color _platformFillColor = Color.LIGHT_GRAY;
}
