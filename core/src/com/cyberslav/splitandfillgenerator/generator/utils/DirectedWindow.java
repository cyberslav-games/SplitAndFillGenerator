package com.cyberslav.splitandfillgenerator.generator.utils;

//import com.cyberslav.UnsignedCharacter.domain.content.resources.Settings;
//import com.cyberslav.UnsignedCharacter.foundation.geometry.*;

/**
 * Created by cyberslav on 28.07.17.
 */
public class DirectedWindow
{
    // public interface
    public DirectedWindow(
            Rectangle rect,
            Point.Direction direction,
            double startPosition,
            double endPosition)
    {
        // TODO: check parameters

        _rect = rect;
        _direction = direction;
        _startPosition = startPosition;
        _endPosition = endPosition;
    }


    public Rectangle getRect() { return _rect; }
    public Point.Direction getDirection() { return _direction; }
    public double getStartPosition() { return _startPosition; }
    public double getEndPosition() { return _endPosition; }
    public double getSize() { return _endPosition - _startPosition; }
    public boolean isOnHorizontalEdge() { return !Point.isHorizontalDirection(_direction); }

    @Override public String toString()
    {
//        double gridStep = Constants._gridStep;
        return String.format("{%s, %.0f, %.0f}", _direction, _startPosition /*/ gridStep*/, _endPosition /*/ gridStep*/);
    }

    public DirectedWindow toAnotherRect(Rectangle otherRect)
    {
//        boolean isHorizontal = !isOnHorizontalEdge();
//        boolean isPositive = _direction == Point.Direction.Right || _direction == Point.Direction.Down;
//        Point distance = isPositive
//                ? otherRect.getPosition().getSum(_rect.getPosition().getMult(-1))
//                : otherRect.getBottomRight().getSum(_rect.getBottomRight().getMult(-1));
//        double distProj = distance.getProjection(isHorizontal);
//
//        if ((Math.abs(distProj) > 0) && ((distProj >= 0) != isPositive))
//            return null;

        double otherRectStartPos = otherRect.getPositionProjection(isOnHorizontalEdge());
        double otherRectEndPos = otherRect.getBottomRightProjection(isOnHorizontalEdge());

        double thisRectPos = _rect.getPositionProjection(isOnHorizontalEdge());
        double globalStartPos = thisRectPos + _startPosition;
        double globalEndPos = thisRectPos + _endPosition;

        double maxGlobalStartPos = Math.max(globalStartPos, otherRectStartPos);
        double minGlobalEndPos = Math.min(globalEndPos, otherRectEndPos);

        if (maxGlobalStartPos >= minGlobalEndPos)
            return null;

        double startPos = maxGlobalStartPos - otherRectStartPos;
        double endPos = minGlobalEndPos - otherRectStartPos;

        return new DirectedWindow(otherRect, _direction, startPos, endPos);
    }


    public boolean isPointInside(DirectedPoint point) throws Exception
    {
        if (_rect != point.getRect())
            throw new Exception("try to check point from another rectangle");

        return _direction == point.getDirection()
                && _startPosition <= point.getPosition()
                && point.getPosition() <= _endPosition;
    }

    /*
    public DirectedWindow getIntersection(DirectedWindow other) throws Exception
    {
        if (_rect != other._rect)
            throw new Exception("try to intersect with window from another rectangle");

        if (_direction != other._direction)
            return null;

        double maxStartPos = Math.max(_startPosition, other._startPosition);
        double minEndPos = Math.min(_endPosition, other._endPosition);

        if (maxStartPos >= minEndPos)
            return null;

        return new DirectedWindow(_rect, _direction, maxStartPos, minEndPos);
    }
    */


    public static DirectedWindow createWindowOnSide(
            Rectangle rect,
            Point.Direction direction)
    {
        return new DirectedWindow(
            rect,
            direction,
            0,
            Point.isHorizontalDirection(direction)
                    ? rect.getHeight()
                    : rect.getWidth());
    }


    /*
    public static DirectedWindow createEnterWindow(
            Rectangle rect,
            Point.Direction direction)
    {
        switch (direction)
        {
            case Down:
            case Up:
                return new DirectedWindow(
                        rect,
                        direction,
                        0,
                        rect.getHeight());
            case Left:
            case Right:
            default:
                return new DirectedWindow(
                        rect,
                        direction,
                        0,
                        rect.getObjectWidth());
        }
    }
    */


    // private interface

    // data members
    private final Rectangle _rect;
    private final Point.Direction _direction;
    private final double _startPosition;
    private final double _endPosition;
}
