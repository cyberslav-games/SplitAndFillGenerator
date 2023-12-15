package com.cyberslav.splitandfillgenerator.strategy;

import com.cyberslav.splitandfillgenerator.*;
import com.cyberslav.splitandfillgenerator.component.*;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by cyberslav on 07.11.17.
 */
public class FloorStrategy implements FillStrategy
{
    @Override public String getName()
    {
        return "Floor";
    }

    
    @Override public Collection<DirectedWindow> tryFill(
            Rectangle rect,
            DirectedWindow exitWindow) throws MapGeneratorException
    {
        ArrayList<DirectedWindow> enterWindows = new ArrayList<>();

        if (exitWindow.isOnHorizontalEdge() || exitWindow.getEndPosition() < getTunnelHeight())
            return enterWindows;

        double minPosition = exitWindow.getEndPosition();

        enterWindows.add(new DirectedWindow(
                rect,
                exitWindow.getDirection(),
                0,
                minPosition));

        enterWindows.add(new DirectedWindow(
                rect,
                Point.getOppositeDirection(exitWindow.getDirection()),
                0,
                minPosition));

        return enterWindows;
    }


    // public interface
    @Override public DirectedPoint fill(
            DirectedRegion region,
            final Collection<MapComponent> components) throws MapGeneratorException
    {
        Rectangle rect = region.getRect();
        DirectedPoint enterPoint = region.getEnterPoint();
        DirectedWindow exitWindow = region.getExitWindow();

        assert(!enterPoint.isOnHorizontalEdge());
        assert(!exitWindow.isOnHorizontalEdge());

        final double GRID_STEP = get("GRID_STEP");

        // create bottom platform
        double floorPosition = toGrid(
                Math.max(enterPoint.getPosition(), exitWindow.getEndPosition()));

        if (rect.getHeight() - enterPoint.getPosition() >= GRID_STEP)
        {
            addPlatform(
                    components,
                    rect,
                    0,
                    floorPosition,
                    rect.getWidth(),
                    rect.getHeight() - floorPosition);
        }

        components.add(new SpawnRegionComponent(
                new Rectangle(
                        toGrid(rect.getX()),
                        toGrid(rect.getY()),
                        toGrid(rect.getWidth()),
                        toGrid(floorPosition))));

        // return exit point
        return new DirectedPoint(
                exitWindow.getRect(),
                exitWindow.getDirection(),
                exitWindow.getEndPosition());
    }


    @Override public double getMinWidth() throws MapGeneratorException
    {
        return toGrid(get("PLAYER_WIDTH")) * 4;
    }

    @Override public double getMinHeight() throws MapGeneratorException
    {
        return getTunnelHeight();
    }


    // private interface
    private void addPlatform(
            final Collection<MapComponent> components,
            Rectangle parentRect,
            double x,
            double y,
            double width,
            double height)
    {
        PlatformComponent platform = new PlatformComponent(
                new Rectangle(
                        x + parentRect.getX(),
                        y + parentRect.getY(),
                        width,
                        height));

        components.add(platform);
    }


    private static double getTunnelHeight() throws MapGeneratorException
    {
        return get("V_WINDOW_SIZE");
    }

    private static double toGrid(double value) throws MapGeneratorException
    {
        return WorldProperties.getInstance().bindToGrid(value);
    }

    private static double get(String name) throws MapGeneratorException
    {
        return WorldProperties.getInstance().get(name);
    }
}
