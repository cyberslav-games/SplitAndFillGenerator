package com.cyberslav.splitandfillgenerator;

//import com.cyberslav.UnsignedCharacter.domain.content.resources.Settings;
//import com.cyberslav.UnsignedCharacter.foundation.geometry.*;


/**
 * Created by cyberslav on 27.07.17.
 */
public class DirectedPoint
{
    // public interface
    public DirectedPoint(
            Rectangle rect,
            Point.Direction direction,
            double position)
    {
        _rect = rect;
        _direction = direction;
        _position = position;
    }

    public Rectangle getRect() { return _rect; }
    public double getPosition() { return _position; }
    public Point.Direction getDirection() { return _direction; }
    public boolean isOnHorizontalEdge() { return !Point.isHorizontalDirection(_direction); }

    @Override public String toString()
    {
//        double gridStep = Constants._gridStep;
        return String.format("{%s, %.0f}", _direction, _position /*/ gridStep*/);
    }


    public DirectedPoint toAnotherRect(Rectangle otherRect)
    {
        double rectStartPos = otherRect.getPositionProjection(isOnHorizontalEdge());
        double globalPos = _rect.getPositionProjection(isOnHorizontalEdge()) + _position;

        double pos = globalPos - rectStartPos;
        return new DirectedPoint(otherRect, _direction, pos);
    }

//    public void setDirection(Point.Direction direction) { _direction = direction; }
//    public void setPosition(double position) { _position = position; }


    public Point toLocalPoint(boolean isEnterPoint)
    {
        Point localPoint = new Point(0, 0);
        localPoint.setProjection(_position, isOnHorizontalEdge());

        boolean isPositiveDirection = _direction == Point.Direction.Right || _direction == Point.Direction.Down;

        if (isPositiveDirection != isEnterPoint)
            localPoint.setProjection(_rect.getSizeProjection(!isOnHorizontalEdge()), !isOnHorizontalEdge());

        return localPoint;
    }


    public Point toGlobalPoint(boolean isEnterPoint)
    {
//        Point localPoint = new Point(0, 0);
//        localPoint.setProjection(_position, isOnHorizontalEdge());
//
//        boolean isPositiveDirection = _direction == Point.Direction.Right || _direction == Point.Direction.Down;
//
//        if (isPositiveDirection != isEnterPoint)
//            localPoint.setProjection(_rect.getSize().getProjection(!isOnHorizontalEdge()), !isOnHorizontalEdge());

        Point localPoint = toLocalPoint(isEnterPoint);
        localPoint.add(_rect.getX(), _rect.getY());

        return localPoint;
    }


    /*
    public Point toPointOnBorder(Rectangle rect)
    {
        Point point = rect.getPosition().getSum(rect.getSize().getMult(0.5));
        Point dirPoint = Point.directionToPoint(_direction);
        point = point.getSum(
                new Point(
                        dirPoint.getX() * rect.getObjectWidth() * 0.5,
                        dirPoint.getY() * rect.getHeight() * 0.5));

        return point;
    }
    */

    // private interface

    // data members
    private final Rectangle _rect;
    private final Point.Direction _direction;
    private final double _position;
}
