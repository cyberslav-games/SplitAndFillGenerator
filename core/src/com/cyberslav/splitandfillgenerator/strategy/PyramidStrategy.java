package com.cyberslav.splitandfillgenerator.strategy;

import com.cyberslav.splitandfillgenerator.*;
import com.cyberslav.splitandfillgenerator.component.*;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by cyberslav on 07.08.17.
 */
public class PyramidStrategy implements FillStrategy
{
    // public interface
    public PyramidStrategy() throws MapGeneratorException
    {
    }


    @Override public Collection<DirectedWindow> tryFill(
            Rectangle rect,
            DirectedWindow exitWindow) throws MapGeneratorException
    {
        ArrayList<DirectedWindow> enterWindows = new ArrayList<>();

        DirectedPoint startExitPoint = new DirectedPoint(
                rect,
                exitWindow.getDirection(),
                toGrid(exitWindow.getStartPosition()));

        DirectedPoint endExitPoint = new DirectedPoint(
                rect,
                exitWindow.getDirection(),
                toGrid(exitWindow.getEndPosition()));

        Point startLocalPoint = startExitPoint.toLocalPoint(false);
        Point endLocalPoint = endExitPoint.toLocalPoint(false);

        if (exitWindow.getDirection() == Point.Direction.Up)
        {
            startLocalPoint.setY(toGrid(get("TOP_PLATFORM_POS")));
            endLocalPoint.setY(toGrid(get("TOP_PLATFORM_POS")));
        }

        if (exitWindow.isOnHorizontalEdge())
            endLocalPoint.setX(
                    Math.min(
                            endLocalPoint.getX() - getHorizontalStep(),
                            rect.getWidth() - getHorizontalStep()));

        createEnterWindowsForVertex(
                enterWindows,
                rect,
                startLocalPoint);

        createEnterWindowsForVertex(
                enterWindows,
                rect,
                endLocalPoint);

        removeInvalidEnterWindows(enterWindows);

        return enterWindows;
    }


    @Override public Collection<DirectedPoint> fill(
            DirectedRegion region,
            final Collection<MapComponent> components) throws MapGeneratorException
    {
        Rectangle rect = region.getRect();
        DirectedPoint enterPoint = region.getEnterPoint();
        DirectedWindow exitWindow = region.getExitWindow();
        ArrayList<DirectedPoint> exitPoints = new ArrayList<>();

        // choose exit point
        DirectedPoint mainExitPoint;

        if (exitWindow.isOnHorizontalEdge())
        {
            double enterXPos = enterPoint.toLocalPoint(true).getX();
            double leftDistance = Math.abs(enterXPos - exitWindow.getStartPosition());
            double rightDistance = Math.abs(enterXPos - (exitWindow.getEndPosition() - getHorizontalStep()));

            double exitPosition
                    = (leftDistance < rightDistance)
                    ? exitWindow.getEndPosition() - getHorizontalStep()
                    : exitWindow.getStartPosition();

            exitPosition = Math.min(exitPosition, rect.getWidth() - getHorizontalStep());
            mainExitPoint = new DirectedPoint(rect, exitWindow.getDirection(), exitPosition);
        }
        else
        {
            double exitPos = exitWindow.getEndPosition();
            mainExitPoint = new DirectedPoint(rect, exitWindow.getDirection(), exitPos);
        }

        exitPoints.add(mainExitPoint);

        // create vertex point
        Point rawVertex = mainExitPoint.toLocalPoint(false);
        double vertexX = toGrid(rawVertex.getX());
        double vertexY = toGrid(rawVertex.getY());

        if (region.getRect().getHeight() - vertexY < getVerticalStep())
        {
            SpawnRegionComponent.addSpawnRegionInRange(
                    0,
                    rect.getWidth(),
                    region,
                    mainExitPoint,
                    rect.getHeight(),
                    components);

            return exitPoints;
        }

        if (region.getExitWindow().getDirection() == Point.Direction.Up)
            vertexY = Math.max(vertexY, WorldProperties.getInstance().get("TOP_PLATFORM_POS"));
        else if (!region.getExitWindow().isOnHorizontalEdge())
            vertexY = Math.max(vertexY, WorldProperties.getInstance().get("V_WINDOW_SIZE"));

        Point vertex = new Point(vertexX, vertexY); // rounded vertex position in rect local coordinates

        // crate bottom platform
        final double bottomPos = Math.max(
                region.getEnterPoint().toLocalPoint(true).getY(),
                vertexY);

        if (bottomPos <= rect.getHeight() - getGridStep())
            addPlatform(
                    components, rect,
                    0,
                    bottomPos,
                    rect.getWidth(),
                    rect.getHeight() - bottomPos,
                    false);

        // create pyramid
        double xStep = getHorizontalStep();
        double yStep = getVerticalStep();

        final double PYRAMID_BOTTOM = bottomPos; // rect.getHeight();
        final Rectangle fixedRect
                = new Rectangle(rect.getX(), rect.getY(), rect.getWidth(), PYRAMID_BOTTOM);
        final DirectedRegion fixedRegion = new DirectedRegion(
                fixedRect,
                region.getEnterPoint().toAnotherRect(fixedRect),
                region.getExitWindow().toAnotherRect(fixedRect));

        //... make central platform
        double vertexPosX = vertex.getX();
        double vertexPosY = vertex.getY();

        if (vertexPosX < rect.getWidth() && vertexPosY < PYRAMID_BOTTOM)
        {
            double platWidth = Math.min(xStep, rect.getWidth() - vertexPosX);
            double platHeight = PYRAMID_BOTTOM - vertexPosY;

            addPlatform(components, rect, vertexPosX, vertexPosY, platWidth, platHeight);
        }

        //... make right platforms
        if (exitWindow.getDirection() == Point.Direction.Up
                && enterPoint.toLocalPoint(true).getX() < vertex.getX()
                && vertex.getX() < rect.getWidth())
        {
            addPlatform(
                    components,
                    rect,
                    vertex.getX() + xStep,
                    0,
                    rect.getWidth() - (vertex.getX() + xStep),
                    PYRAMID_BOTTOM,
                    false);
        }
        else
        {
            double posX = vertex.getX() + xStep;
            double posY = vertex.getY() + yStep;

            for (; posX < rect.getWidth() && posY < PYRAMID_BOTTOM; )
            {
                double platWidth = Math.min(xStep, rect.getWidth() - posX);
                double platHeight = PYRAMID_BOTTOM - posY;

                addPlatform(components, rect, posX, posY, platWidth, platHeight);

                posX += xStep;
                posY += yStep;
            }

            SpawnRegionComponent.addSpawnRegionInRange(
                    posX,
                    rect.getWidth(),
                    fixedRegion,
                    mainExitPoint.toAnotherRect(fixedRect),
                    fixedRegion.getRect().getHeight(),
                    components);
        }

        //... make left platforms
        if (exitWindow.getDirection() == Point.Direction.Up
                && enterPoint.toLocalPoint(true).getX() > vertex.getX()
                && vertex.getX() >= getGridStep())
        {
            addPlatform(
                    components,
                    rect,
                    0,
                    0,
                    vertex.getX(),
                    PYRAMID_BOTTOM,
                    false);
        }
        else
        {
            double posX = vertex.getX() - xStep;
            double posY = vertex.getY() + yStep;

            for (; posX > -xStep && posY < PYRAMID_BOTTOM; )
            {
                double platWidth = Math.min(xStep, posX + xStep);
                double platHeight = PYRAMID_BOTTOM - posY;

                addPlatform(components, rect, Math.max(posX, 0), posY, platWidth, platHeight);

                posX -= xStep;
                posY += yStep;
            }

            SpawnRegionComponent.addSpawnRegionInRange(
                    0,
                    posX + xStep,
                    fixedRegion,
                    mainExitPoint.toAnotherRect(fixedRect),
                    fixedRegion.getRect().getHeight(),
                    components);
        }

        return exitPoints;
    }


    @Override public double getMinWidth() throws MapGeneratorException
    {
        return WorldProperties.getInstance().bindToGrid(
                WorldProperties.getInstance().get("PLAYER_WIDTH") * 6);
    }

    @Override public double getMinHeight() throws MapGeneratorException
    {
        return WorldProperties.getInstance().get("PLAYER_HEIGHT") * 2;
    }

    @Override public double getUseProbability() throws MapGeneratorException
    {
        return 1.0;
    }


    // private interface

    private void addPlatform(
            final Collection<MapComponent> components,
            Rectangle parentRect,
            double x,
            double y,
            double width,
            double height) throws MapGeneratorException
    {
        addPlatform(
                components,
                parentRect,
                x,
                y,
                width,
                height,
                true);
    }


    private void addPlatform(
            final Collection<MapComponent> components,
            Rectangle parentRect,
            double x,
            double y,
            double width,
            double height,
            boolean needSpawnPlatform) throws MapGeneratorException
    {
        components.add(
                new PlatformComponent(
                        new Rectangle(
                                x + parentRect.getX(),
                                y + parentRect.getY(),
                                width,
                                height)));


        if (needSpawnPlatform)
        {
            final double spawnRegionHeight = y;

            components.add(
                    new SpawnRegionComponent(
                            new Rectangle(
                                    x + parentRect.getX() + 0,
                                    y + parentRect.getY() + -spawnRegionHeight,
                                    width,
                                    spawnRegionHeight)));
        }
    }


    private void createEnterWindowsForVertex(
            ArrayList<DirectedWindow> enterWindows,
            Rectangle rect,
            Point rawVertex) throws MapGeneratorException
    {
        double vertexLeftX = toGrid(rawVertex.getX());
        double vertexRightX = toGrid(rawVertex.getX()) + getHorizontalStep();
        double vertexY = toGrid(rawVertex.getY());

        double xStep = getHorizontalStep();
        double yStep = getVerticalStep();
        assert(xStep > 0);
        assert(yStep > 0);
        double pyramidH = toGrid(rect.getHeight() - vertexY);
        double pyramidHalfW = pyramidH * xStep / yStep;
        double leftPyramidWidth = Math.min(pyramidHalfW, vertexLeftX);
        double rightPyramidWidth = Math.min(pyramidHalfW, rect.getWidth() - vertexRightX);

        double leftStairsH = leftPyramidWidth * yStep / xStep;
        double rightStairsH = rightPyramidWidth * yStep / xStep;

        if (pyramidHalfW > vertexLeftX)
            leftStairsH = Math.floor(leftStairsH / yStep) * yStep;

        if (pyramidHalfW > rect.getWidth() - vertexRightX)
            rightStairsH = Math.floor(rightStairsH / yStep) * yStep;

        // add top window
        enterWindows.add(new DirectedWindow(
                rect,
                Point.Direction.Down,
                0,
                rect.getWidth()));

        final double horizontalReserve = toGrid(getPlayerWidth());

        // add left window
        double leftWindowSize = toGridLess(vertexY + leftStairsH);

        enterWindows.add(new DirectedWindow(
                rect,
                Point.Direction.Right,
                0,
                leftWindowSize)); // - leftVerticalReserve));

        // add right window
        double rightWindowSize = toGridLess(vertexY + rightStairsH);

        enterWindows.add(new DirectedWindow(
                rect,
                Point.Direction.Left,
                0,
                rightWindowSize)); // - rightVerticalReserve));

        // add bottom windows
        double reserveDisplacement = (pyramidH < 0.5 * getGridStep()) // (pyramidHalfW < getGridStep())
                ? 0
                : getHorizontalStep() + get("H_WINDOW_DISPLACEMENT");
        double rightPos = toGridLess(vertexLeftX - pyramidHalfW - reserveDisplacement);

        if (rightPos > 0)
            enterWindows.add(new DirectedWindow(
                    rect,
                    Point.Direction.Up,
                    0,
                    rightPos)); // - horizontalReserve));

        double leftPos = toGrid(vertexLeftX + pyramidHalfW + reserveDisplacement);

        if (leftPos < rect.getWidth())
            enterWindows.add(new DirectedWindow(
                    rect,
                    Point.Direction.Up,
                    leftPos, // - horizontalReserve,
                    rect.getWidth()));
    }


    private void removeInvalidEnterWindows(ArrayList<DirectedWindow> enterWindows) throws MapGeneratorException
    {
        ArrayList<DirectedWindow> windowsForDelete = new ArrayList<>();
        final double H_WINDOW_SIZE = get("H_WINDOW_DISPLACEMENT") * 2 + getPlayerWidth();

        for (int windowNum = 0; windowNum < enterWindows.size(); ++windowNum)
        {
            DirectedWindow window = enterWindows.get(windowNum);

            if (window.isOnHorizontalEdge() && window.getSize() < H_WINDOW_SIZE)
                windowsForDelete.add(window);
            else if (!window.isOnHorizontalEdge() && window.getSize() < get("V_WINDOW_SIZE"))
                windowsForDelete.add(window);
            else
            {
                // check if there is encompassing window
                boolean hasEncompassingWindow = false;

                for (int secondWindowNum = windowNum + 1;
                     secondWindowNum < enterWindows.size() && !hasEncompassingWindow;
                     ++secondWindowNum)
                {
                    DirectedWindow anotherWindow = enterWindows.get(secondWindowNum);

                    hasEncompassingWindow
                            = anotherWindow.getDirection() == window.getDirection()
                            && anotherWindow.getStartPosition() <= window.getStartPosition()
                            && window.getEndPosition() <= anotherWindow.getEndPosition();
                }

                if (hasEncompassingWindow)
                    windowsForDelete.add(window);
            }
        }

        enterWindows.removeAll(windowsForDelete);
    }


    private double toGrid(double value) throws MapGeneratorException
    {
        return WorldProperties.getInstance().bindToGrid(value);
    }

    private double toGridLess(double value) throws MapGeneratorException
    {
        return WorldProperties.getInstance().bindToGridLess(value);
    }

    private double getGridStep() throws MapGeneratorException
    {
        return get("GRID_STEP");
    }

    private double getPlayerWidth() throws MapGeneratorException
    {
        return toGrid(get("PLAYER_WIDTH"));
    }

    private double getHorizontalStep() throws MapGeneratorException
    {
        return get("MIN_PLATFORM_WIDTH");
    }

    private double getVerticalStep() throws MapGeneratorException
    {
        return toGrid(get("JUMP_HEIGHT") * 0.7);
    }

    private static double get(String name) throws MapGeneratorException
    {
        return WorldProperties.getInstance().get(name);
    }


    // data members
}
