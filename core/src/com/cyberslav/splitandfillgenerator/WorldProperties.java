package com.cyberslav.splitandfillgenerator;

//import java.util.concurrent.Semaphore;
import java.util.TreeMap;

/**
 * Created by cyberslav on 08.08.17.
 */
public class WorldProperties
{
    // public interface
    public static WorldProperties getInstance() throws MapGeneratorException
    {
        if (_instance == null)
            _instance = new WorldProperties();

        return _instance;
    }


    public double get(String name) throws MapGeneratorException
    {
        if (!_values.containsKey(name))
            throw new MapGeneratorException("unknown world property " + name);

        return _values.get(name);

//        try
//        {
//            _mutex.acquire();
//
//            try
//            {
//            }
//            finally
//            {
//                _mutex.release();
//            }
//        }
//        catch (InterruptedException error)
//        {
//            throw new MapGeneratorException(error);
//        }
    }


    public void set(String name, double value)
    {
        _values.put(name, value);
    }


    public double bindToGrid(double value) throws MapGeneratorException
    {
        double GRID_STEP = get("GRID_STEP");
        return Math.round(value / GRID_STEP) * GRID_STEP;
    }

    public double bindToGridLess(double value) throws MapGeneratorException
    {
        double GRID_STEP = get("GRID_STEP");
        return ((int)(value / GRID_STEP)) * GRID_STEP;
    }


    // private interface
    private WorldProperties() throws MapGeneratorException
    {
//        set("GRID_STEP", STEP);
//        set("RAW_PLAYER_WIDTH", STEP * 1.5);
//        set("RAW_PLAYER_HEIGHT", STEP * 3);
//        set("PLAYER_WIDTH", bindToGrid(get("RAW_PLAYER_WIDTH")));
//        set("PLAYER_HEIGHT", bindToGrid(get("RAW_PLAYER_HEIGHT")));
//        set("JUMP_HEIGHT", STEP * 4);
//        set("MIN_PLATFORM_WIDTH", bindToGrid(get("PLAYER_WIDTH")));
//        set("FLOOR_HEIGHT", get("GRID_STEP"));
//        set("H_WINDOW_DISPLACEMENT", get("PLAYER_WIDTH") * 2);
//        set("V_WINDOW_SIZE", bindToGrid(get("PLAYER_HEIGHT") * 1.5));
//        set("TOP_PLATFORM_POS", bindToGrid(get("JUMP_HEIGHT") * 0.9 - get("FLOOR_HEIGHT")));
//        set("BORDER_SIZE", STEP);
//        set("GRAVITY_FACTOR", STEP * 180);
//        set("RUN_SPEED", STEP * 25);
//        set("CUT_RATE", 0.2);
//        set("MIN_SPLIT_SQUARE", (STEP * STEP) * (4 * 4));
//        set("SPLIT_DEVIATION_RATE", 0.381966011);
//        set("SPAWN_REGION_HEIGHT", get("PLAYER_HEIGHT"));

//        double gridStep = 36.0;
//        double gridStep = 10.0;
        double gridStep = 6.0;

        set("GRID_STEP", gridStep);
        set("RAW_PLAYER_WIDTH", gridStep * 1.5);
        set("RAW_PLAYER_HEIGHT", gridStep * 3);
        set("PLAYER_WIDTH", gridStep * 2);
        set("PLAYER_HEIGHT", gridStep * 3);
        set("JUMP_HEIGHT", gridStep * 4);
        set("MIN_PLATFORM_WIDTH", gridStep * 2);
        set("FLOOR_HEIGHT", gridStep * 1);
        set("H_WINDOW_DISPLACEMENT", gridStep * 4);
        set("V_WINDOW_SIZE", gridStep * 5);
        set("TOP_PLATFORM_POS", gridStep * 3);
        set("BORDER_SIZE", gridStep * 1);
        set("GRAVITY_FACTOR", gridStep * 140);
        set("RUN_SPEED", gridStep * 25);
        set("MIN_SPLIT_SQUARE", gridStep * 320);
        set("SPAWN_REGION_HEIGHT", gridStep * 3);
//        set("CUT_RATE", 0.02);
//        set("SPLIT_DEVIATION_RATE", 0.1);
        set("CUT_RATE", 0.02);
        set("SPLIT_DEVIATION_RATE", 0.381966011);
        set("MIN_REGION_HEIGHT_CELLS", 7);
        set("MIN_REGION_WIDTH_CELLS", 8);
        set("JUMP_PAD_WIDTH_CELLS", 2.0);
        set("MAX_JUMP_PAD_HEIGHT", gridStep * 25.0);
        set("JUMP_PAD_USE_PROB", 1.0);
    }


    // data members
    private TreeMap<String, Double> _values = new TreeMap<>();
//    private final Semaphore _mutex = new Semaphore(1);

    private static WorldProperties _instance;

}
