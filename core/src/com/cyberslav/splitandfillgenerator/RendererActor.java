package com.cyberslav.splitandfillgenerator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.cyberslav.splitandfillgenerator.generator.component.MapComponent;

import java.util.Collection;

public class RendererActor extends Actor
{
    // public
    public RendererActor(
            DebugRenderer renderer
            //OrthographicCamera camera
            )
    {
        _renderer = renderer;

        _camera.setToOrtho(
                true,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight());
    }


    public void setComponents(Collection<MapComponent> components)
    {
        _components = components;
    }


    public void updateSize()
    {
        _camera.setToOrtho(
                true,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight());
    }


    @Override public void draw(Batch batch, float parentAlpha)
    {
        batch.setProjectionMatrix(_camera.combined);
        _renderer.render(_components, (SpriteBatch) batch);
        batch.setProjectionMatrix(getStage().getCamera().combined);
    }


    // private

    // data
    private final DebugRenderer _renderer;
    OrthographicCamera _camera = new OrthographicCamera(
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight());

    private Collection<MapComponent> _components;
}
