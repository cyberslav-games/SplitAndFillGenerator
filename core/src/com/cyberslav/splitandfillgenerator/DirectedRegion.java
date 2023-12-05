package com.cyberslav.splitandfillgenerator;

//import com.cyberslav.UnsignedCharacter.foundation.geometry.Rectangle;

/**
 * Created by cyberslav on 01.12.17.
 */
public class DirectedRegion
{
    // public interface
    public DirectedRegion(
            Rectangle rect,
            DirectedPoint enterPoint,
            DirectedWindow exitWindow)
    {
        assert(enterPoint.getRect() == rect);
        assert(exitWindow.getRect() == rect);

        _rect = rect;
        _enterPoint = enterPoint;
        _exitWindow = exitWindow;
    }

    public Rectangle getRect() { return _rect; }
    public DirectedPoint getEnterPoint() { return _enterPoint; }
    public DirectedWindow getExitWindow() { return _exitWindow; }
    public DirectedPoint getExitPoint() { return _exitPoint; }


    @Override public String toString()
    {
        return String.format(
                "DR{%s %s %s}",
                _rect,
                _enterPoint,
                _exitWindow);
    }


    public void setExitPoint(DirectedPoint exitPoint)
    {
        assert(exitPoint.getRect() == _rect);
        _exitPoint = exitPoint;
    }

    // private interface

    // data members
    final private Rectangle _rect;
    final private DirectedPoint _enterPoint;
    final private DirectedWindow _exitWindow;
    private DirectedPoint _exitPoint;
}
