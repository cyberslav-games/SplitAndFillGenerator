package com.cyberslav.splitandfillgenerator.generator.strategy;

import com.cyberslav.splitandfillgenerator.generator.MapGeneratorException;
import com.cyberslav.splitandfillgenerator.generator.component.MapComponent;
import com.cyberslav.splitandfillgenerator.generator.utils.DirectedPoint;
import com.cyberslav.splitandfillgenerator.generator.utils.DirectedRegion;
import com.cyberslav.splitandfillgenerator.generator.utils.DirectedWindow;
import com.cyberslav.splitandfillgenerator.generator.utils.Rectangle;

import java.util.Collection;

/**
 * Created by cyberslav on 02.12.17.
 */
public interface FillStrategy
{
    Collection<DirectedWindow> tryFill(
            Rectangle rect,
            DirectedWindow exitWindow) throws MapGeneratorException;

    DirectedPoint fill(
            DirectedRegion region,
            final Collection<MapComponent> components) throws MapGeneratorException;

    double getMinWidth() throws MapGeneratorException;
    double getMinHeight() throws MapGeneratorException;
    String getName();
}
