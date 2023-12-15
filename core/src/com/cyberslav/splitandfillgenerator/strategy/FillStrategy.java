package com.cyberslav.splitandfillgenerator.strategy;

import com.cyberslav.splitandfillgenerator.*;
import com.cyberslav.splitandfillgenerator.component.*;

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
