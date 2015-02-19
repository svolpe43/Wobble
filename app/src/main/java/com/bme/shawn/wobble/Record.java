package com.bme.shawn.wobble;

import java.text.DateFormat;
import java.util.ArrayList;

/**
 * Record
 */
public class Record{

    // segments of the downloaded string
    public static final int NAME = 0;
    public static final int THRESHOLD = 1;
    public static final int REFRACTORY = 2;
    public static final int DELTA = 3;
    public static final int SCALAR = 4;
    public static final int STABILITY = 5;
    public static final int START_TIME = 6;
    public static final int POINTS = 7;
    public static final int SPIKE_POINTS = 8;
    public static final int STOP_TIME = 9;

    public static final int numItems = 10;

    public String time;
    public String duration;
    public String name;
    public String stability;
    public String threshold;
    public String refractory;
    public String delta;
    public String scalar;
    public ArrayList<Point> points;
    public ArrayList<Point> spikePoints;

    private String TAG = "Record class";

    // holds the full recording and meta data
    public Record(String[] string){

        // grab the special parameters
        name = string[NAME];
        threshold = string[THRESHOLD];
        refractory = string[REFRACTORY];
        delta = string[DELTA];
        scalar = string[SCALAR];
        stability = string[STABILITY];

        // grab the start time
        long startTime = Long.parseLong(string[START_TIME]);

        DateFormat df = DateFormat.getDateTimeInstance();
        time = df.format(startTime);

        // calculate duration
        long stopTime = Long.parseLong(string[STOP_TIME]);
        duration = String.valueOf((stopTime - startTime) / 1000);

        // gather all of the recorded poitns - make sure to skip the time
        points = new ArrayList<Point>();
        String[] pointsSt = string[POINTS].split("[,]+");
        for(int i = 0; i < pointsSt.length; i++){
            // create the new point object - split the segment into a x an y value
            Point point = new Point(pointsSt[i].split("[:]+"));

            // add it to the array list
            points.add(point);
        }

        // gather all spike points
        spikePoints = new ArrayList<Point>();
        String[] spikePointsSt = string[SPIKE_POINTS].split("[,]+");
        for(int i = 0; i < spikePointsSt.length; i++){
            // create the new point object - split the segment into a x an y value
            Point point = new Point(spikePointsSt[i].split("[:]+"));

            // add it to the array list
            spikePoints.add(point);
        }
    }

    // used to hold the x and y float points for each step
    public class Point {
        public float x;
        public float y;

        // should be able to parse a string array
        public Point(String[] pointString){
            x = Float.parseFloat(pointString[0]);
            y = Float.parseFloat(pointString[1]);
        }
    }
}
