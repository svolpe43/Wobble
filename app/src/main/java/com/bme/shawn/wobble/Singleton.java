package com.bme.shawn.wobble;

import java.util.ArrayList;

/**
 * Singleton to hold the records in the database
 */
public class Singleton {
    private static Singleton instance;

    public ArrayList<Record> records;

    public static void initInstance()
    {
        if (instance == null)
        {
            // Create the instance
            instance = new Singleton();
        }
    }

    public static Singleton getInstance()
    {
        // Return the instance
        return instance;
    }

    private Singleton()
    {
        // todo download the database here?!

        // Constructor hidden because this is a singleton
    }

    public void customSingletonMethod()
    {
        // Custom method
    }
}
