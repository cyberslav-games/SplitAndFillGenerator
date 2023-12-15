package com.cyberslav.splitandfillgenerator.generator.utils;


/**
 * Created by cyberslav on 18.06.17.
 */
public class Point
{
    public static int _liveObjectCount = 0;
    public static int _totalObjectCount = 0;


    // public interface
    public enum Direction {
        Left,
        Right,
        Up,
        Down}


    public Point()
    {
        ++_liveObjectCount;
        ++_totalObjectCount;

        _x = 0;
        _y = 0;
    }

    public Point(double x, double y)
    {
        ++_liveObjectCount;
        ++_totalObjectCount;

        _x = x;
        _y = y;
    }

    public Point(Point other)
    {
        ++_liveObjectCount;
        ++_totalObjectCount;

        _x = other._x;
        _y = other._y;
    }

//    @Override protected void finalize() throws Throwable
//    {
//        --_liveObjectCount;
//    }

    public double getX()
    {
        return _x;
    }

    public double getY()
    {
        return _y;
    }

    public double getLength() { return Math.sqrt(_x * _x + _y * _y); }

    public double getProjection(boolean isHorizontal)
    {
        if (isHorizontal)
            return getX();
        else return getY();
    }

    /*
    public Point getSum(Point pt)
    {
        return new Point(
                _x + pt._x,
                _y + pt._y);
    }

    public Point getDiff(Point pt)
    {
        return new Point(
                _x - pt._x,
                _y - pt._y);
    }

    public Point getSum(double x, double y)
    {
        return new Point(
                _x + x,
                _y + y);
    }

    public Point getMult(double factor)
    {
        return new Point(
                _x * factor,
                _y * factor);
    }


    public Point getMult(double xFactor, double yFactor)
    {
        return new Point(
                _x * xFactor,
                _y * yFactor);
    }


    public Point getNormalized()
    {
        double length = getLength();

        if (length < 0.0000001)
            return new Point(1, 0);

        return new Point(getX() / length, getY() / length);
    }
    */


    @Override public String toString()
    {
        return String.format("[%.2f, %.2f]",
                getX(),
                getY());
    }


    public void copyFrom(Point other)
    {
        _x = other._x;
        _y = other._y;
    }


    public void reset(double x, double y)
    {
        _x = x;
        _y = y;
    }


    public void setX(double x)
    {
        _x = x;
    }


    public void setY(double y)
    {
        _y = y;
    }


    public void add(Point pt)
    {
        _x += pt.getX();
        _y += pt.getY();
    }

    public void add(double x, double y)
    {
        _x += x;
        _y += y;
    }

    public void subtract(Point pt)
    {
        _x -= pt.getX();
        _y -= pt.getY();
    }

    public void subtract(double x, double y)
    {
        _x -= x;
        _y -= y;
    }

    public void multiply(double factor)
    {
        _x *= factor;
        _y *= factor;
    }

    public void multiply(double x, double y)
    {
        _x *= x;
        _y *= y;
    }

    public void normalize()
    {
        double length = getLength();

        if (length < 0.0000001)
        {
            _x = 1;
            _y = 0;
        }
        else
        {
            _x /= length;
            _y /= length;
        }
    }


    public void setProjection(double value, boolean isHorizontal)
    {
        if (isHorizontal)
            setX(value);
        else setY(value);
    }


    public void addToProjection(double value, boolean isHorizontal)
    {
        setProjection(
                getProjection(isHorizontal) + value,
                isHorizontal);
    }


    public static Direction getOppositeDirection(Direction dir) //throws Exception
    {
        switch (dir)
        {
            case Down:  return Direction.Up;
            case Up:    return Direction.Down;
            case Left:  return Direction.Right;
            default:
            case Right: return Direction.Left;
        }
    }


    public static Point directionToPoint(Direction dir)
    {
        return new Point(
                (dir == Direction.Right) ? 1
                        : (dir == Direction.Left) ? -1 : 0,
                (dir == Direction.Down) ? 1
                        : (dir == Direction.Up) ? -1 : 0);
    }


    public static boolean isHorizontalDirection(Direction dir)
    {
        return dir == Direction.Right || dir == Direction.Left;
    }


    public static boolean isPositiveDirection(Direction dir)
    {
        return dir == Direction.Right || dir == Direction.Down;
    }


    public static double getLenght(double x, double y)
    {
        return Math.sqrt(x * x + y * y);
    }


    // data members
    private double _x = 0;
    private double _y = 0;
}
