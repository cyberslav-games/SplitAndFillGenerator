package com.cyberslav.splitandfillgenerator.component;

import com.cyberslav.splitandfillgenerator.*;

/**
 * Created by cyberslav on 09.12.17.
 */
public class DebugRectangleComponent implements MapComponent
{
    public DebugRectangleComponent(Rectangle rect)
    {
        _rect = rect;
    }

    public Rectangle _rect;
}
