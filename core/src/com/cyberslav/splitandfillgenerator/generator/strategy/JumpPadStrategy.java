package com.cyberslav.splitandfillgenerator.generator.strategy;

import com.cyberslav.splitandfillgenerator.generator.MapGeneratorException;
import com.cyberslav.splitandfillgenerator.generator.WorldProperties;
import com.cyberslav.splitandfillgenerator.generator.component.JumpPadComponent;
import com.cyberslav.splitandfillgenerator.generator.component.MapComponent;
import com.cyberslav.splitandfillgenerator.generator.component.PlatformComponent;
import com.cyberslav.splitandfillgenerator.generator.component.SpawnRegionComponent;
import com.cyberslav.splitandfillgenerator.generator.utils.*;

import java.util.ArrayList;
import java.util.Collection;

public class JumpPadStrategy implements FillStrategy
{
    // public
    @Override public String getName()
    {
        return "JumpPad";
    }


    @Override public Collection<DirectedWindow> tryFill(
            Rectangle rect,
            DirectedWindow exitWindow) throws MapGeneratorException
    {
        ArrayList<DirectedWindow> enterWindows = new ArrayList<>();

        // add enter windows
        //.. left
        enterWindows.add(new DirectedWindow(
                rect,
                Point.Direction.Left,
                0,
                rect.getHeight()));

        //.. Right
        enterWindows.add(new DirectedWindow(
                rect,
                Point.Direction.Right,
                0,
                rect.getHeight()));

        //.. Down
        enterWindows.add(new DirectedWindow(
                rect,
                Point.Direction.Down,
                0,
                rect.getWidth()));

        //.. Up
        final Point.Direction exitDir = exitWindow.getDirection();
        final boolean exitOnTop = exitDir == Point.Direction.Up;
        final double leftExitX = exitOnTop ? exitWindow.getStartPosition() : 0;
        final double rightExitX = exitOnTop ? exitWindow.getEndPosition() : rect.getWidth();
        final double jumpPadWidth = get("JUMP_PAD_WIDTH");
        final double heroWidth = get("PLAYER_WIDTH");
        final double windowReserve = get("H_WINDOW_DISPLACEMENT");
        final double leftReserve = jumpPadWidth + windowReserve;
        final double rightReserve = jumpPadWidth + windowReserve + heroWidth;

        if (exitDir == Point.Direction.Right)
            enterWindows.add(new DirectedWindow(
                    rect,
                    Point.Direction.Up,
                    0,
                    rightExitX - rightReserve));
        else if (exitDir == Point.Direction.Left)
            enterWindows.add(new DirectedWindow(
                    rect,
                    Point.Direction.Up,
                    leftExitX + leftReserve,
                    rect.getWidth()));
        else if (exitDir == Point.Direction.Up)
            enterWindows.add(new DirectedWindow(
                    rect,
                    Point.Direction.Up,
                    leftExitX + leftReserve,
                    rightExitX - rightReserve));

        return enterWindows;
    }


    @Override public DirectedPoint fill(
            DirectedRegion region,
            final Collection<MapComponent> components) throws MapGeneratorException
    {
        Rectangle rect = region.getRect();
        DirectedPoint enterPoint = region.getEnterPoint();
        DirectedWindow exitWindow = region.getExitWindow();

        double jumpPadWidth = get("JUMP_PAD_WIDTH");

        // create exit point
        DirectedPoint exitPoint = null;
        double heroWidth = get("PLAYER_WIDTH");
        boolean exitToRight;

        if (exitWindow.isOnHorizontalEdge())
        {
            final double enterXPos = enterPoint.toLocalPoint(true).getX();
            final double leftDistance = Math.abs(enterXPos - exitWindow.getStartPosition());
            final double rightDistance = Math.abs(enterXPos - (exitWindow.getEndPosition()));

            exitToRight = (leftDistance < rightDistance);
            double exitPosition
                    = (exitToRight)
                    ? exitWindow.getEndPosition() - heroWidth
                    : exitWindow.getStartPosition();

            exitPosition = Math.min(exitPosition, rect.getWidth() - heroWidth);
            exitPoint = new DirectedPoint(rect, exitWindow.getDirection(), exitPosition);
        }
        else
        {
            exitToRight = exitWindow.getDirection() == Point.Direction.Right;
            exitPoint = new DirectedPoint(rect, exitWindow.getDirection(), exitWindow.getEndPosition());
        }

        // create component
        //.. create floor
        final double floorPos = Math.max(
                enterPoint.toLocalPoint(true).getY(),
                exitPoint.toLocalPoint(false).getY());

        final double floorHeight = toGrid(rect.getHeight() - floorPos);

        if (floorHeight > 0)
        {
            components.add(new PlatformComponent(new Rectangle(
                    toGrid(rect.getX() + 0),
                    toGrid(rect.getY() + floorPos),
                    toGrid(rect.getWidth()),
                    toGrid(floorHeight)
                    )));
        }

        final boolean needJumpPad = floorPos - exitPoint.toLocalPoint(false).getY() >= get("JUMP_HEIGHT");

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

            return exitPoint;
        }

        //.. create jump pad
        Point localExitPoint = exitPoint.toLocalPoint(false);
        double exitX = localExitPoint.getX();

        if (exitToRight && exitPoint.getDirection() == Point.Direction.Up)
            exitX += heroWidth;

        final double jumpPadX = toGrid(
                exitToRight
                        ? exitX - jumpPadWidth
                        : exitX);

        components.add(
                new JumpPadComponent(
                        new Rectangle(
                                toGrid(rect.getX() + jumpPadX),
                                toGrid(rect.getY() + localExitPoint.getY()),
                                toGrid(jumpPadWidth),
                                toGrid(floorPos - localExitPoint.getY())
                        )));

        //.. create side wall & spawn regions
        if (exitToRight)
        {
            final double wallStart = jumpPadX + jumpPadWidth;
            final double wallSize = rect.getWidth() - wallStart;

            if (wallSize > 0)
                components.add(
                        new PlatformComponent(
                                new Rectangle(
                                        toGrid(rect.getX() + wallStart),
                                        toGrid(rect.getY()),
                                        wallSize,
                                        floorPos
                                        )));

            SpawnRegionComponent.addSpawnRegionInRange(
                    0,
                    jumpPadX,
                    region,
                    exitPoint,
                    floorPos,
                    floorPos,
                    components);
        }
        else
        {
            final double wallEnd = jumpPadX;
            final double wallSize = wallEnd;

            if (wallSize > 0)
                components.add(
                        new PlatformComponent(
                                new Rectangle(
                                        toGrid(rect.getX()),
                                        toGrid(rect.getY()),
                                        wallSize,
                                        floorPos
                                        )));

            SpawnRegionComponent.addSpawnRegionInRange(
                    jumpPadX + jumpPadWidth,
                    rect.getWidth(),
                    region,
                    exitPoint,
                    floorPos,
                    floorPos,
                    components);
        }

        // return exit point
        return exitPoint;
    }


    @Override public double getMinWidth() throws MapGeneratorException
    {
        return get("JUMP_PAD_WIDTH") + 2.0 * get("PLAYER_WIDTH") + get("H_WINDOW_DISPLACEMENT");
    }

    @Override public double getMinHeight() throws MapGeneratorException
    {
        return toGrid(get("V_WINDOW_SIZE") * 4.0);
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
