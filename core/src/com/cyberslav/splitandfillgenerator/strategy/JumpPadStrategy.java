package com.cyberslav.splitandfillgenerator.strategy;

import com.cyberslav.splitandfillgenerator.*;
import com.cyberslav.splitandfillgenerator.component.*;

import java.util.ArrayList;
import java.util.Collection;

public class JumpPadStrategy implements FillStrategy
{
    // public
    @Override public String getName()
    {
        return "JumpPud";
    }


    @Override public Collection<DirectedWindow> tryFill(
            Rectangle rect,
            DirectedWindow exitWindow) throws MapGeneratorException
    {
        ArrayList<DirectedWindow> enterWindows = new ArrayList<>();

        if (rect.getHeight() > get("MAX_JUMP_PAD_HEIGHT"))
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

        boolean needJumpPad = exitWindow.getDirection() == Point.Direction.Up
                || (!exitWindow.isOnHorizontalEdge()
                        && rect.getHeight() - exitWindow.getEndPosition() > get("JUMP_HEIGHT"));

        double playerWidth = get("PLAYER_WIDTH");

        if (needJumpPad)
        {
            Point.Direction exitDir = exitWindow.getDirection();
            boolean exitOnTop = exitDir == Point.Direction.Up;
            double leftExitX = exitOnTop ? exitWindow.getStartPosition() : 0;
            double rightExitX = exitOnTop
                    ? (exitWindow.getEndPosition() - playerWidth)
                    : (rect.getWidth() - playerWidth);
            double jumpPadWidth = get("GRID_STEP") * get("JUMP_PAD_WIDTH_CELLS");
            double reserve = get("H_WINDOW_DISPLACEMENT") + playerWidth;

            double leftWindowWidth = toGrid(rightExitX - Math.max(0, jumpPadWidth - playerWidth) - reserve);

            if (exitDir != Point.Direction.Left && leftWindowWidth >= playerWidth)
                enterWindows.add(new DirectedWindow(
                        rect,
                        Point.Direction.Up,
                        0,
                        leftWindowWidth));

            double rightWindowWidth = toGrid(rect.getWidth() - (leftExitX + jumpPadWidth + reserve));

            if (exitDir != Point.Direction.Right && rightWindowWidth >= playerWidth)
                enterWindows.add(new DirectedWindow(
                        rect,
                        Point.Direction.Up,
                        rect.getWidth() - rightWindowWidth,
                        rect.getWidth()
                        ));
        }
        else
        {
            double reserve = playerWidth + get("H_WINDOW_DISPLACEMENT");

            if (exitWindow.getDirection() == Point.Direction.Right)
            {
                enterWindows.add(new DirectedWindow(
                        rect,
                        Point.Direction.Up,
                        0,
                        rect.getWidth() - reserve - playerWidth));
            }
            else if (exitWindow.getDirection() == Point.Direction.Left)
            {
                enterWindows.add(new DirectedWindow(
                        rect,
                        Point.Direction.Up,
                        0 + reserve,
                        rect.getWidth()));
            }
            else
            {
                enterWindows.add(new DirectedWindow(
                        rect,
                        Point.Direction.Up,
                        0,
                        rect.getWidth()));
            }
        }

        return enterWindows;
    }


    @Override public Collection<DirectedPoint> fill(
            DirectedRegion region,
            final Collection<MapComponent> components) throws MapGeneratorException
    {
        Rectangle rect = region.getRect();
        DirectedPoint enterPoint = region.getEnterPoint();
        DirectedWindow exitWindow = region.getExitWindow();

        double jumpPadWidth = get("GRID_STEP") * get("JUMP_PAD_WIDTH_CELLS");

        // create exit point
        ArrayList<DirectedPoint> exitPoints = new ArrayList<>();
        DirectedPoint exitPoint = null;
        double playerWidth = get("PLAYER_WIDTH");

        if (exitWindow.isOnHorizontalEdge())
        {
            // TODO: выбирать точку выхода изходя расстояния до входной точки
            double enterXPos = enterPoint.toLocalPoint(true).getX();
            double exitPos = (enterXPos < 0.5 * rect.getWidth())
                    ? exitWindow.getEndPosition() - playerWidth
                    : exitWindow.getStartPosition();

            exitPoint = new DirectedPoint(rect, exitWindow.getDirection(), exitPos);
        }
        else
        {
            exitPoint = new DirectedPoint(rect, exitWindow.getDirection(), exitWindow.getEndPosition());
        }

        exitPoints.add(exitPoint);

        // create component
        //.. create floor
        double floorPos = Math.max(
                enterPoint.toLocalPoint(true).getY(),
                exitPoint.toLocalPoint(false).getY());
        double floorHeight = toGrid(rect.getHeight() - floorPos);

        if (floorHeight > 0)
        {
            components.add(new PlatformComponent(new Rectangle(
                    toGrid(rect.getX() + 0),
                    toGrid(rect.getY() + floorPos),
                    toGrid(rect.getWidth()),
                    toGrid(floorHeight)
                    )));
        }

        boolean needJumpPad = exitWindow.getDirection() == Point.Direction.Up
                || (!exitWindow.isOnHorizontalEdge()
                && /*rect.getHeight()*/floorPos - exitWindow.getEndPosition() > get("JUMP_HEIGHT"));

        if (!needJumpPad)
        {
            SpawnRegionComponent.addSpawnRegionInRange(
                    0,
                    rect.getWidth(),
                    region,
                    exitPoint,
                    floorPos,
                    floorPos,
                    components);

            return exitPoints;
        }

        //.. create jump pad
        Point localExitPoint = exitPoint.toLocalPoint(false);
        double exitX = localExitPoint.getX();

        double jumpPadX = toGrid(exitX + 0.5 * playerWidth - 0.5 * jumpPadWidth);

        jumpPadX = Math.max(0, jumpPadX);
        jumpPadX = Math.min(rect.getWidth() - jumpPadWidth, jumpPadX);

        components.add(new JumpPadComponent(new Rectangle(
                toGrid(rect.getX() + jumpPadX),
                toGrid(rect.getY() + localExitPoint.getY()),
                toGrid(jumpPadWidth),
                toGrid(/*rect.getHeight()*/ floorPos - localExitPoint.getY())
                )));

        //.. create side wall & spawn regions
        boolean exitIsUp = exitWindow.getDirection() == Point.Direction.Up;
        double enterX = enterPoint.toLocalPoint(true).getX();
        double localXPos = jumpPadX + jumpPadWidth;
        double rightWallWidth = exitIsUp ? toGrid(rect.getWidth() - localXPos) : 0;
        double leftWallWidth = exitIsUp ? toGrid(jumpPadX) : 0;

        if (enterX < exitX)
        {
            if (rightWallWidth > 0)
                components.add(new PlatformComponent(new Rectangle(
                        toGrid(rect.getX() + localXPos),
                        toGrid(rect.getY() + 0),
                        rightWallWidth,
                        toGrid(rect.getHeight())
                        )));

            SpawnRegionComponent.addSpawnRegionInRange(
                    0,
                    localXPos - jumpPadWidth,
                    region,
                    exitPoint,
                    floorPos,
                    floorPos,
                    components);
        }
        else
        {
            if (leftWallWidth > 0)
                components.add(new PlatformComponent(new Rectangle(
                        toGrid(rect.getX() + 0),
                        toGrid(rect.getY() + 0),
                        leftWallWidth,
                        toGrid(floorPos)
                        )));

            SpawnRegionComponent.addSpawnRegionInRange(
                    leftWallWidth + jumpPadWidth,
                    rect.getWidth(),
                    region,
                    exitPoint,
                    floorPos,
                    floorPos,
                    components);
        }

        // return exit point
        return exitPoints;
    }


    @Override public double getMinWidth() throws MapGeneratorException
    {
        return get("GRID_STEP") * get("JUMP_PAD_WIDTH_CELLS")
//                + get("PLAYER_WIDTH")
                + get("H_WINDOW_DISPLACEMENT")
                ;
    }

    @Override public double getMinHeight() throws MapGeneratorException
    {
//        return toGrid(get("V_WINDOW_SIZE") * 1.5);
        return toGrid(get("V_WINDOW_SIZE") * 4.0);  // TODO: вынести в настройки
    }

    @Override public double getUseProbability() throws MapGeneratorException
    {
        return get("JUMP_PAD_USE_PROB");
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
}
