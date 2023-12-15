package com.cyberslav.splitandfillgenerator.generator.component;

import com.cyberslav.splitandfillgenerator.generator.utils.Rectangle;

public class PlatformTreeComponent extends RectangleComponent implements MapComponent
{
    // public
    public PlatformTreeComponent(
            Rectangle rect,
            double fullHeight,
            boolean isReversed)
    {
        super(rect);

        _fullHeight = fullHeight;
        _isReversed = isReversed;
    }

    public double getFullHeight()
    {
        return _fullHeight;
    }

    public boolean isReversed()
    {
        return _isReversed;
    }


    private final double _fullHeight;
    private final boolean _isReversed;
}
