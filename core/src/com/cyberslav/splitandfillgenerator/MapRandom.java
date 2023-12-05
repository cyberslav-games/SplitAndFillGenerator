package com.cyberslav.splitandfillgenerator;

import java.util.List;
import java.util.Random;

/**
 * Created by cyberslav on 28.12.17.
 */
public class MapRandom
{
    // public

    public int getNextInt()
    {
        return _random.nextInt();
    }


    public int getNextInt(int end)
    {
        return _random.nextInt(end);
    }


    public int getNextInt(int start, int end)
    {
        return start + _random.nextInt(end - start);
    }


    public double getNextDouble()
    {
        return _random.nextDouble();
    }


    public double getNextDouble(double start, double end)
    {
        return start + (end - start) * _random.nextDouble();
    }


    public double getNextNormal(double mean, double deviation)
    {
        return _random.nextGaussian() * deviation + mean;
    }


    public <ValueType> void doRandomSort(List<ValueType> array)
    {
        for (int i = array.size() - 1; i >= 0; --i)
        {
            int j = _random.nextInt(i + 1);
            ValueType tmp = array.get(i);
            array.set(i, array.get(j));
            array.set(j, tmp);
        }
    }


    public static MapRandom getInstance()
    {
        if (_instance == null)
            _instance = new MapRandom();

        return _instance;
    }


    // protected
    protected MapRandom()
    {
    }


    // private

    // data
    private final Random _random = new Random();
    private static MapRandom _instance;
}
