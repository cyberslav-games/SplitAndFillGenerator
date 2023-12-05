package com.cyberslav.splitandfillgenerator;

/**
 * Created by cyberslav on 28.12.17.
 */
public class MapGeneratorException extends RuntimeException
{
    public MapGeneratorException(String message)
    {
        super(message);
    }


    public MapGeneratorException(Exception error)
    {
        super(error);
    }
}
