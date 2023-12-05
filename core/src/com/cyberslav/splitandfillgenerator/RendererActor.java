package com.cyberslav.splitandfillgenerator;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.cyberslav.splitandfillgenerator.component.MapComponent;

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

        //
    }


    public void setComponents(Collection<MapComponent> components)
    {
        _components = components;
    }


    @Override public void draw(Batch batch, float parentAlpha)
    {
//        batch.setProjectionMatrix(_camera.getProjectionMatrix());
        _renderer.render(_components, (SpriteBatch) batch);
//        batch.setProjectionMatrix(getStage().getCamera().combined);
    }


    // private

    // data
    private final DebugRenderer _renderer;
    private Collection<MapComponent> _components;
}
