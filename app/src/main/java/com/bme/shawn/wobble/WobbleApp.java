package com.bme.shawn.wobble;

import android.app.Application;

public class WobbleApp extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();

        // Initialize the singletons so their instances
        // are bound to the application process.
        initSingletons();
    }

    protected void initSingletons()
    {
        // Initialize the instance of MySingleton
        Singleton.initInstance();
    }

    public void commitChanges()
    {
        // todo write records to the db
    }
}