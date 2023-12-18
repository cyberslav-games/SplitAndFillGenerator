package com.cyberslav.splitandfillgenerator.generator.component;

import com.cyberslav.splitandfillgenerator.generator.MapGeneratorException;
import com.cyberslav.splitandfillgenerator.generator.WorldProperties;
import com.cyberslav.splitandfillgenerator.generator.utils.DirectedPoint;
import com.cyberslav.splitandfillgenerator.generator.utils.DirectedRegion;
import com.cyberslav.splitandfillgenerator.generator.utils.Point;
import com.cyberslav.splitandfillgenerator.generator.utils.Rectangle;

import java.util.Collection;

/**
 * Created by cyberslav on 13.12.17.
 */
public class SpawnRegionComponent extends RectangleComponent implements MapComponent
{
    public SpawnRegionComponent(Rectangle rect)
    {
        super(rect);
    }


    public static void addSpawnRegionInRange(
            double startPos,
            double endPos,
            DirectedRegion region,
            DirectedPoint exitPoint,
            double height,
            final Collection<MapComponent> components) throws MapGeneratorException
    {
        addSpawnRegionInRange(
                startPos,
                endPos,
                region,
                exitPoint,
                region.getRect().getHeight(),
                height,
                components);
    }


    public static void addSpawnRegionInRange(
            double startPos,
            double endPos,
            DirectedRegion region,
            DirectedPoint exitPoint,
            double yPos,
            double height,
            final Collection<MapComponent> components) throws MapGeneratorException
    {
        if (endPos <= startPos)
            return;

        final double WINDOW_DISPLACEMENT = get("H_WINDOW_DISPLACEMENT");
        final double PLAYER_WIDTH = toGrid(get("PLAYER_WIDTH"));

        // process collision with enter
        final DirectedPoint enterPoint = region.getEnterPoint();
        final double enterPosition = toGrid(enterPoint.getPosition());
        final double enterStart = enterPosition - WINDOW_DISPLACEMENT;
        final double enterEnd = enterPosition + PLAYER_WIDTH + WINDOW_DISPLACEMENT;
        final boolean enterIsDown = enterPoint.getDirection() == Point.Direction.Up;
        final boolean enterIsTouching = enterIsDown && (enterStart < endPos) && (enterEnd > startPos);

        if (enterIsTouching)
        {
            addSpawnRegionInRange(startPos, enterStart, region, exitPoint, yPos, height, components);
            addSpawnRegionInRange(enterEnd, endPos, region, exitPoint, yPos, height, components);
            return;
        }

        // process collision with exit
        boolean exitIsDown = exitPoint.getDirection() == Point.Direction.Down;
        final double exitPosition = toGrid(exitPoint.getPosition());
        final double exitStart = exitPosition - WINDOW_DISPLACEMENT;
        final double exitEnd = exitPosition + PLAYER_WIDTH + WINDOW_DISPLACEMENT;
        final boolean exitIsTouching = exitIsDown && (exitStart < endPos) && (exitEnd > startPos);

        if (exitIsTouching)
        {
            addSpawnRegionInRange(startPos, exitStart, region, exitPoint, yPos, height, components);
            addSpawnRegionInRange(exitEnd, endPos, region, exitPoint, yPos, height, components);
            return;
        }

        // create valid region
        final Rectangle rect = region.getRect();

        SpawnRegionComponent spawnRegion = new SpawnRegionComponent(
                new Rectangle(
                        toGrid(rect.getLeft() + startPos),
                        toGrid(rect.getTop() + yPos - height),
                        toGrid(endPos - startPos),
                        toGrid(height)));

        for (MapComponent component : components)
        {
            if (component instanceof PlatformComponent)
            {
                Rectangle platformRect = ((PlatformComponent)component).getRectangle();

                if (platformRect.isStrictCollide(spawnRegion.getRectangle()))
                {
                    System.out.println(spawnRegion.getRectangle());
                    System.out.println(platformRect);

                    throw new MapGeneratorException("Spawn region collide platform");
                }
            }
        }

        components.add(spawnRegion);
    }


    // private interface
    private static double toGrid(double value) throws MapGeneratorException
    {
        return WorldProperties.getInstance().bindToGrid(value);
    }

    private static double get(String name) throws MapGeneratorException
    {
        return WorldProperties.getInstance().get(name);
    }
}
