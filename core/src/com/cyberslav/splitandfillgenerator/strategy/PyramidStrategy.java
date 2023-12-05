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
                            endLocalPoint.getX(),
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
            double leftDistance = enterXPos - exitWindow.getStartPosition();
            double rightDistance = exitWindow.getEndPosition() - getPlayerWidth() - enterXPos;
            double exitPosition
                    = (leftDistance < rightDistance)
                    ? exitWindow.getEndPosition() - getPlayerWidth()
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

        if (region.getRect().getHeight() - vertexY < get("JUMP_HEIGHT"))
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

        vertexX = Math.min(vertexX, rect.getWidth() - getHorizontalStep());

        Point vertex = new Point(vertexX, vertexY); // rounded vertex position in rect local coordinates

        // crate bottom platform
        final double bottomPosVariant1 = toGrid(0.5 *(vertex.getY() + rect.getHeight()));
        final double bottomPosVariant2 = toGrid(
                region.getEnterPoint().toLocalPoint(true).getY()
                + getVerticalStep());
        double bottomPos = Math.min(bottomPosVariant1, bottomPosVariant2);
        bottomPos = Math.max(
                bottomPos,
                toGrid(region.getEnterPoint().toLocalPoint(true).getY()));
        bottomPos = Math.max(bottomPos, vertex.getY());
        bottomPos = Math.min(bottomPos, rect.getHeight());

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

        //... make central platforms
        double xPos = vertex.getX();
        double yPos = vertex.getY();

        if (xPos < rect.getWidth() && yPos < PYRAMID_BOTTOM)
        {
            double platWidth = Math.min(xStep, rect.getWidth() - xPos);
            double platHeight = PYRAMID_BOTTOM - yPos;

            addPlatform(components, rect, xPos, yPos, platWidth, platHeight);
        }

        if (yPos - yStep < PYRAMID_BOTTOM
                && mainExitPoint.getDirection() != Point.Direction.Right)
            exitPoints.add(new DirectedPoint(rect, Point.Direction.Right, yPos)); // - yStep));

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
            xPos = vertex.getX() + xStep;
            yPos = vertex.getY() + yStep;

            for (; xPos < rect.getWidth() && yPos < PYRAMID_BOTTOM; )
            {
                double platWidth = Math.min(xStep, rect.getWidth() - xPos);
                double platHeight = PYRAMID_BOTTOM - yPos;

                addPlatform(components, rect, xPos, yPos, platWidth, platHeight);

                xPos += xStep;
                yPos += yStep;
            }

            if (yPos - yStep < PYRAMID_BOTTOM
                    && mainExitPoint.getDirection() != Point.Direction.Right)
                exitPoints.add(new DirectedPoint(rect, Point.Direction.Right, yPos)); // - yStep));

            // TODO
            SpawnRegionComponent.addSpawnRegionInRange(
                    xPos,
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
            xPos = vertex.getX() - xStep;
            yPos = vertex.getY() + yStep;

            for (; xPos > -xStep && yPos < PYRAMID_BOTTOM; )
            {
                double platWidth = Math.min(xStep, xPos + xStep);
                double platHeight = PYRAMID_BOTTOM - yPos;

                addPlatform(components, rect, Math.max(xPos, 0), yPos, platWidth, platHeight);

                xPos -= xStep;
                yPos += yStep;
            }

            if (yPos - yStep < PYRAMID_BOTTOM
                    && mainExitPoint.getDirection() != Point.Direction.Left)
                exitPoints.add(new DirectedPoint(rect, Point.Direction.Left, yPos));

            SpawnRegionComponent.addSpawnRegionInRange(
                    0,
                    xPos + xStep,
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
        double vertexX = toGrid(rawVertex.getX());
        double vertexXDispl = toGrid(rawVertex.getX()) + getPlayerWidth();
        double vertexY = toGrid(rawVertex.getY());

        double xStep = getHorizontalStep();
        double yStep = getVerticalStep();
        assert(xStep > 0);
        assert(yStep > 0);
        double yDispl = toGrid(rect.getHeight() - vertexY);
        double xDispl = yDispl * xStep / yStep;
        double lxDispl = Math.min(xDispl, vertexX);
        double rxDispl = Math.min(xDispl, rect.getWidth() - vertexXDispl);
        double lyDispl = lxDispl * yStep / xStep;
        double ryDispl = rxDispl * yStep / xStep;

        // add top window
        enterWindows.add(new DirectedWindow(
                rect,
                Point.Direction.Down,
                0,
                rect.getWidth()));

        final double horizontalReserve = toGrid(getPlayerWidth());

        // add left window
        double leftWindowSize = toGridLess(vertexY + lyDispl);
        final double leftVerticalReserve
                = (xDispl >= getGridStep() && lxDispl < xDispl + getHorizontalStep() / 2)
                ? toGrid(getPlayerHeight() * 1.0)
                : 0;

        enterWindows.add(new DirectedWindow(
                rect,
                Point.Direction.Right,
                0,
                leftWindowSize - leftVerticalReserve));

        // add right window
        double rightWindowSize = toGridLess(vertexY + ryDispl);
        final double rightVerticalReserve
                = (xDispl >= getGridStep() && rxDispl < xDispl + getHorizontalStep() / 2)
                ? toGrid(getPlayerHeight() * 1.0)
                : 0;

        enterWindows.add(new DirectedWindow(
                rect,
                Point.Direction.Left,
                0,
                rightWindowSize - rightVerticalReserve));

        // add bottom windows
        double reserveDisplacement = (yDispl < 0.5 * getGridStep()) // (xDispl < getGridStep())
                ? 0
                : getHorizontalStep() + get("H_WINDOW_DISPLACEMENT");
        double rightPos = toGridLess(vertexX - xDispl - reserveDisplacement);

        if (rightPos > 0)
            enterWindows.add(new DirectedWindow(
                    rect,
                    Point.Direction.Up,
                    0,
                    rightPos)); // - horizontalReserve));

        double leftPos = toGrid(vertexX + xDispl + reserveDisplacement);

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
            else if(!window.isOnHorizontalEdge() && window.getSize() < getPlayerHeight())
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
        return WorldProperties.getInstance().get("GRID_STEP");
    }

    private double getPlayerHeight() throws MapGeneratorException
    {
        return WorldProperties.getInstance().get("PLAYER_HEIGHT");
    }

    private double getPlayerWidth() throws MapGeneratorException
    {
        return WorldProperties.getInstance().bindToGrid(
                WorldProperties.getInstance().get("PLAYER_WIDTH"));
    }

    private double getHorizontalStep() throws MapGeneratorException
    {
        return WorldProperties.getInstance().get("MIN_PLATFORM_WIDTH");
    }

    private double getVerticalStep() throws MapGeneratorException
    {
        return WorldProperties.getInstance().bindToGrid(
                WorldProperties.getInstance().get("JUMP_HEIGHT") * 0.7);
    }

    private static double get(String name) throws MapGeneratorException
    {
        return WorldProperties.getInstance().get(name);
    }


    // data members
}
