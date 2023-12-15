package com.cyberslav.splitandfillgenerator.generator.component;

import com.cyberslav.splitandfillgenerator.generator.utils.Point;

/**
 * Created by cyberslav on 09.12.17.
 */
public class DebugLineComponent implements MapComponent
{
    public DebugLineComponent(
            Point begin,
            Point end)
    {
        _begin = begin;
        _end = end;
    }

    public final Point _begin;
    public final Point _end;
}
