package com.cyberslav.splitandfillgenerator.generator;

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
    }


    public void set(String name, double value)
    {
        _values.put(name, value);
    }


    public void update(double gridStep)
    {
        set("GRID_STEP", gridStep);
        set("PLAYER_WIDTH", gridStep * 2);
        set("PLAYER_HEIGHT", gridStep * 3);
        set("JUMP_HEIGHT", gridStep * 4);
        set("MIN_PLATFORM_WIDTH", get("PLAYER_WIDTH"));
        set("H_WINDOW_DISPLACEMENT", 2.0 * get("PLAYER_WIDTH"));
        set("V_WINDOW_SIZE", gridStep * 2 + get("PLAYER_HEIGHT"));
        set("TOP_PLATFORM_POS", get("PLAYER_HEIGHT"));
        set("BORDER_SIZE", gridStep * 1);
        set("GRAVITY_FACTOR", gridStep * 140);
        set("RUN_SPEED", gridStep * 25);
        set("MIN_REGION_HEIGHT_CELLS", 7);
        set("MIN_REGION_WIDTH_CELLS", 8);
        set("MIN_REGION_SQUARE", gridStep * gridStep * get("MIN_REGION_HEIGHT_CELLS") * get("MIN_REGION_WIDTH_CELLS"));
        set("CUT_RATE", 0.02);
        set("SPLIT_DEVIATION_RATE", 0.381966011);   // golden ratio or something like that. I don't remember
        set("JUMP_PAD_WIDTH", get("PLAYER_WIDTH"));
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
        update(6.0);
    }


    // data members
    private TreeMap<String, Double> _values = new TreeMap<>();
//    private final Semaphore _mutex = new Semaphore(1);

    private static WorldProperties _instance;

}
