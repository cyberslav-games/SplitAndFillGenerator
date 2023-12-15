package com.cyberslav.splitandfillgenerator.generator.component;


import com.cyberslav.splitandfillgenerator.generator.utils.Rectangle;

/**
 * Created by cyberslav on 01.12.17.
 */
public class RectangleComponent
{
    // public
    public Rectangle getRectangle()
    {
        return _rectangle;
    }


    // protected
    protected RectangleComponent(Rectangle rect)
    {
        _rectangle = rect;
    }

    protected void setRectangle(Rectangle rect)
    {
        _rectangle = rect;
    }

    // private
    private Rectangle _rectangle;
}
