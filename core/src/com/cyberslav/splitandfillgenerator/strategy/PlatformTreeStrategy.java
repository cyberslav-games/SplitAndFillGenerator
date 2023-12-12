package com.cyberslav.splitandfillgenerator.strategy;

import com.cyberslav.splitandfillgenerator.*;
import com.cyberslav.splitandfillgenerator.component.*;
import java.util.ArrayList;
import java.util.Collection;

public class PlatformTreeStrategy implements FillStrategy
{
    // public
    @Override public String getName()
    {
        return "Tree";
    }


    @Override public Collection<DirectedWindow> tryFill(
            Rectangle rect,
            DirectedWindow exitWindow) throws MapGeneratorException
    {
        ArrayList<DirectedWindow> enterWindows = new ArrayList<>();

        if (exitWindow.isOnHorizontalEdge()) // || rect.getHeight() > _maxHeight)
            return enterWindows;

//        final boolean needTree = exitWindow.getDirection() == Point.Direction.Up
//                || (!exitWindow.isOnHorizontalEdge()
//                && rect.getHeight() - exitWindow.getEndPosition() > get("JUMP_HEIGHT"));

        final double treeHeight = rect.getHeight() - exitWindow.getEndPosition();

        if (/*needTree &&*/ treeHeight > _maxTreeHeight)
            return enterWindows;

        enterWindows.add(new DirectedWindow(
                rect,
                Point.Direction.Left,
                0,
                rect.getHeight()));

        enterWindows.add(new DirectedWindow(
                rect,
                Point.Direction.Right,
                0,
                rect.getHeight()));

        enterWindows.add(new DirectedWindow(
                rect,
                Point.Direction.Down,
                0,
                rect.getWidth()));

        return enterWindows;
    }


    @Override public Collection<DirectedPoint> fill(
            DirectedRegion region,
            final Collection<MapComponent> components) throws MapGeneratorException
    {
        ArrayList<DirectedPoint> exitPoints = new ArrayList<>();
        DirectedWindow exitWindow = region.getExitWindow();
        Rectangle rect = region.getRect();

        boolean needTree = exitWindow.getDirection() == Point.Direction.Up
                || (!exitWindow.isOnHorizontalEdge()
                && rect.getHeight() - exitWindow.getEndPosition() > get("JUMP_HEIGHT"));

        DirectedPoint exitPoint = new DirectedPoint(
                rect,
                exitWindow.getDirection(),
                exitWindow.getEndPosition());

        exitPoints.add(exitPoint);

        // create spawn region
        SpawnRegionComponent.addSpawnRegionInRange(
                0,
                rect.getWidth(),
                region,
                exitPoint,
                components);

        // create tree
        assert(!exitWindow.isOnHorizontalEdge());

        final double treeHeight = needTree
                ? rect.getHeight() - exitWindow.getEndPosition()
                : Math.min(_maxTreeHeight, rect.getHeight() - _segmentHeight);

        final double treeWidth = needTree ? rect.getWidth() : Math.min(_treeWidth, rect.getWidth());
        final double treeX = (rect.getWidth() - treeWidth) / 2.0;
        final boolean isReversed = needTree
                ? exitWindow.getDirection() == Point.Direction.Left
                : MapRandom.getInstance().getNextInt() % 2 == 1;

        if (needTree || treeHeight >= _minTreeHeight)
        {
            components.add(new PlatformTreeComponent(
                    new Rectangle(
                            rect.getX() + treeX,
                            rect.getBottom() - treeHeight,
                            treeWidth,
                            treeHeight),
                    rect.getHeight() - 1.0 * get("GRID_STEP"),
                    isReversed
//                    exitWindow.getDirection() == Point.Direction.Left
                    ));
        }

        return exitPoints;
    }


    @Override public double getMinWidth() throws MapGeneratorException
    {
        return get("GRID_STEP") * (
                2 * 2   // endings
                + 4 * 2   // branches
                + 4     // trunk
                + 2     // reserve
                );
    }

    @Override public double getMinHeight() throws MapGeneratorException
    {
        return get("GRID_STEP") * (4 * 2 /*+ 6*/);
    }

    @Override public double getUseProbability() throws MapGeneratorException
    {
        return 1.0;
    }


    // protected

    // private
    private static double toGrid(double value) throws MapGeneratorException
    {
        return WorldProperties.getInstance().bindToGrid(value);
    }

    private static double get(String name) throws MapGeneratorException
    {
        return WorldProperties.getInstance().get(name);
    }

    // constants
    private final int _maxSegmentCount = 6;
    private final int _minSegmentCount = 2;
    private final double _segmentHeight = get("GRID_STEP") * 4;
    private final double _maxTreeHeight = _maxSegmentCount * _segmentHeight;
    private final double _minTreeHeight = _minSegmentCount * _segmentHeight;
    private final double _treeWidth = get("GRID_STEP") * (
            2 * 2   // endings
            + 4 * 3 * 2   // branches
            + 6     // trunk
            );
}
