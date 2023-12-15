package com.cyberslav.splitandfillgenerator.generator.utils;

/**
 * Created by cyberslav on 18.06.17.
 */
public class Rectangle
{
    // public interface
    public Rectangle()
    {
        _x = 0;
        _y = 0;
        _width = 0;
        _height = 0;
    }


    public Rectangle(double x, double y, double width, double height)
    {
        _x = x;
        _y = y;
        _width = width;
        _height = height;
    }


    public Rectangle(Rectangle other)
    {
        _x = other._x;
        _y = other._y;
        _width = other._width;
        _height = other._height;
    }


    public void writePosition(Point target)
    {
        target.setX(_x);
        target.setY(_y);
    }


    public void writeSize(Point target)
    {
        target.setX(_width);
        target.setY(_height);
    }


    public void writeBottomRight(Point target)
    {
        target.setX(_x + _width);
        target.setY(_y + _height);
    }


    public void writeCenter(Point target)
    {
        target.setX(_x + 0.5 * _width);
        target.setY(_y + 0.5 * _height);
    }


    public double getX()
    {
        return _x;
    }

    public double getY()
    {
        return _y;
    }

    public double getWidth()
    {
        return _width;
    }

    public double getHeight()
    {
        return _height;
    }

    public double getCenterX()
    {
        return _x + 0.5 * _width;
    }

    public double getCenterY()
    {
        return _y + 0.5 * _height;
    }

    public double getTop() { return getY(); }
    public double getBottom() { return getY() + getHeight(); }
    public double getRight() { return getX() + getWidth(); }
    public double getLeft() { return getX(); }
    public double getSquare() { return getWidth() * getHeight(); }


    public double getPositionProjection(boolean isHorizontal)
    {
        if (isHorizontal)
            return getX();
        else return getY();
    }


    public double getSizeProjection(boolean isHorizontal)
    {
        if (isHorizontal)
            return getWidth();
        else return getHeight();
    }


    public double getBottomRightProjection(boolean isHorizontal)
    {
        if (isHorizontal)
            return getRight();
        else return getBottom();
    }


    @Override public String toString()
    {
//        double gridStep = Settings.getInstance().getDouble("grid_step");
//
//        return String.format("[%.0f, %.0f, %.0f, %.0f]",
//                getX() / gridStep,
//                getY() / gridStep,
//                getWidth() / gridStep,
//                getHeight() / gridStep);

        return String.format("[%.0f, %.0f, %.0f, %.0f]",
                getX(),
                getY(),
                getWidth(),
                getHeight());
    }


    public boolean isStrictCollide(Rectangle other)
    {
        return !(other.getLeft() >= getRight() ||
                other.getRight() <= getLeft() ||
                other.getTop() >= getBottom() ||
                other.getBottom() <= getTop());
    }


    public boolean isInside(Point point)
    {
        return getLeft() <= point.getX() && point.getX() <= getRight()
                && getTop() <= point.getY() && point.getY() <= getBottom();
    }


    public boolean isOtherRectInside(Rectangle other)
    {
        return getLeft() <= other.getLeft() && other.getRight() <= getRight()
                && getTop() <= other.getTop() && other.getBottom() <= getBottom();
    }


    public void setPosition(double x, double y)
    {
        _x = x;
        _y = y;
    }

    public void setSize(double width, double height)
    {
        _width = width;
        _height = height;
    }

    public void setX(double value)
    {
        _x = value;
    }

    public void setY(double value)
    {
        _y = value;
    }

    public void setWidth(double value)
    {
        _width = value;
    }

    public void setHeight(double value)
    {
        _height = value;
    }

    public void setLeft(double left)
    {
        _width = getRight() - left;
        _x = left;
    }

    public void setTop(double top)
    {
        _height = getBottom() - top;
        _y = top;
    }

    public void setRight(double right)
    {
        _width = right - getLeft();
    }

    public void setBottom(double bottom)
    {
        _height = bottom - getTop();
    }


    public void reset(double x, double y, double width, double height)
    {
        _x = x;
        _y = y;
        _width = width;
        _height = height;
    }


    // data members
    private double _x = 0;
    private double _y = 0;
    private double _width = 0;
    private double _height = 0;
}
