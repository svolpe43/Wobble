/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bme.shawn.wobble;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.android.opengl.R;

public class OpenGLES20Activity extends Activity implements SensorEventListener {

    private MyGLSurfaceView mGLView;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private float vals[] = new float[3];
    private long starttime;
    private long lasttime;
    private int xstatus,ystatus,zstatus;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Configure the accelerometer to report data at the fastest rate. */
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this,
                mAccelerometer,
                SensorManager.SENSOR_DELAY_FASTEST);

        // Create a GLSurfaceView instance and set it
        // as the ContentView for this Activity
        // mGLView = new MyGLSurfaceView(this);
        setContentView(R.layout.main);
        mGLView = (MyGLSurfaceView)findViewById(R.id.glview);

        starttime = lasttime = System.currentTimeMillis();
        xstatus=1; ystatus=1; zstatus=1;
    }

    @Override
    protected void onPause() {
        super.onPause();
        // The following call pauses the rendering thread.
        // If your OpenGL application is memory intensive,
        // you should consider de-allocating objects that
        // consume significant memory here.
        mGLView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The following call resumes a paused rendering thread.
        // If you de-allocated graphic objects for onPause()
        // this is a good place to re-allocate them.
        mGLView.onResume();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent)
    {
        Sensor mySensor = sensorEvent.sensor;

        for(int j=0;j<3;j++)
        {
            vals[j] = sensorEvent.values[j];
        }
        float angle_scalar=1;
        lasttime = System.currentTimeMillis();
        if(lasttime>(starttime+100)) {
            mGLView.setAngle(vals[0] * angle_scalar, vals[1] * angle_scalar, vals[2] * angle_scalar,xstatus,ystatus,zstatus);
            starttime = lasttime;
            TextView edit_field = (TextView) findViewById(R.id.acceltext);
            String message = "x" + xstatus + ": " + sensorEvent.values[0] + ", y"+ystatus+": " + sensorEvent.values[1] + ", z"+zstatus+": " +sensorEvent.values[2];
            edit_field.setText(message);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onClickButtonX(View view)
    {
        Button button = (Button) findViewById(R.id.buttonX);
        xstatus=xstatus^1;
    }

    public void onClickButtonY(View view)
    {
        Button button = (Button) findViewById(R.id.buttonY);
        ystatus=ystatus^1;
    }

    public void onClickButtonZ(View view)
    {
        Button button = (Button) findViewById(R.id.buttonZ);
        zstatus=zstatus^1;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    //Handles action bar items clicked
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()){
            case R.id.wobble:
                startActivity(new Intent(this, WobbleActivity.class));
                break;
        }
        return true;
    }

}