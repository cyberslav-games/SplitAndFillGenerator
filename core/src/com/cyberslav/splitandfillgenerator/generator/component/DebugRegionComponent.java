package com.cyberslav.splitandfillgenerator.generator.component;

import com.cyberslav.splitandfillgenerator.generator.utils.DirectedRegion;

/**
 * Created by cyberslav on 20.12.17.
 */
public class DebugRegionComponent implements MapComponent
{
    public DebugRegionComponent(
            DirectedRegion region,
            String debugString)
    {
        _region = region;
        _debugString = debugString;
    }


    public DirectedRegion getRegion()
    {
        return _region;
    }


    public String getDebugString()
    {
        return _debugString;
    }


    private final DirectedRegion _region;
    private final String _debugString;
}
