package com.cyberslav.splitandfillgenerator.strategy;

import com.cyberslav.splitandfillgenerator.*;
import com.cyberslav.splitandfillgenerator.component.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.cyberslav.splitandfillgenerator.component.SpawnRegionComponent.addSpawnRegionInRange;

/**
 * Created by cyberslav on 05.09.17.
 */
public class GridStrategy implements FillStrategy
{
    public GridStrategy()
    {
    }


    @Override public String getName()
    {
        return "Grid";
    }


    @Override public Collection<DirectedWindow> tryFill(
            Rectangle rect,
            DirectedWindow exitWindow) throws MapGeneratorException
    {
        ArrayList<DirectedWindow> enterWindows = new ArrayList<>();

        if (exitWindow.isOnHorizontalEdge())
        {
            // check horizontal window
            final double windowSize = exitWindow.getEndPosition() - exitWindow.getStartPosition();
            final double minHWindowSize = PlatformGrid.getHorizontalStep() + PlatformGrid.getPlatformWidth();

            if (exitWindow.isOnHorizontalEdge() && windowSize < minHWindowSize)
                return enterWindows;
        }
        else
        {
            // check vertical window
            final boolean verticalWindowIsValid
                    = PlatformGrid.getRowNumUnder(exitWindow.getStartPosition() + get("PLAYER_HEIGHT"))
                    < PlatformGrid.getRowNumUnder(exitWindow.getEndPosition());

            if (!verticalWindowIsValid)
                return enterWindows;
        }

        enterWindows.add(new DirectedWindow(rect, Point.Direction.Down, 0, rect.getWidth()));
        enterWindows.add(new DirectedWindow(rect, Point.Direction.Right, 0, rect.getHeight()));
        enterWindows.add(new DirectedWindow(rect, Point.Direction.Left, 0, rect.getHeight()));

        final double H_WINDOW_SIZE = get("H_WINDOW_DISPLACEMENT") * 2 + toGrid(get("PLAYER_WIDTH"));

        if (rect.getWidth()
                >= PlatformGrid.getHorizontalStep() * 2 + PlatformGrid.getPlatformWidth() + H_WINDOW_SIZE)
        {
            if (!exitWindow.isOnHorizontalEdge()
                    && exitWindow.getEndPosition() >= rect.getHeight() - PlatformGrid.getVerticalStep())
            {
                if (exitWindow.getDirection() == Point.Direction.Right)
                    enterWindows.add(
                            new DirectedWindow(
                                    rect,
                                    Point.Direction.Up,
                                    0,
                                    toGrid(rect.getWidth() / 2)));
                else
                    enterWindows.add(
                            new DirectedWindow(
                                    rect,
                                    Point.Direction.Up,
                                    toGrid(rect.getWidth() / 2),
                                    rect.getWidth()));
            }
            else
                enterWindows.add(
                        new DirectedWindow(
                                rect,
                                Point.Direction.Up,
                                0,
                                rect.getWidth()));
        }

        return enterWindows;
    }


    // public interface
    @Override public Collection<DirectedPoint> fill(
            DirectedRegion region,
            final Collection<MapComponent> components) throws MapGeneratorException
    {
        Rectangle rect = region.getRect();
        DirectedPoint enterPoint = region.getEnterPoint();
        DirectedWindow exitWindow = region.getExitWindow();
        ArrayList<DirectedPoint> exitPoints = new ArrayList<>();

        // create fill variants
        class FillVariant
        {
            FillVariant(
                    double leftShift,
                    boolean firstRowDisplacement,
                    double regionWidth)
            {
                _leftShift = leftShift;
                _firstRowDisplacement = firstRowDisplacement;
                _regionWidth = regionWidth;
            }

            public final double _leftShift;
            public final boolean _firstRowDisplacement;
            public final double _regionWidth;
        }

        List<FillVariant> variants = new ArrayList<>();

        final double originWidth = rect.getWidth();
        final double cutWidth = toGrid(rect.getWidth() - get("PLAYER_WIDTH"));
        final double maxShift = PlatformGrid.getMaxShift(originWidth);

        variants.add(new FillVariant(0, false, originWidth));
        variants.add(new FillVariant(0, true, originWidth));
        variants.add(new FillVariant(maxShift, false, originWidth));
        variants.add(new FillVariant(maxShift, true, originWidth));
        variants.add(new FillVariant(get("PLAYER_WIDTH"), false, cutWidth));
        variants.add(new FillVariant(get("PLAYER_WIDTH"), true, cutWidth));

        doRandomSort(variants);

        // try fill variants
        boolean isBuilt = false;

        for (int variantNum = 0; variantNum < variants.size() && !isBuilt; ++variantNum)
        {
            FillVariant variant = variants.get(variantNum);

            isBuilt = tryBuild(
                    variant._firstRowDisplacement,
                    variant._leftShift,
                    variant._regionWidth,
                    region,
                    components,
                    exitPoints);
        }

        if (!isBuilt)
        {
            System.out.println(rect);
            System.out.println(enterPoint);
            System.out.println(exitWindow);
            throw new MapGeneratorException("GridStrategy: can't fill region");
        }

        return exitPoints;
    }


    @Override public double getMinWidth() throws MapGeneratorException
    {
        return PlatformGrid.getHorizontalStep()
                + PlatformGrid.getPlatformWidth()
                + toGrid(get("PLAYER_WIDTH") * 2);
    }

    @Override public double getMinHeight() throws MapGeneratorException
    {
        return PlatformGrid.getVerticalStep() * 1
                + PlatformGrid.getPlatformHeight()
                + PlatformGrid.getStartYPos();
    }


    // private interface
    private boolean tryBuild(
            boolean firstRowDisplacement,
            double leftShift,
            double regionWidth,
            DirectedRegion region,
            final Collection<MapComponent> components,
            final Collection<DirectedPoint> exitPoints) throws MapGeneratorException
    {
        Rectangle rect = region.getRect();
        DirectedPoint enterPoint = region.getEnterPoint();
        DirectedWindow exitWindow = region.getExitWindow();

        // create initial exit point
        DirectedPoint exitPoint = getDefaultExitPoint(rect, enterPoint, exitWindow);

        // create grid
        PlatformGrid grid = new PlatformGrid(
                new Rectangle(
                        rect.getX(),
                        rect.getY(),
                        regionWidth,
                        rect.getHeight()),
                firstRowDisplacement,
                leftShift,
                rect.getWidth());

        // block platforms
        blockRegionNearPoint(grid, enterPoint, true);
        blockRegionNearPoint(grid, exitPoint, false);

        // try to find paths
        exitPoint = grid.build(exitPoint, exitWindow).toAnotherRect(rect);

        if (!grid.isValid())
            return false;

        // create components
        exitPoints.add(exitPoint);

//        Iterator<Rectangle> platformIt = grid.getPlatforms();

//        for (platformIt.first(); !platformIt.isDone(); platformIt.next())
        for (Rectangle platformRect : grid.getPlatforms())
        {
//            Rectangle platformRect = platformIt.current();

            Rectangle platformRectCopy = new Rectangle(platformRect);
            Point newPosition = new Point(platformRectCopy.getX(), platformRectCopy.getY());
            newPosition.add(rect.getX(), rect.getY());
            platformRectCopy.setPosition(newPosition.getX(), newPosition.getY());

            // create platform
            components.add(new PlatformComponent(platformRectCopy));

            // create spawn region
            double spawnRegionHeight = Math.min(
                    platformRect.getTop(),
                    2.0 * PlatformGrid.getVerticalStep() - PlatformGrid.getPlatformHeight());

            components.add(
                    new SpawnRegionComponent(
                            new Rectangle(
                                    platformRectCopy.getX() + 0,
                                    platformRectCopy.getY() - spawnRegionHeight,
                                    platformRectCopy.getWidth(),
                                    spawnRegionHeight)));
        }

        // create bottom spawn regions
        //... get bottom platforms
//        Iterator<Rectangle> bottomPlatformIt = grid.getBottomPlatforms();
        ArrayList<Rectangle> bottomPlatforms = new ArrayList<>();

//        for (bottomPlatformIt.first(); !bottomPlatformIt.isDone(); bottomPlatformIt.next())
        for (Rectangle bottomPlatform : grid.getBottomPlatforms())
            bottomPlatforms.add(bottomPlatform);

        //... work out bottom platform height
        final double bottomPlatformHeight = bottomPlatforms.isEmpty()
                ? region.getRect().getHeight()
                : region.getRect().getHeight() - bottomPlatforms.get(0).getBottom();

        //... create spawn regions
        if (bottomPlatformHeight < get("PLAYER_HEIGHT"))
        {
//            double spawnRegionHeight = 2.0 * PlatformGrid.getVerticalStep() - PlatformGrid.getPlatformHeight();
            double spawnRegionHeight = bottomPlatformHeight + PlatformGrid.getVerticalStep();
            double startPos = 0;

            for (Rectangle platformRect : bottomPlatforms)
            {
                addSpawnRegionInRange(
                        startPos,
                        platformRect.getLeft(),
                        region,
                        exitPoint,
                        spawnRegionHeight,
                        components);

                startPos = platformRect.getRight();
            }

            addSpawnRegionInRange(
                    startPos,
                    region.getRect().getWidth(),
                    region,
                    exitPoint,
                    spawnRegionHeight,
                    components);
        }
        else
        {
            addSpawnRegionInRange(
                    0,
                    region.getRect().getWidth(),
                    region,
                    exitPoint,
                    bottomPlatformHeight,
                    components);
        }

        return true;
    }



    private DirectedPoint getDefaultExitPoint(
            Rectangle rect,
            DirectedPoint enterPoint,
            DirectedWindow exitWindow) throws MapGeneratorException
    {
        DirectedPoint exitPoint = null;

        if (exitWindow.isOnHorizontalEdge())
        {
            double enterXPos = enterPoint.toLocalPoint(true).getX();
            double leftBorder = exitWindow.getStartPosition() + PlatformGrid.getHorizontalStep() / 2;
            double rightBorder = exitWindow.getEndPosition() - PlatformGrid.getHorizontalStep() / 2;
            double leftDistance = enterXPos - leftBorder;
            double rightDistance = rightBorder - enterXPos;
            double exitPosition
                    = (leftDistance < rightDistance)
                    ? rightBorder
                    : leftBorder;

            exitPosition = toGrid(Math.min(exitPosition, rect.getWidth() - get("PLAYER_WIDTH")));
            exitPoint = new DirectedPoint(rect, exitWindow.getDirection(), exitPosition);
        }
        else
        {
            double exitPos = exitWindow.getEndPosition();
            exitPoint = new DirectedPoint(rect, exitWindow.getDirection(), exitPos);
        }

        return exitPoint;
    }


    private void blockRegionNearPoint(
            PlatformGrid grid,
            DirectedPoint point,
            boolean isEnter) throws MapGeneratorException
    {
        if (isEnter && point.getDirection() == Point.Direction.Down
                || !isEnter && point.getDirection() == Point.Direction.Up)
            return;

        final double PLAYER_HEIGHT = get("PLAYER_HEIGHT");
        final double PLAYER_WIDTH = toGrid(get("PLAYER_WIDTH"));
        final double GRID_STEP = get("GRID_STEP");

        if (point.isOnHorizontalEdge())
        {
            final double H_WINDOW_SIZE = get("H_WINDOW_DISPLACEMENT") * 2 + toGrid(get("PLAYER_WIDTH"));
            final double WINDOW_SIZE = H_WINDOW_SIZE + GRID_STEP * 2;
            final double HEIGHT_RATE = 1.5;
            double regionXPos = point.toLocalPoint(isEnter).getX() + PLAYER_WIDTH / 2 - WINDOW_SIZE / 2;
            double regionYPos = point.toLocalPoint(isEnter).getY() - PLAYER_HEIGHT * HEIGHT_RATE;

            grid.blockRegion(
                    new Rectangle(
                            regionXPos,
                            regionYPos,
                            WINDOW_SIZE,
                            PLAYER_HEIGHT * HEIGHT_RATE));
        }
        else
        {
            double regionXPos = point.toLocalPoint(isEnter).getX();
            double regionYPos = point.toLocalPoint(isEnter).getY() - PLAYER_HEIGHT;

            if (isEnter != Point.isPositiveDirection(point.getDirection()))
                regionXPos -= PLAYER_WIDTH;

            grid.blockRegion(
                    new Rectangle(
                            regionXPos + 0.25 * GRID_STEP,
                            regionYPos + 0.25 * GRID_STEP,
                            PLAYER_WIDTH - 0.5 * GRID_STEP,
                            PLAYER_HEIGHT - 0.5 * GRID_STEP));
        }
    }


    private static double toGrid(double value) throws MapGeneratorException
    {
        return WorldProperties.getInstance().bindToGrid(value);
    }

    private static double get(String name) throws MapGeneratorException
    {
        return WorldProperties.getInstance().get(name);
    }

    private <ValueType> void doRandomSort(List<ValueType> array)
    {
        for (int i = array.size() - 1; i >= 0; --i)
        {
            int j = MapRandom.getInstance().getNextInt(0, i + 1);
            ValueType tmp = array.get(i);
            array.set(i, array.get(j));
            array.set(j, tmp);
        }
    }

    // data members
}
