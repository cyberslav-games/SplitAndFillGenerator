package com.cyberslav.splitandfillgenerator.strategy;

import com.cyberslav.splitandfillgenerator.*;
import com.cyberslav.splitandfillgenerator.component.*;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by cyberslav on 30.07.17.
 */
public class UniversalDebugStrategy implements FillStrategy
{
    @Override public String getName()
    {
        return "Empty";
    }


    @Override public Collection<DirectedWindow> tryFill(
            Rectangle rect,
            DirectedWindow exitWindow)
    {
        ArrayList<DirectedWindow> enterWindows = new ArrayList<>();

        for (Point.Direction dir : Point.Direction.values())
            if (dir != Point.getOppositeDirection(exitWindow.getDirection()))
                enterWindows.add(new DirectedWindow(
                        rect,
                        dir,
                        0,
                        Point.isHorizontalDirection(dir)
                                ? rect.getHeight()
                                : rect.getWidth()));

        return enterWindows;
    }


    // public interface
    @Override public Collection<DirectedPoint> fill(
            DirectedRegion region,
            final Collection<MapComponent> components
            ) throws MapGeneratorException
    {
        ArrayList<DirectedPoint> exitPoints = new ArrayList<>();

        double position
                = (region.getExitWindow().getStartPosition()
                + region.getExitWindow().getEndPosition()) / 2;

        exitPoints.add(
                new DirectedPoint(
                        region.getRect(),
                        region.getExitWindow().getDirection(),
                        WorldProperties.getInstance().bindToGrid(position)));

        return exitPoints;
    }


    @Override public double getMinWidth() throws MapGeneratorException
    {
        return WorldProperties.getInstance().get("JUMP_HEIGHT") * 2;
    }

    @Override public double getMinHeight() throws MapGeneratorException
    {
        return WorldProperties.getInstance().get("JUMP_HEIGHT") * 2;
    }

    @Override public double getUseProbability() throws MapGeneratorException
    {
        return 1.0;
    }


    // protected interface

    // data members
}
