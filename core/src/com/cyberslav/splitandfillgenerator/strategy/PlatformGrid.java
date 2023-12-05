package com.cyberslav.splitandfillgenerator.strategy;

import com.cyberslav.splitandfillgenerator.*;
import com.cyberslav.splitandfillgenerator.component.*;

import java.util.*;

/**
 * Created by cyberslav on 03.10.17.
 */
public class PlatformGrid
{
    // public interface
    public PlatformGrid(
            Rectangle rect,
            boolean firstRowDisplacement,
            double leftShift,
            double maxWidth) throws MapGeneratorException
    {
        _rect = rect;
        _firstRowDisplacement = firstRowDisplacement;
        _leftShift = leftShift;
        _maxWidth = maxWidth;

        // calculate constants
        final double PLATFORM_WIDTH = getPlatformWidth();
        final double PLATFORM_HEIGHT = getPlatformHeight();
        final double V_STEP = getVerticalStep();
        final double H_STEP = getHorizontalStep();

        // create grid
        double width = _rect.getWidth();
        double height = _rect.getHeight() - getStartYPos();
        int rowCount = (int)((height - PLATFORM_HEIGHT) / V_STEP) + 1;
        final double DISPLACEMENT = H_STEP;
        NOT_DISPL_ROW_SIZE = 1 + (int)((width - PLATFORM_WIDTH) / (H_STEP * 2));
        DISPL_ROW_SIZE = 1 + (int)((width - PLATFORM_WIDTH - DISPLACEMENT) / (H_STEP * 2));
        _cells = new ArrayList<>();

        for (int rowNum = 0; rowNum < rowCount; ++rowNum)
        {
            _cells.add(new ArrayList<Cell>());
            int columnCount = getColumnCount(rowNum);

            for (int columnNum = 0; columnNum < columnCount; ++columnNum)
            {
                double xPos = leftShift + columnNum * H_STEP * 2 + (rowHasDisplacement(rowNum) ? DISPLACEMENT : 0);
                double yPos = getStartYPos() + rowNum * V_STEP;

                _cells.get(rowNum).add(new Cell(
                        rowNum,
                        columnNum,
                        new Rectangle(xPos, yPos, PLATFORM_WIDTH, PLATFORM_HEIGHT)));
            }
        }
    }


    public boolean isValid()
    {
        boolean hasExitCell = _exitCell != null;
        boolean exitCellIsReachable = hasExitCell && !_exitCell._isDisabled && _exitCell._isOnPath;
        return !_needExitCell || (hasExitCell && exitCellIsReachable);
    }


    public void blockRegion(Rectangle region) throws MapGeneratorException
    {
        _blockedRegions.add(region);

        for (int rowNum = 0; rowNum < getRowCount(); ++rowNum)
            for (int columnNum = 0; columnNum < getColumnCount(rowNum); ++columnNum)
                if (region.isStrictCollide(getCell(rowNum, columnNum)._rect))
                    getCell(rowNum, columnNum)._isDisabled = true;
    }


    public DirectedPoint build(
            DirectedPoint exitPoint,
            DirectedWindow exitWindow) throws MapGeneratorException
    {
        _originExitPoint = new DirectedPoint(
                exitPoint.getRect(),
                exitPoint.getDirection(),
                exitPoint.getPosition());

        // find exit cell
        findExitNear(exitPoint, exitWindow);

        _needExitCell = exitWindow.getDirection() != Point.Direction.Down;

        if (_exitCell != null && !isExitCellCorrect(_exitCell, exitPoint, exitWindow))
            _exitCell = null;

        if (exitWindow.getDirection() == Point.Direction.Up
                && (_exitCell == null || _exitCell._rowNum > 0))
            return exitPoint;

        // create control points
        ArrayList<Cell> controlCells = new ArrayList<>();

        if (_preExitCell != null)
            controlCells.add(_preExitCell);

        int maxControlCellCount = Math.max(getRowCount(), NOT_DISPL_ROW_SIZE);
        int minControlCellCount = (int)(Math.min(getRowCount(), NOT_DISPL_ROW_SIZE) / 1.5);
        int controlCellCount = minControlCellCount
                + MapRandom.getInstance().getNextInt(maxControlCellCount - minControlCellCount + 1);

        ArrayList<Cell> allCells = new ArrayList<>();

        for (int rowNum = 1; rowNum < getRowCount(); ++rowNum)
            for (int columnNum = 0; columnNum < getColumnCount(rowNum); ++columnNum)
                if (!getCell(rowNum, columnNum)._isDisabled)
                    allCells.add(getCell(rowNum, columnNum));

        MapRandom.getInstance().doRandomSort(allCells);

        for (int cellNum = 0; cellNum < Math.min(controlCellCount, allCells.size()); ++cellNum)
            controlCells.add(allCells.get(cellNum));

        // create paths to control points
        if (_exitCell != null && _exitCell != _preExitCell)
            _exitCell._isDisabled = true;

        for (Cell controlPoint : controlCells)
            createPath(controlPoint);

        if (_exitCell != null && _preExitCell != null)
        {
            _exitCell._isOnPath |= _preExitCell._isOnPath;

            if (_exitCell != _preExitCell)
                _exitCell._isDisabled = false;
        }

        // do post process
        doPostProcess();

        // return exit point
        if (_exitCell != null
                && _preExitCell != null
                && exitPoint.getDirection() == Point.Direction.Up)
        {
            double exitPointPos = (_exitCell._rect.getLeft() > _preExitCell._rect.getLeft())
                    ? _exitCell._rect.getLeft() - toGrid(get("PLAYER_WIDTH"))
                    : _exitCell._rect.getRight();

            return new DirectedPoint(_rect, Point.Direction.Up, exitPointPos);
        }

        if (_exitCell != null
                && !exitPoint.isOnHorizontalEdge()
                && _exitCell._rowNum != getRowCount() - 1
                && _exitCell._rect.getY() < exitPoint.getPosition())
            return new DirectedPoint(_rect, exitPoint.getDirection(), _exitCell._rect.getY());

        else return exitPoint;
    }



    public Collection<Rectangle> getPlatforms() throws MapGeneratorException
    {
        Collection<Rectangle> platforms = new ArrayList<>();

        for (int rowNum = 0; rowNum < getRowCount(); ++rowNum)
            for (int columnNum = 0; columnNum < getColumnCount(rowNum); ++columnNum)
                if (getCell(rowNum, columnNum)._isOnPath)
                    platforms.add(getCell(rowNum, columnNum)._rect);

        return platforms;
    }


    public Collection<Rectangle> getBottomPlatforms() throws MapGeneratorException
    {
        Collection<Rectangle> platforms = new ArrayList<>();

        final int rowNum = getRowCount() - 1;

        for (int columnNum = 0; columnNum < getColumnCount(rowNum); ++columnNum)
            if (getCell(rowNum, columnNum)._isOnPath)
                platforms.add(getCell(rowNum, columnNum)._rect);

        return platforms;
    }


    public static double getMaxShift(double width) throws MapGeneratorException
    {
        final double PLATFORM_WIDTH = getPlatformWidth();
        final double H_STEP = getHorizontalStep();
        final double MAX_SHIFT = Math.min(
                (width - PLATFORM_WIDTH) % (H_STEP * 2),
                (width - PLATFORM_WIDTH - H_STEP) % (H_STEP * 2));

        return MAX_SHIFT;
    }


    public static int getRowNumUnder(double yPos) throws MapGeneratorException
    {
        double verticalPos = Math.max(0, yPos - getStartYPos());
        return (int)((verticalPos) / getVerticalStep()) + 1;
    }

    public static double getPlatformWidth() throws MapGeneratorException
    {
        return toGrid(get("PLAYER_WIDTH") * 2);
    }

    public static double getPlatformHeight() throws MapGeneratorException
    {
        return toGrid(get("BORDER_SIZE"));
    }

    public static double getVerticalDistance() throws MapGeneratorException
    {
        return toGrid(get("JUMP_HEIGHT") - getPlatformHeight());
    }

    public static double getHorizontalDistance() throws MapGeneratorException
    {
        final double H = getVerticalStep();
        final double G = get("GRAVITY_FACTOR");
        final double v_speed = Math.sqrt(2 * G * H);
        final double top_point_time = v_speed / G;
        return toGridLess(top_point_time * get("RUN_SPEED"));
    }

    public static double getVerticalStep() throws MapGeneratorException
    {
        return getPlatformHeight() + getVerticalDistance();
    }

    public static double getHorizontalStep() throws MapGeneratorException
    {
        return getPlatformWidth() + getHorizontalDistance();
    }

    public static double getStartYPos() throws MapGeneratorException
    {
        return toGrid(get("TOP_PLATFORM_POS"));
    }


    // data types
    private class Cell
    {
        public Cell(int rowNum, int columnNum, Rectangle rect)
        {
            _rowNum = rowNum;
            _columnNum = columnNum;
            _rect = rect;
        }

        public final int _rowNum;
        public final int _columnNum;
        public Rectangle _rect;
        public boolean _isOnPath = false;
        public boolean _isDisabled = false;
    }

    // private interface

    private boolean isExitCellCorrect(
            Cell exitCell,
            DirectedPoint exitPoint,
            DirectedWindow exitWindow) throws MapGeneratorException
    {
        if (exitWindow.isOnHorizontalEdge())
            return true;

        Point normalExitPoint = exitPoint.toLocalPoint(false);

        double dx = Math.min(
                Math.abs(normalExitPoint.getX() - exitCell._rect.getLeft()),
                Math.abs(normalExitPoint.getX() - exitCell._rect.getRight()));
        double yPos = exitCell._rect.getY();
        double minYPos = exitWindow.getStartPosition() + get("PLAYER_HEIGHT");
        double maxYPos = normalExitPoint.getY() + get("JUMP_HEIGHT");

        return minYPos <= yPos && yPos <= maxYPos
                && dx <= getHorizontalDistance() * 2;
    }


    private void doPostProcess() throws MapGeneratorException
    {
        boolean exitIsOnTop
                = _originExitPoint != null
                && _originExitPoint.getDirection() == Point.Direction.Up
                && getPlatformWidth() <= _originExitPoint.getPosition()
                && _originExitPoint.getPosition() <= _maxWidth - getPlatformWidth()
                ;

        for (int rowNum = 0; rowNum < getRowCount(); ++rowNum)
            for (int columnNum = 0; columnNum < getColumnCount(rowNum); ++columnNum)
            {
                Cell cell = getCell(rowNum, columnNum);

                if (cell != _exitCell || !exitIsOnTop)
                    postProcessCell(cell);
            }
    }


    private void postProcessCell(Cell cell) throws MapGeneratorException
    {
        for (int isLeftInt = 0; isLeftInt <= 1; ++isLeftInt)
        {
            boolean isLeft = isLeftInt != 0;

            if (!hasPlatformAbove(cell, isLeft) && !hasPlatformBellow(cell, isLeft))
            {
                Cell rightBorderCell = null;

                for (int columnNum = cell._columnNum + 1;
                     columnNum < getColumnCount(cell._rowNum)
                        && rightBorderCell == null; ++columnNum)
                {
                    Cell rightCell = getCell(cell._rowNum, columnNum);

                    if (hasPlatformBellow(rightCell, false))
                        rightBorderCell = getCellBelow(rightCell, false);

                    if (hasPlatformAbove(rightCell, false))
                        rightBorderCell = getCellAbove(rightCell, false);

                    if (rightCell._isOnPath)
                        rightBorderCell = rightCell;

                    if (hasPlatformBellow(rightCell, true))
                        rightBorderCell = getCellBelow(rightCell, true);

                    if (hasPlatformAbove(rightCell, true))
                        rightBorderCell = getCellAbove(rightCell, true);
                }

                Cell leftBorderCell = null;

                for (int columnNum = cell._columnNum - 1;
                     columnNum >= 0 && leftBorderCell == null;
                     --columnNum)
                {
                    Cell leftCell = getCell(cell._rowNum, columnNum);

                    if (hasPlatformBellow(leftCell, true))
                        leftBorderCell = getCellBelow(leftCell, true);

                    if (hasPlatformAbove(leftCell, true))
                        leftBorderCell = getCellAbove(leftCell, true);

                    if (leftCell._isOnPath)
                        leftBorderCell = leftCell;

                    if (hasPlatformBellow(leftCell, false))
                        leftBorderCell = getCellBelow(leftCell, false);

                    if (hasPlatformAbove(leftCell, false))
                        leftBorderCell = getCellAbove(leftCell, false);
                }

                final double distance = getHorizontalDistance(); // * 2 + getPlatformWidth();

                final double minLeftBorder = 0;
                double leftBorder = (leftBorderCell != null)
                        ? leftBorderCell._rect.getRight() + distance : minLeftBorder;

                final double maxRightBorder = _maxWidth;
                double rightBorder = (rightBorderCell != null)
                        ? rightBorderCell._rect.getLeft() - distance : maxRightBorder;

                // TODO: продумать этот алгоритм лучше
                // платформа всё ещё может расширяться слишком сильно

                if (!isLeft && cell._rect.getLeft() <= minLeftBorder)
                    rightBorder = Math.min(rightBorder, maxRightBorder - 2.0 * get("PLAYER_WIDTH"));

                if (isLeft && cell._rect.getRight() >= maxRightBorder)
                    leftBorder = Math.max(leftBorder, minLeftBorder + 2.0 * get("PLAYER_WIDTH"));

                final double border = isLeft ? leftBorder : rightBorder;

                boolean forceExpand
                        = (isLeft
                            && !rowHasDisplacement(cell._rowNum)
                            && cell._columnNum == 0)
                        || (!isLeft
                            && !rowHasDisplacement(cell._rowNum, false)
                            && (cell._columnNum == getColumnCount(cell._rowNum) - 1));

                expandPlatform(
                        cell,
                        border,
                        isLeft,
                        forceExpand);
            }
            else if (!hasPlatformAbove(cell, isLeft) || !hasPlatformBellow(cell, isLeft))
            {
                Cell aboveCell = getCellAbove(cell, isLeft);
                Cell belowCell = getCellBelow(cell, isLeft);
                Cell otherCell = hasPlatformAbove(cell, isLeft) ? aboveCell : belowCell;

                if (belowCell != null && hasPlatformBellow(belowCell, !isLeft)
                        || belowCell != null && belowCell._rowNum == getRowCount() - 1
                        || cell._rowNum == getRowCount() - 1) //== _beginCell)
                    otherCell = null;

                if (otherCell != null)
                {
                    double border = isLeft ? otherCell._rect.getRight() : otherCell._rect.getLeft();
                    expandPlatform(cell, border, isLeft, false);
                }
            }
        }

        if (cell._rowNum == getRowCount() - 1
                && _rect.getHeight() - cell._rect.getBottom() < get("PLAYER_HEIGHT"))
            cell._rect.setHeight(_rect.getHeight() - cell._rect.getY());
    }


    private void expandPlatform(
            Cell cell,
            double border,
            boolean isLeft,
            boolean forceExpand) throws MapGeneratorException
    {
        // find max border that doesn't collide any blocked region
        double allowedBorder = border;
        Rectangle expandedRect = getExpandedRect(cell._rect, allowedBorder, isLeft);

        for (Rectangle blockedRegion : _blockedRegions)
            if (expandedRect.isStrictCollide(blockedRegion))
            {
                allowedBorder = isLeft ? blockedRegion.getRight() : blockedRegion.getLeft();
                expandedRect = getExpandedRect(cell._rect, allowedBorder, isLeft);
            }

        // expand platform to random allowed border
        double minBorder = isLeft ? cell._rect.getLeft() : cell._rect.getRight();
        double rate = MapRandom.getInstance().getNextDouble();

        //... in case of exit cell use maximum border to move platform to exit point
        if (cell == _exitCell || forceExpand)
            rate = 1;

        double randomBorder = toGrid(minBorder * (1 - rate) + allowedBorder * rate);
        cell._rect = getExpandedRect(cell._rect, randomBorder, isLeft);
    }


    private Rectangle getExpandedRect(Rectangle rect, double border, boolean isLeft) throws MapGeneratorException
    {
        double dist = isLeft ? rect.getLeft() - border : border - rect.getRight();
        Rectangle copyRect = new Rectangle(rect);

        if (dist <= 0)
            return copyRect;

        if (isLeft)
        {
            copyRect.setX(copyRect.getX() - dist);
            copyRect.setWidth(copyRect.getWidth() + dist);
        }
        else
            copyRect.setWidth(copyRect.getWidth() + dist);

        return copyRect;
    }


    private boolean hasPlatformBellow(Cell cell, boolean isLeft)
    {
        return getCellBelow(cell, isLeft) != null && getCellBelow(cell, isLeft)._isOnPath;
    }

    private boolean hasPlatformAbove(Cell cell, boolean isLeft)
    {
        return getCellAbove(cell, isLeft) != null && getCellAbove(cell, isLeft)._isOnPath;
    }


    private void createPath(Cell targetCell) throws MapGeneratorException
    {
        Queue<Cell> cells = new LinkedList<>();
        cells.add(targetCell);
        Map<Cell, Cell> nextCellMap = new HashMap<>();
        nextCellMap.put(targetCell, null);

        for ( ; !cells.isEmpty(); )
        {
            Cell cell = cells.element();
            cells.remove();

            if (cell._isOnPath
                    || ((cell._rowNum == (getRowCount() - 1)) && !cell._isDisabled))
            {
                cell._isOnPath = true;

                // restore path to target
                Cell pathCell = nextCellMap.get(cell);

                for ( ; pathCell != null; )
                {
                    pathCell._isOnPath = true;
                    pathCell = nextCellMap.get(pathCell);
                }

                return;
            }

            List<Cell> previousCells = new ArrayList<>();
            previousCells.add(getCellAbove(cell, true));
            previousCells.add(getCellAbove(cell, false));
            previousCells.add(getCellBelow(cell, false));
            previousCells.add(getCellBelow(cell, true));

            MapRandom.getInstance().doRandomSort(previousCells);

            for (Cell prevCell : previousCells)
                if (prevCell != null && !prevCell._isDisabled && !nextCellMap.containsKey(prevCell))
                {
                    nextCellMap.put(prevCell, cell);
                    cells.add(prevCell);
                }
        }
    }


    private void findExitNear(
            DirectedPoint exitPoint,
            DirectedWindow exitWindow) throws MapGeneratorException
    {
        if (exitPoint.getDirection() == Point.Direction.Down)
            return;

        // choose exit cell row
        final double exitYPos = exitPoint.toLocalPoint(false).getY();
        int exitRowNum
                = (exitPoint.getDirection() == Point.Direction.Up) ? 0 : getRowNumUnder(exitYPos);

        if (!exitPoint.isOnHorizontalEdge())
        {
            final boolean exitIsLeft = exitPoint.getDirection() == Point.Direction.Left;

            if (rowHasDisplacement(exitRowNum, exitIsLeft))
                exitRowNum--;
        }

        if (exitRowNum >= getRowCount())
            exitRowNum = getRowCount() - 1;

        // choose exit cell column
        double minDistance = Double.MAX_VALUE;
        int bestColumnNum = -1;
        final double exitXPos = exitPoint.toLocalPoint(false).getX();
        final double WIDTH_RESERVE = Math.max(0, toGrid(getPlatformWidth() / 2 - get("PLAYER_WIDTH")));
        final double minExitPos = exitWindow.isOnHorizontalEdge()
                ? exitWindow.getStartPosition() - WIDTH_RESERVE //getPlatformWidth() / 2
                : 0;
        final double maxExitPos = exitWindow.isOnHorizontalEdge()
                ? exitWindow.getEndPosition() + WIDTH_RESERVE //getPlatformWidth() / 2
                : _rect.getWidth();

        for (int columnNum = 0; columnNum < getColumnCount(exitRowNum); ++columnNum)
        {
            Cell cell = getCell(exitRowNum, columnNum);
            double cellXPos = (cell._rect.getLeft() + cell._rect.getRight()) / 2;
            double distance = Math.abs(exitXPos - cellXPos);

            if (!cell._isDisabled
                    && distance < minDistance
                    && minExitPos <= cellXPos && cellXPos <= maxExitPos)
            {
                minDistance = distance;
                bestColumnNum = columnNum;
            }
        }

        final int exitColumnNum = bestColumnNum;

        if (exitColumnNum < 0)
            return;

        _exitCell = getCell(exitRowNum, exitColumnNum);
        _preExitCell = _exitCell;

        // find pre exit cell if necessary
        if (exitPoint.getDirection() == Point.Direction.Up && _exitCell != null)
        {
            _preExitCell = null;
            double exitCellXPos = (_exitCell._rect.getLeft() + _exitCell._rect.getRight()) / 2;
            ArrayList<Cell> preExitCellVariants = new ArrayList<>();
            preExitCellVariants.add(getCellBelow(_exitCell, true));
            preExitCellVariants.add(getCellBelow(_exitCell, false));

            for (int cellNum = 0; cellNum < preExitCellVariants.size() && _preExitCell == null; ++cellNum)
            {
                Cell preExitCell = preExitCellVariants.get(cellNum);

                if (preExitCell != null && !preExitCell._isDisabled)
                {
                    double preExitCellXPos = (preExitCell._rect.getLeft() + preExitCell._rect.getRight()) / 2;
                    double middlePos = (exitCellXPos + preExitCellXPos) / 2;

                    if (minExitPos <= middlePos && middlePos <= maxExitPos)
                        _preExitCell = preExitCell;
                }
            }

            if (_preExitCell == null || _preExitCell._isDisabled)
                _exitCell = null;

            /*
            double cellXPos = (_exitCell._rect.getLeft() + _exitCell._rect.getRight()) / 2;
            boolean preExitCellIsLeft = cellXPos > exitXPos;
            _preExitCell = getCellBelow(_exitCell, preExitCellIsLeft);

            if (_preExitCell == null || _preExitCell._isDisabled)
                _exitCell = null;
            */
        }
    }


    private Cell getCell(int rowNum, int columnNum) throws MapGeneratorException
    {
        return _cells.get(rowNum).get(columnNum);
    }

    private int getRowCount()
    {
        return _cells.size();
    }

    private int getColumnCount(int rowNum)
    {
        return rowHasDisplacement(rowNum) ? DISPL_ROW_SIZE : NOT_DISPL_ROW_SIZE;
    }

    private boolean isPositionValid(int rowNum, int columnNum)
    {
        return 0 <= rowNum && rowNum < getRowCount()
                && 0 <= columnNum && columnNum < getColumnCount(rowNum);
    }

    private Cell getCellAbove(Cell cell, boolean isLeft)
    {
        int rowNum = cell._rowNum - 1;
        int dirDispl = isLeft ? 0 : 1;
        int rowDispl = rowHasDisplacement(rowNum) ? -1 : 0;
        int columnNum = cell._columnNum + dirDispl + rowDispl;

        return isPositionValid(rowNum, columnNum)
                ? _cells.get(rowNum).get(columnNum)
                : null;
    }

    private Cell getCellBelow(Cell cell, boolean isLeft)
    {
        int rowNum = cell._rowNum + 1;
        int dirDispl = isLeft ? 0 : 1;
        int rowDispl = rowHasDisplacement(rowNum) ? -1 : 0;
        int columnNum = cell._columnNum + dirDispl + rowDispl;

        return isPositionValid(rowNum, columnNum)
                ? _cells.get(rowNum).get(columnNum)
                : null;
    }

    private boolean rowHasDisplacement(int rowNum)
    {
        return (rowNum % 2 == 0) == _firstRowDisplacement;
    }

    private boolean rowHasDisplacement(int rowNum, boolean isLeft)
    {
        boolean isMinRow = getColumnCount(rowNum) < Math.max(DISPL_ROW_SIZE, NOT_DISPL_ROW_SIZE);

        return isLeft
                ? rowHasDisplacement(rowNum)
                : (isMinRow || ((DISPL_ROW_SIZE == NOT_DISPL_ROW_SIZE) && !rowHasDisplacement(rowNum)));

//        boolean isMaxRow = getColumnCount(rowNum) == Math.max(DISPL_ROW_SIZE, NOT_DISPL_ROW_SIZE);
//
//        return isLeft
//                ? rowHasDisplacement(rowNum)
//                : (rowHasDisplacement(rowNum) != isMaxRow);
    }


    private static double toGrid(double value) throws MapGeneratorException
    {
        return WorldProperties.getInstance().bindToGrid(value);
    }

    private static double toGridLess(double value) throws MapGeneratorException
    {
        return WorldProperties.getInstance().bindToGridLess(value);
    }

    private static double get(String name) throws MapGeneratorException
    {
        return WorldProperties.getInstance().get(name);
    }


    // data members
    private final Rectangle _rect;
    private final boolean _firstRowDisplacement;
    private final double _leftShift;
    private final double _maxWidth;
    private final ArrayList<ArrayList<Cell>> _cells;
    private final int NOT_DISPL_ROW_SIZE;
    private final int DISPL_ROW_SIZE;
    private Cell _exitCell = null;
    private Cell _preExitCell = null;
    private boolean _needExitCell = false;
    private ArrayList<Rectangle> _blockedRegions = new ArrayList<>();
    private DirectedPoint _originExitPoint;
}
