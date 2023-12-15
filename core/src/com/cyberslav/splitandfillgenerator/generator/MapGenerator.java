package com.cyberslav.splitandfillgenerator.generator;

import com.cyberslav.splitandfillgenerator.generator.component.MapComponent;
import com.cyberslav.splitandfillgenerator.generator.utils.DirectedRegion;
import com.cyberslav.splitandfillgenerator.generator.utils.DirectedWindow;

import java.util.Collection;


/**
 * Created by cyberslav on 01.12.17.
 */
public interface MapGenerator
{
    boolean canGenerateRegion(DirectedRegion region) throws MapGeneratorException;

    Collection<MapComponent> generateRegion(
            DirectedRegion region,
            DirectedWindow enterWindow) throws MapGeneratorException;

//    double getMinRegionWidth() throws MapGeneratorException;
//    double getMinRegionHeight() throws MapGeneratorException;
}
