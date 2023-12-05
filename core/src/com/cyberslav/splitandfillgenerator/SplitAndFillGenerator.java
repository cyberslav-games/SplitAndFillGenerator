package com.cyberslav.splitandfillgenerator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.cyberslav.splitandfillgenerator.strategy.*;
import com.cyberslav.splitandfillgenerator.component.*;


/**
 * Created by cyberslav on 30.07.17.
 */
public class SplitAndFillGenerator implements MapGenerator
{
    // public interface
    // <editor-fold desc="public">
    public SplitAndFillGenerator() throws MapGeneratorException
    {
        // create strategies
        _strategyRegistry.add(new GridStrategy());
        _strategyRegistry.add(new FloorStrategy());
        _strategyRegistry.add(new PyramidStrategy());
        _strategyRegistry.add(new JumpPadStrategy());
        _strategyRegistry.add(new PlatformTreeStrategy());

        // calculate min strategy size
        for (FillStrategy strategy : _strategyRegistry)
        {
            _minStrategyHeight = Math.min(_minStrategyHeight, strategy.getMinHeight());
            _minStrategyWidth = Math.min(_minStrategyWidth, strategy.getMinWidth());
        }

        _minStrategyHeight = Math.max(
                _minStrategyHeight,
                get("GRID_STEP") * get("MIN_REGION_HEIGHT_CELLS"));

        _minStrategyWidth = Math.max(
                _minStrategyWidth,
                get("GRID_STEP") * get("MIN_REGION_WIDTH_CELLS"));

        System.out.printf("_minStrategyHeight = %f  _minStrategyWidth = %f\n", _minStrategyHeight, _minStrategyWidth);
    }


    public boolean canGenerateRegion(DirectedRegion region) throws MapGeneratorException
    {
        boolean onTheSameSide
                = region.getEnterPoint().getDirection()
                == Point.getOppositeDirection(region.getExitWindow().getDirection());

        return !onTheSameSide && findInitStrategy(region) != null;
    }


    public Collection<MapComponent> generateRegion(
            DirectedRegion region,
            DirectedWindow enterWindow) throws MapGeneratorException
    {
        // choose init strategy
        FillStrategy initStrategy = findInitStrategy(region);

        // start recursion
        _mapComponents = new ArrayList<>();
        RegionTree rootRegionNode = new RegionTree(region);

        processRegion(
                rootRegionNode,
                initStrategy,
                0);

        region.setExitPoint(
                rootRegionNode._region.getExitPoint().toAnotherRect(region.getRect()));

        addOuterBorder(region, rootRegionNode, enterWindow);

        Collection<MapComponent> mapComponents = _mapComponents;
        _mapComponents = null;
        return mapComponents;
    }


    public double getMinRegionWidth() throws MapGeneratorException
    {
        return _minStrategyWidth;
    }

    public double getMinRegionHeight() throws MapGeneratorException
    {
        return _minStrategyHeight;
    }



    public static boolean canEnter(
            DirectedPoint enterPoint,
            DirectedWindow enterWindow) throws MapGeneratorException
    {
        if (enterPoint.getRect() != enterWindow.getRect())
            throw new MapGeneratorException("canEnter: try to test window from another rectangle");

        if (enterPoint.getDirection() != enterWindow.getDirection())
            return false;

        double pos = enterPoint.getPosition();
        double start = enterWindow.getStartPosition();
        double end = enterWindow.getEndPosition();
        double width = WorldProperties.getInstance().get("PLAYER_WIDTH");
        double height = WorldProperties.getInstance().get("V_WINDOW_SIZE"); //"PLAYER_HEIGHT");

        if (enterWindow.isOnHorizontalEdge())
            return start <= pos && pos <= end - width;
        else
            return start + height <= pos && pos <= end;
    }


    public static boolean regionIsValidForStrategy(
            Rectangle rect,
            DirectedWindow exitWindow,
            FillStrategy strategy) throws MapGeneratorException
    {
        // check that player can move through exit window
        if (!canEnter(
                new DirectedPoint(
                        exitWindow.getRect(),
                        exitWindow.getDirection(),
                        exitWindow.isOnHorizontalEdge()
                                ? exitWindow.getStartPosition()
                                : exitWindow.getEndPosition()),
                exitWindow))
            return false;

        // check that rectangle has valid size
//        if (rect.getObjectWidth() < strategy.getMinWidth()
//                || rect.getHeight() < strategy.getMinHeight())
//            return false;
//
//        return true;

        return strategy.getMinWidth() <= rect.getWidth()
                && strategy.getMinHeight() <= rect.getHeight();
    }


    public static boolean canApplyStrategy(
            FillStrategy strategy,
            DirectedRegion region) throws MapGeneratorException
    {
        if (!regionIsValidForStrategy(region.getRect(), region.getExitWindow(), strategy))
            return false;

        Collection<DirectedWindow> enterWindows
                = strategy.tryFill(region.getRect(), region.getExitWindow());

        // check if there is enter window that covers enter position
        for (DirectedWindow enterWindow : enterWindows)
            if (canEnter(region.getEnterPoint(), enterWindow))
                return true;

        return false;
    }


    // </editor-fold>

    // private interface
    // <editor-fold desc="data types">

    private class RegionTree
    {
        public RegionTree(DirectedRegion region)
        {
            _region = region;
        }

        public DirectedRegion _region;
        public RegionTree _enterSubregionNode;
        public RegionTree _exitSubregionNode;
    }


    private class SplitVariant
    {
        public SplitVariant(
                Rectangle enterRect,
                Rectangle exitRect,
                Point.Direction traverseDirection)
        {
            _enterRect = enterRect;
            _exitRect = exitRect;
            _traverseDirection = traverseDirection;
        }

        public final Rectangle _enterRect;
        public final Rectangle _exitRect;
        public final Point.Direction _traverseDirection;
    }


    private class StrategyPair
    {
        StrategyPair(
                FillStrategy enterStrategy,
                FillStrategy exitStrategy,
                DirectedWindow traverseWindow)
        {
            _enterStrategy = enterStrategy;
            _exitStrategy = exitStrategy;
            _traverseWindow = traverseWindow;
        }

        public final FillStrategy _enterStrategy;
        public final FillStrategy _exitStrategy;
        public final DirectedWindow _traverseWindow; // must be bound to enter subrectangle
    }


    private class CutVariant
    {
        public CutVariant(
                Rectangle mainRect,
                Rectangle cutRect,
                Point.Direction direction)
        {
            _mainRect = mainRect;
            _cutRect = cutRect;
            _direction = direction;
        }

        public final Rectangle _mainRect;
        public final Rectangle _cutRect;
        public final Point.Direction _direction;
    }

    // </editor-fold>


    // <editor-fold desc="main">

    private FillStrategy findInitStrategy(
            DirectedRegion region) throws MapGeneratorException
    {
        ArrayList<FillStrategy> validStrategies = new ArrayList<>();

        for (FillStrategy strategy : _strategyRegistry)
            if (canApplyStrategy(strategy, region))
                validStrategies.add(strategy);

        return validStrategies.isEmpty()
                ? null
                : validStrategies.get(MapRandom.getInstance().getNextInt(validStrategies.size()));
    }


    private void processRegion(
            RegionTree regionNode,
            FillStrategy initStrategy,
            int debugPrintLevel) throws MapGeneratorException
    {
        // try split
        if (trySplit(regionNode, debugPrintLevel))
            return;

        if (tryCut(regionNode))
            return;

        // apply init strategy
        applyStrategy(initStrategy, regionNode._region);
    }



    private void applyStrategy(
            FillStrategy strategy,
            DirectedRegion region) throws MapGeneratorException
    {
        if (!canApplyStrategy(strategy, region))
        {
            System.out.println(strategy.getClass().getSimpleName());
            System.out.println(region.getRect());
            System.out.println(region.getEnterPoint());
            System.out.println(region.getExitWindow());
            throw new MapGeneratorException("strategy couldn't be applied to this region");
        }

        Collection<MapComponent> components = new ArrayList<>();
        Collection<DirectedPoint> exitPoints = strategy.fill(
                region,
                components);

        _mapComponents.addAll(components);

        // find compatible exit point
        ArrayList<DirectedPoint> mixedExitPoints = new ArrayList<>(exitPoints);
        MapRandom.getInstance().doRandomSort(mixedExitPoints);
        DirectedPoint exitPoint = null;

        for (int pointNum = 0; pointNum < mixedExitPoints.size() && exitPoint == null; ++pointNum)
            if (canEnter(mixedExitPoints.get(pointNum), region.getExitWindow()))
                exitPoint = mixedExitPoints.get(pointNum);

        if (exitPoint == null)
        {
            System.out.println(strategy.getClass().getSimpleName());
            System.out.println(region.getRect());
            System.out.println(region.getEnterPoint());
            System.out.println(region.getExitWindow());
            throw new MapGeneratorException("strategy didn't create compatible exit point");
        }

        region.setExitPoint(exitPoint);

        // create debug region
        _mapComponents.add(
                new DebugRegionComponent(
                        region,
                        strategy.getClass().getSimpleName()
                                + "\n" + region.getRect().toString()
                                + "\n" + region.getEnterPoint().toString()
                                + "\n" + region.getExitWindow().toString()));
    }

    // </editor-fold>


    // <editor-fold desc="split">


    private boolean trySplit(
            RegionTree regionNode,
            int debugPrintLevel) throws MapGeneratorException
    {
        DirectedPoint enterPoint = regionNode._region.getEnterPoint();
        DirectedWindow exitWindow = regionNode._region.getExitWindow();

        if (enterPoint.getDirection() == exitWindow.getDirection()
                && !enterPoint.isOnHorizontalEdge()
//                && toGrid(enterPoint.getPosition()) == toGrid(exitWindow.getEndPosition())
                )
            return false;

        List<SplitVariant> variants = getSplitVariants(regionNode._region);

        for (SplitVariant variant : variants)
            if (trySplitVariant(regionNode, variant, debugPrintLevel))
                return true;

        return false;
    }


    private ArrayList<FillStrategy> chooseStrategies()
    {
        ArrayList<FillStrategy> strategies = new ArrayList<>();

        for (FillStrategy strategy : _strategyRegistry)
            if (MapRandom.getInstance().getNextDouble(0.0, 1.0) <= strategy.getUseProbability())
                strategies.add(strategy);

        return strategies;
    }


    private boolean trySplitVariant(
            RegionTree regionNode,
            SplitVariant splitVariant,
            int debugPrintLevel) throws MapGeneratorException
    {
        // check parts square
        final double MIN_SQUARE = get("MIN_SPLIT_SQUARE");

        if (splitVariant._enterRect.getSquare() < MIN_SQUARE
                || splitVariant._exitRect.getSquare() < MIN_SQUARE)
            return false;

        // convert exit window to exit rect
        DirectedWindow localExitWindow = regionNode._region.getExitWindow().toAnotherRect(splitVariant._exitRect);

        if (localExitWindow == null)
            return false;

        // try regular split
        ArrayList<StrategyPair> validPairs = new ArrayList<>();

        // try different exit strategies
//        for (FillStrategy exitStrategy : _strategyRegistry)
        for (FillStrategy exitStrategy : chooseStrategies())
        {
            boolean exitRegionIsValid = regionIsValidForStrategy(
                    splitVariant._exitRect,
                    localExitWindow,
                    exitStrategy);

            Collection<DirectedWindow> exitCompEnterWindows = exitRegionIsValid
                    ? exitStrategy.tryFill(splitVariant._exitRect, localExitWindow)
                    : new ArrayList<DirectedWindow>();

            // for each possible enter window of exit strategy
            // try to find enter strategy, using this window as exit window
            for (DirectedWindow exitCompEnterWindow : exitCompEnterWindows)
            {
                // ignore windows that has unsuitable direction
                if (exitCompEnterWindow.getDirection() == splitVariant._traverseDirection)
                {
                    DirectedWindow traverseWindow = exitCompEnterWindow.toAnotherRect(splitVariant._enterRect);

                    if (traverseWindow != null
                            && traverseWindow.getDirection()
                                != Point.getOppositeDirection(localExitWindow.getDirection()))
                    {
                        // in enter strategies
//                        for (FillStrategy enterStrategy : _strategyRegistry)
                        for (FillStrategy enterStrategy : chooseStrategies())
                        {
                            boolean canApply = canApplyStrategy(
                                    enterStrategy,
                                    new DirectedRegion(
                                            splitVariant._enterRect,
                                            regionNode._region.getEnterPoint().toAnotherRect(splitVariant._enterRect),
                                            traverseWindow));

                            if (canApply)
                                validPairs.add(new StrategyPair(
                                        enterStrategy,
                                        exitStrategy,
                                        traverseWindow));
                        }
                    }
                }
            }
        }

        if (validPairs.isEmpty())
            return false;

        // apply split with random pair of strategies
        StrategyPair strategyPair = validPairs.get(MapRandom.getInstance().getNextInt(validPairs.size()));

        //... process enter subregion
        RegionTree enterSubregionNode = new RegionTree(
                new DirectedRegion(
                        splitVariant._enterRect,
                        regionNode._region.getEnterPoint().toAnotherRect(splitVariant._enterRect),
                        strategyPair._traverseWindow));
        regionNode._enterSubregionNode = enterSubregionNode;

        processRegion(
                regionNode._enterSubregionNode,
                strategyPair._enterStrategy,
                debugPrintLevel + 1);

        //... process exit subregion
        RegionTree exitSubregionNode = new RegionTree(
                new DirectedRegion(
                        splitVariant._exitRect,
                        enterSubregionNode._region.getExitPoint().toAnotherRect(splitVariant._exitRect),
                        localExitWindow.toAnotherRect(splitVariant._exitRect)));
        regionNode._exitSubregionNode = exitSubregionNode;

        processRegion(
                regionNode._exitSubregionNode,
                strategyPair._exitStrategy,
                debugPrintLevel + 1);

        regionNode._region.setExitPoint(
                exitSubregionNode._region.getExitPoint().toAnotherRect(
                        regionNode._region.getRect()));

        // make border
        addBorder(
                regionNode,
                strategyPair._traverseWindow);

        return true;
    }


    private List<SplitVariant> getSplitVariants(
            DirectedRegion region) throws MapGeneratorException
    {
        ArrayList<SplitVariant> variants = new ArrayList<>();

        final double GRID_STEP = WorldProperties.getInstance().get("GRID_STEP");

        final Point MIN_STRATEGY_SIZE = new Point(
                getMinStrategyWidth(),
                getMinStrategyHeight());

        // vertical split
        //... make common vertical range
        double verticalWindowSize = get("V_WINDOW_SIZE");
        double verticalRangeStart = MIN_STRATEGY_SIZE.getY();
        double verticalRangeEnd = region.getRect().getHeight() - verticalWindowSize;

        //if enter point is on vertical side try split below and above enter position
        if (Point.isHorizontalDirection(region.getEnterPoint().getDirection()))
        {
            // try split above
            double firstVertRangeStart = verticalRangeStart;
            double firstVertRangeEnd = region.getEnterPoint().getPosition() - verticalWindowSize; //PLAYER_SIZE.getY();
            firstVertRangeEnd = Math.min(firstVertRangeEnd, verticalRangeEnd);

            addSplitInRange(
                    variants,
                    region,
                    firstVertRangeStart,
                    firstVertRangeEnd,
                    false);

            // try split below
            double secondVertRangeStart = region.getEnterPoint().getPosition() + GRID_STEP;
            secondVertRangeStart = Math.max(secondVertRangeStart, verticalRangeStart);
            double secondVertRangeEnd = verticalRangeEnd;

            addSplitInRange(
                    variants,
                    region,
                    secondVertRangeStart,
                    secondVertRangeEnd,
                    false);
        }
        else
        {
            addSplitInRange(
                    variants,
                    region,
                    verticalRangeStart,
                    verticalRangeEnd,
                    false);
        }

        // horizontal split
        //... make common horizontal range
        double horizontalRangeStart = MIN_STRATEGY_SIZE.getX();
        double horizontalRangeEnd = region.getRect().getWidth() - MIN_STRATEGY_SIZE.getX();

        // if enter point is on horizontal side try split left and right from enter position
        if (!Point.isHorizontalDirection(region.getEnterPoint().getDirection()))
        {
            final double DISPLACEMENT = toGrid(get("H_WINDOW_DISPLACEMENT"));

            // try split left
            double firstHorizRangeStart = horizontalRangeStart;
            double firstHorizRangeEnd = region.getEnterPoint().getPosition() - DISPLACEMENT;
            firstHorizRangeEnd = Math.min(firstHorizRangeEnd, horizontalRangeEnd);

            addSplitInRange(
                    variants,
                    region,
                    firstHorizRangeStart,
                    firstHorizRangeEnd,
                    true);

            // try split right
            double secondHorizRangeStart
                    = region.getEnterPoint().getPosition()
                    + toGrid(get("PLAYER_WIDTH"))
                    + DISPLACEMENT;
            secondHorizRangeStart = Math.max(secondHorizRangeStart, horizontalRangeStart);
            double secondHorizRangeEnd = horizontalRangeEnd;

            addSplitInRange(
                    variants,
                    region,
                    secondHorizRangeStart,
                    secondHorizRangeEnd,
                    true);
        }
        else
        {
            addSplitInRange(
                    variants,
                    region,
                    horizontalRangeStart,
                    horizontalRangeEnd,
                    true);
        }

        // do random sort
        double horizontalWeight = region.getRect().getWidth() / region.getRect().getHeight();
        double verticalWeight = region.getRect().getHeight() / region.getRect().getWidth();
        horizontalWeight *= horizontalWeight;
        verticalWeight *= verticalWeight;

        ArrayList<Double> weights = new ArrayList<>();

        for (SplitVariant variant : variants)
            if (Point.isHorizontalDirection(variant._traverseDirection))
                weights.add(horizontalWeight);
            else
                weights.add(verticalWeight);

        doRandomWeighedSort(variants, weights);
        //doRandomSort(variants);

        return variants;
    }


    private void addSplitInRange(
            ArrayList<SplitVariant> variants,
            DirectedRegion region,
            double rangeStart,
            double rangeEnd,
            boolean isHorizontal) throws MapGeneratorException
    {
        double rangeStartB = WorldProperties.getInstance().bindToGrid(rangeStart);
        double rangeEndB = WorldProperties.getInstance().bindToGrid(rangeEnd);

        if (canMakeSplitInRange(region, rangeStartB, rangeEndB, isHorizontal))
            variants.add(makeSplitInRange(
                    region,
                    rangeStartB,
                    rangeEndB,
                    isHorizontal));
    }


    private boolean canMakeSplitInRange(
            DirectedRegion region,
            double rangeStart,
            double rangeEnd,
            boolean isHorizontal) throws MapGeneratorException
    {
        double MIN_WIDTH = getMinStrategyWidth();
        double MIN_HEIGHT = getMinStrategyHeight();
        double len = region.getRect().getSizeProjection(isHorizontal);

        if (isHorizontal)
            return rangeStart >= MIN_WIDTH
                    && (len - rangeEnd) >= MIN_WIDTH
                    && (toGridLess(rangeEnd) - toGridLess(rangeStart)) >= get("BORDER_SIZE");
        else
            return rangeStart >= MIN_HEIGHT
                    && (len - rangeEnd) >= MIN_HEIGHT
                    && (toGridLess(rangeEnd) - toGridLess(rangeStart)) >= get("BORDER_SIZE");
    }


    private SplitVariant makeSplitInRange(
            DirectedRegion region,
            double rangeStart,
            double rangeEnd,
            boolean isHorizontal) throws MapGeneratorException
    {
        final double BORDER_SIZE = get("BORDER_SIZE");

        // calculate first part size
//        double firstPartSize = rangeStart + _random.nextDouble() * (rangeEnd - rangeStart);
        double fullSize = region.getRect().getSizeProjection(isHorizontal);
        double anotherFullSize = region.getRect().getSizeProjection(!isHorizontal);
        double strategySizeBorder = isHorizontal ? getMinStrategyWidth() : getMinStrategyHeight();
        double squareBorder = get("MIN_SPLIT_SQUARE") / anotherFullSize;
        double border = Math.max(strategySizeBorder, squareBorder);
        double actualRangeStart = Math.max(rangeStart, border);
        double actualRangeEnd = Math.min(rangeEnd, fullSize - border);

        double firstPartSize = MapRandom.getInstance().getNextNormal(
                fullSize / 2,
                fullSize * get("SPLIT_DEVIATION_RATE"));
        firstPartSize = Math.max(firstPartSize, actualRangeStart);
        firstPartSize = Math.min(firstPartSize, actualRangeEnd);
        firstPartSize = Math.min(firstPartSize, actualRangeEnd - BORDER_SIZE);
        firstPartSize = WorldProperties.getInstance().bindToGrid(firstPartSize);

        // create first & second subrectangle
        Rectangle firstSubrect;
        Rectangle secondSubrect;
        Rectangle rect = region.getRect();

        if (isHorizontal)
        {
            firstSubrect = new Rectangle(
                    rect.getX(),
                    rect.getY(),
                    firstPartSize,
                    rect.getHeight());

            secondSubrect = new Rectangle(
                    rect.getX() + firstPartSize + BORDER_SIZE,
                    rect.getY(),
                    rect.getWidth() - firstPartSize - BORDER_SIZE,
                    rect.getHeight());
        }
        else
        {
            firstSubrect = new Rectangle(
                    rect.getX(),
                    rect.getY(),
                    rect.getWidth(),
                    firstPartSize);

            secondSubrect = new Rectangle(
                    rect.getX(),
                    rect.getY() + firstPartSize + BORDER_SIZE,
                    rect.getWidth(),
                    rect.getHeight() - firstPartSize - BORDER_SIZE);
        }

        // choose enter & exit subrectangle
        Rectangle enterRect;
        Rectangle exitRect;
        boolean enterPartIsFirst; // = region.getEnterPoint().getPosition() < firstPartSize;
        Point.Direction enterDir = region.getEnterPoint().getDirection();

        if (Point.isHorizontalDirection(enterDir) == !isHorizontal)
            enterPartIsFirst = region.getEnterPoint().getPosition() < firstPartSize;
        else
            enterPartIsFirst = enterDir == Point.Direction.Right || enterDir == Point.Direction.Down;

        if (enterPartIsFirst)
        {
            enterRect = firstSubrect;
            exitRect = secondSubrect;
        }
        else
        {
            enterRect = secondSubrect;
            exitRect = firstSubrect;
        }

        // determine traverse direction
        Point.Direction traverseDirection
                = isHorizontal
                ? (enterPartIsFirst ? Point.Direction.Right : Point.Direction.Left)
                : (enterPartIsFirst ? Point.Direction.Down : Point.Direction.Up);

        return new SplitVariant(enterRect, exitRect, traverseDirection);
    }

    // </editor-fold>


    // <editor-fold desc="cut">

    private boolean tryCut(
//            DirectedRegion region,
            RegionTree regionNode) throws MapGeneratorException
    {
        FillStrategy finalStrategy = null;

        // create direction use mask
        final int DIR_COUNT = Point.Direction.values().length;
        boolean[] dirUseMask = new boolean[DIR_COUNT];

        for (Point.Direction dir : Point.Direction.values())
            dirUseMask[dir.ordinal()] = false;

        // try different directions
        boolean hasCut = true;
        double rate = get("CUT_RATE");
        DirectedRegion currentRegion = regionNode._region;

        for (int usedDirectionCount = 0; hasCut && usedDirectionCount < DIR_COUNT && rate > 0; )
        {
            // try different variants
            List<CutVariant> filledCutVariants
                    = getCutVariants(currentRegion, rate);

            FillStrategy cutStrategy = null;
            CutVariant cutVariant = null;

            for (int variantNum = 0; variantNum < filledCutVariants.size() && cutStrategy == null; ++variantNum)
            {
                CutVariant variant = filledCutVariants.get(variantNum);

                if (!dirUseMask[variant._direction.ordinal()])
                {
                    cutVariant = variant;
                    cutStrategy = getStrategyForCutVariant(currentRegion, cutVariant);
                }
            }

            // do cut
            hasCut = cutStrategy != null;

            if (hasCut)
            {
                finalStrategy = cutStrategy;

                currentRegion = new DirectedRegion(
                        cutVariant._mainRect,
                        currentRegion.getEnterPoint().toAnotherRect(cutVariant._mainRect),
                        currentRegion.getExitWindow().toAnotherRect(cutVariant._mainRect));

                _mapComponents.add(new PlatformComponent(cutVariant._cutRect));

                ++usedDirectionCount;
                dirUseMask[cutVariant._direction.ordinal()] = true;
                rate -= cutVariant._cutRect.getSquare() / currentRegion.getRect().getSquare();
            }
        }

        if (finalStrategy == null)
            return false;

        regionNode._region = currentRegion;
        applyStrategy(finalStrategy, currentRegion);
        return true;
    }


    private FillStrategy getStrategyForCutVariant(
            DirectedRegion region,
            CutVariant variant) throws MapGeneratorException
    {
        DirectedPoint enterPoint = region.getEnterPoint().toAnotherRect(variant._mainRect);
        DirectedWindow localExitWindow = region.getExitWindow().toAnotherRect(variant._mainRect);

        if (enterPoint == null || localExitWindow == null)
            return null;

        ArrayList<FillStrategy> validStrategies = new ArrayList<>();

        for (FillStrategy strategy : chooseStrategies() /*_strategyRegistry*/)
            if (canApplyStrategy(
                    strategy,
                    new DirectedRegion(
                            variant._mainRect,
                            enterPoint,
                            localExitWindow)
                    ))
                validStrategies.add(strategy);

        MapRandom.getInstance().doRandomSort(validStrategies);

        return validStrategies.isEmpty() ? null : validStrategies.get(0);
    }


    private List<CutVariant> getCutVariants(
            DirectedRegion region,
            double cutRate) throws MapGeneratorException
    {
        final Point MIN_STRATEGY_SIZE = new Point(
                getMinStrategyWidth(),
                getMinStrategyHeight());

        final Point PLAYER_SIZE = new Point(
                toGrid(get("PLAYER_WIDTH")),
                get("V_WINDOW_SIZE"));

        final double WINDOW_DISPLACEMENT = toGrid(get("H_WINDOW_DISPLACEMENT"));

        ArrayList<CutVariant> variants = new ArrayList<>();

        Point.Direction exitDirection = region.getExitWindow().getDirection();
        Point.Direction enterDirection = Point.getOppositeDirection(region.getEnterPoint().getDirection());

        // try cut top part
        if (Point.Direction.Up != exitDirection && Point.Direction.Up != enterDirection)
        {
            double cutSize = Math.min(
                    region.getEnterPoint().toLocalPoint(true).getY() - PLAYER_SIZE.getY(),
                    region.getRect().getHeight() - MIN_STRATEGY_SIZE.getY());

            if (!region.getExitWindow().isOnHorizontalEdge())
                cutSize = Math.min(cutSize, region.getExitWindow().getEndPosition() - PLAYER_SIZE.getY());

            addCutVariant(
                    variants,
                    region.getRect(),
                    Point.Direction.Up,
                    cutSize,
                    cutRate);
        }

        // try cut bottom part
        if (Point.Direction.Down != exitDirection && Point.Direction.Down != enterDirection)
        {
            double cutSize = Math.min(
                    region.getRect().getHeight() - region.getEnterPoint().toLocalPoint(true).getY(),
                    region.getRect().getHeight() - MIN_STRATEGY_SIZE.getY());

            if (!region.getExitWindow().isOnHorizontalEdge())
                cutSize = Math.min(
                        cutSize,
                        region.getRect().getHeight() - (region.getExitWindow().getStartPosition() + PLAYER_SIZE.getY()));

            addCutVariant(
                    variants,
                    region.getRect(),
                    Point.Direction.Down,
                    cutSize,
                    cutRate);
        }

        // try cut left part
        if (Point.Direction.Left != exitDirection && Point.Direction.Left != enterDirection)
        {
            double cutSize = Math.min(
                    region.getEnterPoint().toLocalPoint(true).getX() - WINDOW_DISPLACEMENT,
                    region.getRect().getWidth() - MIN_STRATEGY_SIZE.getX());

            if (region.getExitWindow().isOnHorizontalEdge())
                cutSize = Math.min(cutSize, region.getExitWindow().getEndPosition() - WINDOW_DISPLACEMENT);

            addCutVariant(
                    variants,
                    region.getRect(),
                    Point.Direction.Left,
                    cutSize,
                    cutRate);
        }

        // try cut right part
        if (Point.Direction.Right != exitDirection && Point.Direction.Right != enterDirection)
        {
            double cutSize = Math.min(
                    region.getRect().getWidth()
                            - (region.getEnterPoint().toLocalPoint(true).getX()
                                + toGrid(get("PLAYER_WIDTH"))
                                + WINDOW_DISPLACEMENT),
                    region.getRect().getWidth() - MIN_STRATEGY_SIZE.getX());

            if (region.getExitWindow().isOnHorizontalEdge())
                cutSize = Math.min(
                        cutSize,
                        region.getRect().getWidth()
                                - (region.getExitWindow().getStartPosition()
                                    + toGrid(get("PLAYER_WIDTH"))
                                    + WINDOW_DISPLACEMENT));

            addCutVariant(
                    variants,
                    region.getRect(),
                    Point.Direction.Right,
                    cutSize,
                    cutRate);
        }

        // do random sort
        ArrayList<Double> weights = new ArrayList<>();

        for (CutVariant variant : variants)
        {
            double cutSquare = variant._cutRect.getWidth() * variant._cutRect.getHeight();
            weights.add(cutSquare * cutSquare);
        }

        doRandomWeighedSort(variants, weights);

        return variants;
    }


    private void addCutVariant(
            ArrayList<CutVariant> variants,
            Rectangle rect,
            Point.Direction direction,
            double maxCutSize,
            double cutRate) throws MapGeneratorException
    {
        boolean isHorizontal = Point.isHorizontalDirection(direction);
        boolean isPositive = Point.isPositiveDirection(direction);
        double fullSize = rect.getSizeProjection(isHorizontal);
        double deviation = Math.min(getMinStrategyHeight(), getMinStrategyWidth()) / 4;

        double cutSize = MapRandom.getInstance().getNextNormal(fullSize * cutRate, deviation);
        cutSize = Math.abs(cutSize);
        cutSize = toGrid(cutSize);
        cutSize = Math.min(cutSize, toGrid(maxCutSize));

        if (cutSize <= 0)
            return;

        // create main rect
        Point mainRectSize = new Point(rect.getWidth(), rect.getHeight());
        mainRectSize.setProjection(fullSize - cutSize, isHorizontal);
        Point mainRectPos = new Point(rect.getX(), rect.getY());

        if (!isPositive)
            mainRectPos.setProjection(mainRectPos.getProjection(isHorizontal) + cutSize, isHorizontal);

        Rectangle mainRect = new Rectangle(
                mainRectPos.getX(),
                mainRectPos.getY(),
                mainRectSize.getX(),
                mainRectSize.getY());

        // create cut rect
        Point cutRectSize = new Point(rect.getWidth(), rect.getHeight());
        cutRectSize.setProjection(cutSize, isHorizontal);
        Point cutRectPos = new Point(rect.getX(), rect.getY());

        if (isPositive)
            cutRectPos.setProjection(
                    cutRectPos.getProjection(isHorizontal) + fullSize - cutSize,
                    isHorizontal);

        Rectangle cutRect = new Rectangle(
                cutRectPos.getX(),
                cutRectPos.getY(),
                cutRectSize.getX(),
                cutRectSize.getY());

        variants.add(new CutVariant(mainRect, cutRect, direction));
    }


//    private double getNormalRandom(double mean, double deviation)
//    {
//        return _random.nextGaussian() * deviation + mean;
//    }

    // </editor-fold>



    // <editor-fold desc="borders">

    private void addOuterBorder(
            DirectedRegion outerRegion,
            RegionTree rootRegionNode,
            DirectedWindow outerEnterWindow) throws MapGeneratorException
    {
        Point.Direction enterDirection = outerRegion.getEnterPoint().getDirection();
        Point.Direction exitDirection = outerRegion.getExitWindow().getDirection();

        assert(enterDirection != Point.getOppositeDirection(exitDirection));

        final double BORDER_SIZE = get("BORDER_SIZE");
        Rectangle rect = outerRegion.getRect();

        for (Point.Direction dir : Point.Direction.values())
        {
            boolean isHorizontal = Point.isHorizontalDirection(dir);
            double posDisplacement = Point.isPositiveDirection(dir)
                    ? rect.getSizeProjection(isHorizontal)
                    : -BORDER_SIZE;

            Point pos = new Point(0, 0);
            pos.setProjection(posDisplacement, isHorizontal);
            pos.add(rect.getX(), rect.getY());

            Point size = new Point(BORDER_SIZE, BORDER_SIZE);
            size.setProjection(rect.getSizeProjection(!isHorizontal), !isHorizontal);

            if (dir == Point.getOppositeDirection(enterDirection))
            {
                // find inner enter region
                RegionTree innerEnterRegionNode = rootRegionNode;

                for (; innerEnterRegionNode._enterSubregionNode != null; )
                    innerEnterRegionNode = innerEnterRegionNode._enterSubregionNode;

                // work out enter window
                DirectedWindow enterWindow
                        = outerEnterWindow;

                if (Point.isHorizontalDirection(dir))
                {
                    enterWindow = new DirectedWindow(
                            rect,
                            dir,
                            enterWindow.getStartPosition(),
                            outerRegion.getEnterPoint().getPosition());
                }
                else
                {
                    final double WINDOW_DISPLACEMENT = get("H_WINDOW_DISPLACEMENT");
                    final double PLAYER_WIDTH = toGrid(get("PLAYER_WIDTH"));
                    double enterPos = outerRegion.getEnterPoint().getPosition();
                    double windowStart = Math.max(0, enterPos - WINDOW_DISPLACEMENT);
                    double windowEnd = Math.min(
                            enterPos + PLAYER_WIDTH + WINDOW_DISPLACEMENT,
                            rect.getWidth());

                    enterWindow = new DirectedWindow(
                            rect,
                            dir,
                            Math.max(windowStart, enterWindow.getStartPosition()),
                            Math.min(windowEnd, enterWindow.getEndPosition()));
                }

                // add border
                addOuterBorderWithWindow(
                        outerRegion,
                        enterWindow);   // TODO: Reverse direction?
            }
            else if (dir == exitDirection)
            {
                // find inner enter region
                RegionTree innerExitRegionNode = rootRegionNode;

                for (; innerExitRegionNode._exitSubregionNode != null; )
                    innerExitRegionNode = innerExitRegionNode._exitSubregionNode;

                // work out exit window
                DirectedWindow exitWindow = outerRegion.getExitWindow().toAnotherRect(
                    innerExitRegionNode._region.getRect());

                if (Point.isHorizontalDirection(dir))
                {
                    exitWindow = new DirectedWindow(
                                    innerExitRegionNode._region.getRect(),
                                    dir,
                                    exitWindow.getStartPosition(),
                                    innerExitRegionNode._region.getExitPoint().getPosition());
                }
                else
                {
                    final double WINDOW_DISPLACEMENT = get("H_WINDOW_DISPLACEMENT");
                    final double PLAYER_WIDTH = toGrid(get("PLAYER_WIDTH"));
                    double exitPos = innerExitRegionNode._region.getExitPoint().getPosition();
                    double windowStart = Math.max(0, exitPos - WINDOW_DISPLACEMENT);
                    double windowEnd = Math.min(
                            exitPos + PLAYER_WIDTH + WINDOW_DISPLACEMENT,
                            innerExitRegionNode._region.getRect().getWidth());

                    exitWindow = new DirectedWindow(
                            innerExitRegionNode._region.getRect(),
                            dir,
                            Math.max(windowStart, exitWindow.getStartPosition()),
                            Math.min(windowEnd, exitWindow.getEndPosition()));
                }

                // add border
                addOuterBorderWithWindow(
                        outerRegion,
                        exitWindow.toAnotherRect(rect));
            }
            else
                _mapComponents.add(
                        new PlatformComponent(
                                new Rectangle(
                                        pos.getX(),
                                        pos.getY(),
                                        size.getX(),
                                        size.getY())));
        }

        // add corner borders
        Point beginPos = new Point(rect.getX(), rect.getY());
        beginPos.add(-BORDER_SIZE, -BORDER_SIZE);
        Point endPos = new Point(rect.getX(), rect.getY());
        endPos.add(rect.getWidth(), rect.getHeight());

        Point cornerSize = new Point(BORDER_SIZE, BORDER_SIZE);

        _mapComponents.add(new PlatformComponent(
                new Rectangle(
                        beginPos.getX(),
                        beginPos.getY(),
                        cornerSize.getX(),
                        cornerSize.getY())));

        _mapComponents.add(new PlatformComponent(
                new Rectangle(
                        endPos.getX(),
                        beginPos.getY(),
                        cornerSize.getX(),
                        cornerSize.getY())));

        _mapComponents.add(new PlatformComponent(
                new Rectangle(
                        beginPos.getX(),
                        endPos.getY(),
                        cornerSize.getX(),
                        cornerSize.getY())));

        _mapComponents.add(new PlatformComponent(
                new Rectangle(
                        endPos.getX(),
                        endPos.getY(),
                        cornerSize.getX(),
                        cornerSize.getY())));
    }


    private void addOuterBorderWithWindow(
            DirectedRegion region,
            DirectedWindow window) throws MapGeneratorException
    {
        assert(window.getRect() == region.getRect());

        // calculate border sizes
        final double BORDER_SIZE = get("BORDER_SIZE");
        Rectangle rect = region.getRect();
        Point.Direction dir = window.getDirection();
        boolean isHorizontal = Point.isHorizontalDirection(dir);
        double posDisplacement = Point.isPositiveDirection(dir)
                ? rect.getSizeProjection(isHorizontal)
                : -BORDER_SIZE;

        Point pos = new Point(0, 0);
        pos.setProjection(posDisplacement, isHorizontal);
        pos.add(rect.getX(), rect.getY());

        Point size = new Point(BORDER_SIZE, BORDER_SIZE);
        size.setProjection(rect.getSizeProjection(!isHorizontal), !isHorizontal);

        Point firstSize = new Point(size.getX(), size.getY());
        firstSize.setProjection(window.getStartPosition(), !isHorizontal);

        Point secondSize = new Point(size.getX(), size.getY());
        secondSize.setProjection(
                rect.getSizeProjection(!isHorizontal) - window.getEndPosition(),
                !isHorizontal);

        Point secondPos = new Point(pos.getX(), pos.getY());
        secondPos.setProjection(
                pos.getProjection(!isHorizontal) + window.getEndPosition(),
                !isHorizontal);

        // add borders
        _mapComponents.add(new PlatformComponent(
                new Rectangle(
                        toGrid(pos.getX()),
                        toGrid(pos.getY()),
                        toGrid(firstSize.getX()),
                        toGrid(firstSize.getY()))));

        _mapComponents.add(new PlatformComponent(
                new Rectangle(
                        toGrid(secondPos.getX()),
                        toGrid(secondPos.getY()),
                        toGrid(secondSize.getX()),
                        toGrid(secondSize.getY()))));
    }


    private void addBorder(
            RegionTree parentRegionNode,
            DirectedWindow traverseWindow) throws MapGeneratorException
    {
        if (traverseWindow.isOnHorizontalEdge())
            addHorizontalBorder(parentRegionNode, traverseWindow);
        else
            addVerticalBorder(parentRegionNode, traverseWindow);
    }


    private void addHorizontalBorder(
            RegionTree parentRegionNode,
            DirectedWindow traverseWindow) throws MapGeneratorException
    {
        assert(traverseWindow.isOnHorizontalEdge());

        DirectedRegion parentRegion = parentRegionNode._region;
        DirectedRegion enterRegion = parentRegionNode._enterSubregionNode._region;
        DirectedRegion exitRegion = parentRegionNode._exitSubregionNode._region;
        Rectangle enterRect = enterRegion.getRect();
        Rectangle exitRect = exitRegion.getRect();
        Rectangle rect
                = enterRect.getY() > exitRect.getY()
                ? enterRect
                : exitRect;
        Rectangle parentRect = parentRegion.getRect();

        // find most inner enter region
        RegionTree innerEnterRegionNode = parentRegionNode._enterSubregionNode;

        for ( ; innerEnterRegionNode._exitSubregionNode != null; )
            innerEnterRegionNode = innerEnterRegionNode._exitSubregionNode;

        // find most inner exit region
        RegionTree innerExitRegionNode = parentRegionNode._exitSubregionNode;

        for ( ; innerExitRegionNode._enterSubregionNode != null; )
            innerExitRegionNode = innerExitRegionNode._enterSubregionNode;

        // find inner traverse window
        final double BORDER_SIZE = get("BORDER_SIZE");
        final double displacement = get("H_WINDOW_DISPLACEMENT");
        final double xPos = innerExitRegionNode._region.getEnterPoint().toGlobalPoint(true).getX();
        final double yPos = rect.getY() - BORDER_SIZE;

        double startXPos = xPos - displacement;
        startXPos = Math.max(startXPos, innerEnterRegionNode._region.getRect().getX());
        startXPos = Math.max(startXPos, innerExitRegionNode._region.getRect().getX());
        startXPos = toGrid(startXPos);

        double endXPos = xPos + toGrid(get("PLAYER_WIDTH")) + displacement;
        endXPos = Math.min(endXPos, innerEnterRegionNode._region.getRect().getRight());
        endXPos = Math.min(endXPos, innerExitRegionNode._region.getRect().getRight());
        endXPos = toGrid(endXPos);

        // add left border
        final double minBorderSize = toGrid(get("PLAYER_WIDTH") * 1.5);
        final double leftBorderSize = startXPos - parentRect.getX();

        if (leftBorderSize >= minBorderSize)
            _mapComponents.add(
                    new PlatformComponent(
                            new Rectangle(
                                    toGrid(parentRect.getX()),
                                    toGrid(yPos),
                                    toGrid(leftBorderSize),
                                    toGrid(BORDER_SIZE))));

        // add right border
        final double rightBorderSize = parentRect.getRight() - endXPos;

        if (rightBorderSize >= minBorderSize)
            _mapComponents.add(
                    new PlatformComponent(
                            new Rectangle(
                                    toGrid(endXPos),
                                    toGrid(yPos),
                                    toGrid(rightBorderSize),
                                    toGrid(BORDER_SIZE))));
    }


    private void addVerticalBorder(
            RegionTree parentRegionNode,
            DirectedWindow traverseWindow) throws MapGeneratorException
    {
        assert(!traverseWindow.isOnHorizontalEdge());

        DirectedRegion parentRegion = parentRegionNode._region;
        DirectedRegion enterRegion = parentRegionNode._enterSubregionNode._region;
        DirectedRegion exitRegion = parentRegionNode._exitSubregionNode._region;
        Rectangle enterRect = enterRegion.getRect();
        Rectangle exitRect = exitRegion.getRect();
        Rectangle rect
                = enterRect.getX() < exitRect.getX()
                ? enterRect
                : exitRect;
        Rectangle parentRect = parentRegion.getRect();

        // find most inner enter region
        RegionTree innerEnterRegionNode = parentRegionNode._enterSubregionNode;

        for ( ; innerEnterRegionNode._exitSubregionNode != null; )
            innerEnterRegionNode = innerEnterRegionNode._exitSubregionNode;

        // find most inner exit region
        RegionTree innerExitRegionNode = parentRegionNode._exitSubregionNode;

        for ( ; innerExitRegionNode._enterSubregionNode != null; )
            innerExitRegionNode = innerExitRegionNode._enterSubregionNode;

        // find inner traverse window
        final double windowGlobalPos = Math.max(
                innerEnterRegionNode._region.getRect().getY(),
                innerExitRegionNode._region.getRect().getY());
        final double windowPos = windowGlobalPos - parentRect.getY();

        final double windowSize
                = Math.min(
                    innerEnterRegionNode._region.getExitPoint().toGlobalPoint(false).getY(),
                    innerExitRegionNode._region.getEnterPoint().toGlobalPoint(true).getY())
                - windowGlobalPos;

        final double BORDER_SIZE = get("BORDER_SIZE");

        // add top border
        final double topBorderSize
                = toGrid(windowPos);

        if (topBorderSize > 0)
            _mapComponents.add(
                    new PlatformComponent(
                            new Rectangle(
                                    toGrid(rect.getRight()),
                                    toGrid(parentRect.getY()),
                                    toGrid(BORDER_SIZE),
                                    toGrid(topBorderSize))));

        // add bottom border
        final double bottomBorderPos
                = toGrid(windowPos + windowSize);
        final double bottomBorderSize
                = toGrid(parentRect.getHeight() - bottomBorderPos);

        if (bottomBorderSize > 0)
            _mapComponents.add(
                    new PlatformComponent(
                            new Rectangle(
                                    toGrid(rect.getRight()),
                                    toGrid(parentRect.getY() + bottomBorderPos),
                                    toGrid(BORDER_SIZE),
                                    toGrid(bottomBorderSize))));
    }

    // </editor-fold>


    // <editor-fold desc="common functions">


    private double toGrid(double value) throws MapGeneratorException
    {
        return WorldProperties.getInstance().bindToGrid(value);
    }


    private double toGridLess(double value) throws MapGeneratorException
    {
        return WorldProperties.getInstance().bindToGridLess(value);
    }


    private double get(String name) throws MapGeneratorException
    {
        return WorldProperties.getInstance().get(name);
    }


    private double getMinStrategyWidth()
    {
        return _minStrategyWidth;
    }


    private double getMinStrategyHeight()
    {
        return _minStrategyHeight;
    }


    private <ValueType> void doRandomWeighedSort(
            ArrayList<ValueType> array,
            ArrayList<Double> weights)
    {
        assert(array.size() == weights.size());

        ArrayList<ValueType> copy = new ArrayList<>(array);
        ArrayList<Double> copyWeights = new ArrayList<>(weights);
        array.clear();

        double total = 0;

        for (Double weight : copyWeights)
            total += weight;

        for ( ; !copy.isEmpty(); )
        {
            double value = MapRandom.getInstance().getNextDouble() * total;
            int number = 0;

            if (value > 0)
            {
                for (double sum = 0; sum < value; ++number)
                    sum += copyWeights.get(number);

                --number;
            }

            total -= copyWeights.get(number);
            array.add(copy.get(number));
            copy.remove(number);
            copyWeights.remove(number);
        }
    }

//    private <ValueType> void doRandomSort(ArrayList<ValueType> array)
//    {
//        for (int i = array.size() - 1; i >= 0; --i)
//        {
//            int j = _random.nextInt(i + 1);
//            ValueType tmp = array.get(i);
//            array.set(i, array.get(j));
//            array.set(j, tmp);
//        }
//    }

    // </editor-fold>


    // data members
    private final ArrayList<FillStrategy> _strategyRegistry = new ArrayList<>();
    private double _minStrategyWidth = Double.MAX_VALUE;
    private double _minStrategyHeight = Double.MAX_VALUE;
    Collection<MapComponent> _mapComponents;
}
