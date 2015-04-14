package com.bme.shawn.wobble;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Toast;

import com.example.android.opengl.R;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * UI thread to display current position of board and feedback.
 */
public class WobbleThread extends Thread implements SensorEventListener{

    // Debugging
    private static final String TAG = "WobbleThread";

    // State-tracking constants
    public static final int STATE_PAUSE = 2;
    public static final int STATE_RUNNING = 4;

    // background image
    private Bitmap BackgroundImage;

    // accel paint
    private Paint mAccelPaint;
    private Paint mCirclePaint;
    private Paint mAccelCirclePaint;

    // circle graphics
    private static final float X_PADDING = 30f;
    private float mBoardRadius;

    // position of the dot on screen
    private float mX;
    private float mY;
    private float mAccelRadius;
    private float[] mAccelIn;
    private float[] mAccelOut;
    private float mStability = 0.15f;
    private int hitFade = 0;

    // external storage file
    private FileWriter mRecordingWriter;
    private FileWriter mRawDataWriter;
    private boolean mRecording;
    private static final String RAW_DATA_FILE_NAME = "raw_data.txt";
    private static final String RECORDINGS_FILE_NAME = "recordings.txt";
    private StringBuilder mRawDataBuf;
    private StringBuilder mRecordingBuf;
    private StringBuilder mSpikePointsBuf;
    private StringBuilder mPointsBuf;

    // capturing points delays
    private long mLastSeqTime;
    private long mLastSpike;

    // set by seek bars in settings
    private long mDelta = 300; //ms
    private long mRefractory = 100; //ms
    private int mScalar = 40; //m/s^2
    private float mThreshold = 100; //m/s^2


    // indicate whether the surface has been created & is ready to draw
    private int mMode;
    private boolean mRun = false;
    private final Object mRunLock = new Object();

    // Surface holder that can access the physical surface
    private final SurfaceHolder mHolder;
    private Context mContext;

    // canvas dimensions
    private int mXCenter = 1;
    private int mYCenter = 1;
    private static final int OFFSET = 0;

    // Message handler used by thread to interact with TextView
    private Handler mHandler;

    /* Bluetooth Globals
     *********************************************************************/
    // Message types sent from the BluetoothService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // storage for the data source
    private int mDataSource;

    // data source constants
    public static final int BLUETOOTH = 0;
    public static final int INTERNAL_SENSORS = 1;

    // Name of the connected device
    private String mConnectedDeviceName = null;

    // Bluetooth Service
    private BluetoothService mBluetoothService;

    // Bluetooth status
    private String mDataStatusText;

    public WobbleThread(SurfaceHolder surfaceHolder, Context context, Handler handler) {

        this.mHolder = surfaceHolder;
        this.mHandler = handler;
        this.mContext = context;

        // initialize accel in and out holders
        mAccelIn = new float[10];
        mAccelOut = new float[10];

        for(int i = 0; i < 10; i++){
            mAccelIn[i] = 0;
            mAccelOut[i] = 0;
        }

        //Going to want a black background
        Resources res = context.getResources();
        //BackgroundImage = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        BackgroundImage = BitmapFactory.decodeResource(res,R.drawable.black);

        Log.d("thread", "initializing");

        // accel position paint
        mAccelPaint = new Paint();
        mAccelPaint.setAntiAlias(true);
        mAccelPaint.setARGB(255, 255, 255, 255);

        mAccelCirclePaint = new Paint();
        mAccelCirclePaint.setAntiAlias(true);
        mAccelCirclePaint.setStyle(Paint.Style.STROKE);
        mAccelCirclePaint.setStrokeWidth(10);
        mAccelCirclePaint.setARGB(255, 255, 255, 255);

        // accel position paint
        mCirclePaint = new Paint();
        mCirclePaint.setAntiAlias(true);
        mCirclePaint.setStyle(Paint.Style.STROKE);
        mCirclePaint.setStrokeWidth(10);
        mCirclePaint.setARGB(255, 255, 255, 255);
    }

    // almost like a constructor
    public void startThread(){

        Log.d(TAG, "starting thread");

        this.start();

        setRunning(true);

        setState(STATE_RUNNING);

        // use this(?) - synchronized (mHolder){}
    }

    // sets the data source to BT or internal sensors
    public void setThreadDataSource(int source){

        // set the new source
        mDataSource = source;

        // initialize recording status
        mRecording = false;

        // turn on the accelerometer listeners at the fastest rate
        if(source == INTERNAL_SENSORS){
             /* Configure the accelerometer to report data at the fastest rate. */
            SensorManager mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
            Sensor mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mSensorManager.registerListener(this,mAccelerometer,SensorManager.SENSOR_DELAY_FASTEST);
            mDataStatusText = "Internal accelerometers";
        }else{
            // Initialize the BluetoothService to use bluetooth connections
            if(mBluetoothService == null) {
                // todo turn off sensor listener - but now im just checking the datasource in the onSensorChange
                mBluetoothService = new BluetoothService(mContext, mBluetoothHandler);
            }
        }
    }

    // set external storage state
    public void initExternalStorage() {
        // what is the point of StringBuffer over String
        mRawDataBuf = new StringBuilder();
        mRecordingBuf = new StringBuilder();

        // buffers to collect points in
        mSpikePointsBuf = new StringBuilder();
        mPointsBuf = new StringBuilder();
    }

    // start recording the accel data and create the txt files for output
    public void startRecording(String recordName){

        Log.i(TAG, "Starting recording...");

        // make sure that we can record data to external storage
        if(mRecording){
            Log.e(TAG, "Already recording.");
        }else{
            // add all the parameters
            mRecordingBuf.append(recordName).append(";");
            mRecordingBuf.append(mThreshold).append(";");
            mRecordingBuf.append(mRefractory).append(";");
            mRecordingBuf.append(mDelta).append(";");
            mRecordingBuf.append(mScalar).append(";");
            mRecordingBuf.append(mStability).append(";");
            mRecordingBuf.append(String.valueOf(System.currentTimeMillis()));
            mRecording = true;

            // start the interval timers
            mLastSeqTime = System.currentTimeMillis();
            mLastSpike = System.currentTimeMillis();

            Log.i(TAG, "Started recording.");
        }
    }

    // stop recording and save the data
    public void stopRecording(){

        Log.i(TAG, "Stopping recording...");

        if(mRecording){
            mRecording = false;

            // timestamp string
            String timestamp = getTimeStamp();

            // try opening and creating the files
            try {
                String filepath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/wobble/";
                File mainDir = new File(filepath);
                if(!mainDir.isDirectory()){
                    if(!mainDir.mkdir()){
                        Log.e(TAG, "Couldn't create the main Wobble directory.");
                    }
                }

                File rawDataFile = new File(filepath, timestamp + RAW_DATA_FILE_NAME);
                if (!rawDataFile.createNewFile()) {
                    Log.e(TAG, "Raw Data file not created");
                } else {
                    mRawDataWriter = new FileWriter(rawDataFile);
                }

                // only get file and initiate the writer if it hasn't already been done
                File recordingFile = new File(filepath, RECORDINGS_FILE_NAME);
                if (!recordingFile.canWrite()) {
                    if (!recordingFile.createNewFile())
                        Log.e(TAG, "Couldn't create the Recordings file.");
                    Log.e(TAG, "Recordings file not writtable.");
                }
                mRecordingWriter = new FileWriter(recordingFile, true);

                // try writing the data to the files and close the writers
                try{
                    // write the strings
                    mRawDataWriter.write(mRawDataBuf.toString());

                    // add the end UTC time to the file and a new line
                    mRecordingBuf.append(";").append(mPointsBuf.toString());
                    mRecordingBuf.append(";").append(mSpikePointsBuf.toString());
                    mRecordingBuf.append(";").append(String.valueOf(System.currentTimeMillis()));
                    mRecordingBuf.append("\n");
                    mRecordingWriter.write(mRecordingBuf.toString());

                    // close the writer - only close raw data because we will re use the recording writer
                    mRawDataWriter.close();
                    mRecordingWriter.close();

                    // reset the buffer strings
                    mRawDataBuf.setLength(0);
                    mRecordingBuf.setLength(0);
                    mSpikePointsBuf.setLength(0);
                    mPointsBuf.setLength(0);

                }catch(IOException e){
                    Log.i(TAG, "Could not write to file " + e);
                }
            }catch(IOException e){
                Log.e(TAG, "Unable to open file writer " + e);
            }
        }else{
            Log.i(TAG, "Already stopped.");
        }
    }

    // close the writers if they are open
    public void closeWriters(){
        try{

            Log.i(TAG, "Closing writers...");

            if(mRecordingWriter != null)
                mRecordingWriter.close();

            if(mRawDataWriter != null)
                mRawDataWriter.close();
        }catch(IOException e){
            Log.i(TAG, "Could not close file writers");
        }
    }

    // get the current timestamp
    public static String getTimeStamp() {
        Time now = new Time();
        now.setToNow();
        return now.format("%Y_%m_%d_%H_%M_%S");
    }

    // reset
    public void reset(){
        setState(STATE_PAUSE);
    }

    @Override
    public void run() {
        while (mRun) {
            Canvas c = null;
            try {
                c = mHolder.lockCanvas(null);
                synchronized (mHolder) {

                    // updates game and returns if the game is exiting
                    // if(mMode == STATE_RUNNING)

                    // Critical section. Do not allow run to be set false until
                    // we are sure all canvas drawings operations are complete.
                    // If mRun has been toggled false, inhibit canvas operations.
                    synchronized (mRunLock) {
                        if (mRun)
                            draw(c);
                    }
                }
            } finally {
                // do this in a finally so that if an exception is thrown
                // during the above, we don't leave the Surface in an
                // inconsistent state
                if (c != null) {
                    mHolder.unlockCanvasAndPost(c);
                }
            }
        }
    }

    public void setState(int mode){

        synchronized (mHolder) {
            mMode = mode;

            if (mMode == STATE_PAUSE) {

                Message msg = mHandler.obtainMessage();
                Bundle b = new Bundle();
                b.putString("text", "Paused");
                b.putInt("viz", View.INVISIBLE);
                msg.setData(b);
                mHandler.sendMessage(msg);
            }
        }

    }

    /**
     * Used to signal the thread whether it should be running or not.
     * Passing true allows the thread to run; passing false will shut it
     * down if it's already running. Calling start() after this was most
     * recently called with false will result in an immediate shutdown.
     *
     * @param b true to run, false to shut down
     */
    public void setRunning(boolean b) {
        // Do not allow mRun to be modified while any canvas operations
        // are potentially in-flight. See draw().
        synchronized (mRunLock) {
            mRun = b;
        }
    }

    // set the mStability
    public void setAccelWeight(int percent){
        if(percent == 0)
            mStability = .01f;
        else
            mStability = 1 * (float) percent/100;
    }

    // set the scalar size
    public void setScalar(int progress){
        mScalar = progress;
    }

    // set refractory period after a spike
    public void setRefractory(int progress){
        mRefractory = progress;
    }

    // set delta time in between samples
    public void setDelta(int progress){
        mDelta = progress;
    }

    // set the spike threshold
    public void setThreshold(int progress){
        mThreshold = progress;
    }

    // takes in raw accel data and converts to use-able values
    public void updateAccelUI(float x, float y, float z){
        //System.out.println("Accel data X: " + x_int + "Y: " + y_int + "Z: " + z_int);

        mAccelIn[0] = x;
        mAccelIn[1] = y;
        mAccelIn[2] = z;

        // process each data point
        for(int i = 0; i < 3; i++ ){

            // stabilizing filter - output = old + difference of old and new * mStability
            mAccelOut[i] = mAccelOut[i] + mStability * (mAccelIn[i] - mAccelOut[i]);

            // check if spike is detected
            if(mAccelIn[i] - mAccelOut[i] > mThreshold && mLastSpike + mRefractory< System.currentTimeMillis()){
                mAccelCirclePaint.setStrokeWidth(20);
                hitFade = 255;

                // if first point then write the comma
                if(mSpikePointsBuf.length() != 0)
                    mSpikePointsBuf.append(",");

                mSpikePointsBuf
                        .append(mXCenter + mAccelOut[0])
                        .append(":")
                        .append(mYCenter + mAccelOut[1] - OFFSET);      // subtract offset to get true location

                mLastSpike = System.currentTimeMillis();
            }
        }

        // adjust color on spike detected
        if (hitFade < 1) {
            mAccelCirclePaint.setARGB(255, 255, 255, 255);
            mAccelCirclePaint.setStrokeWidth(10);
            hitFade = 0;
        }else{
            hitFade -= 3;
            mAccelCirclePaint.setARGB(255, 200, 255 - hitFade, 255 - hitFade);
        }

        // adjust accel radius and detect floor touch
        mAccelRadius = (float) Math.sqrt(Math.abs(mAccelOut[0])* Math.abs(mAccelOut[0]) + Math.abs(mAccelOut[1]) * Math.abs(mAccelOut[1]));
        if(mAccelRadius > mBoardRadius){
            mAccelCirclePaint.setARGB(255, 0, 153, 51);
            mAccelCirclePaint.setStrokeWidth(20);
        }

        // save accelerometer position
        mX = mXCenter + mAccelOut[0];
        mY = mYCenter + mAccelOut[1];

        // if recording write the points to the buffer
        if(mRecording && mLastSeqTime + mDelta < System.currentTimeMillis()) {
            if(mPointsBuf.length() != 0)
                mPointsBuf.append(",");

            mPointsBuf
                    .append(mX)
                    .append(":")
                    .append(mY - OFFSET);   // subtract offset to get true point location

            // save the last recorded time
            mLastSeqTime = System.currentTimeMillis();
        }
    }

    @Override
    // change the data to screen position and send it to UI method
    public void onSensorChanged(SensorEvent sensorEvent)
    {
        if(mDataSource == INTERNAL_SENSORS){
            float xAccel = - sensorEvent.values[0] * mScalar;
            float yAccel = sensorEvent.values[1] * mScalar;
            float zAccel = sensorEvent.values[2] * mScalar;

            // changes vars for UI drawing
            updateAccelUI(xAccel, yAccel, zAccel);

            // record the raw sensor data into buffer
            if(mRecording){
                mRawDataBuf.append("x:")
                        .append(String.valueOf(sensorEvent.values[0]))
                        .append("y:")
                        .append(String.valueOf(sensorEvent.values[1]))
                        .append("z:")
                        .append(String.valueOf(sensorEvent.values[2]))
                        .append("\n");
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    /**
     * Draws the position of the board
     */
    public void draw(Canvas canvas) {

        if(canvas == null)
            return;

        // remove this to save location of the ball
        // clear the screen - accumulating canvas drawing
        canvas.drawBitmap(BackgroundImage, 0, 0, null);

        // draw the accelerometer position - the dot
        canvas.drawCircle(mX, mY, 30, mAccelPaint);

        // draw the accel circle
        RectF mAccelOval = new RectF(mXCenter - mAccelRadius, mYCenter - mAccelRadius, mXCenter + mAccelRadius, mYCenter + mAccelRadius);
        canvas.drawOval(mAccelOval, mAccelCirclePaint);

        // draw the outer circle
        if(mRecording){
            mCirclePaint.setARGB(255, 0, 102, 255);
        }else{
            mCirclePaint.setARGB(255, 255, 255, 255);
        }
        RectF outterOval = new RectF(mXCenter - mBoardRadius, mYCenter - mBoardRadius, mXCenter + mBoardRadius, mYCenter + mBoardRadius);
        canvas.drawOval(outterOval, mCirclePaint);

        canvas.save();
        canvas.restore();

        // show the accel locator position
        Message msg = mHandler.obtainMessage();
        Bundle b = new Bundle();

        // add the accel data
        b.putString("accel_text", "X: " + (int)mAccelIn[0] + " Y: " + (int)mAccelIn[1] + " Z: " + (int)mAccelIn[2] );

        // add the bluetooth status text
        b.putString("bluetooth_text", mDataStatusText);

        // send the message bundle
        msg.setData(b);
        mHandler.sendMessage(msg);
    }

    // starts the Bluetooth Service
    public void startBluetoothService(){
        if (mBluetoothService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mBluetoothService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth chat services
                mBluetoothService.start();
            }
        }
    }

    // stops the Bluetooth Service
    public void stopBluetoothService(){
        if (mBluetoothService != null)
            mBluetoothService.stop();
    }

    // The Handler that gets information back from the BluetoothService
    private final Handler mBluetoothHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:

                            mDataStatusText = "Connected to " +  mConnectedDeviceName;

                            // send ready to start flag
                            Message readymsg = mHandler.obtainMessage();
                            Bundle b = new Bundle();
                            b.putString("bluetooth_text", mDataStatusText);
                            b.putBoolean("ready", true);
                            readymsg.setData(b);
                            mHandler.sendMessage(readymsg);

                            break;
                        case BluetoothService.STATE_CONNECTING:
                            mDataStatusText = "Connecting...";
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            mDataStatusText = "Connect";
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    //byte[] writeBuf = (byte[]) msg.obj;

                    // do something with the writeBuf

                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;

                    ByteBuffer buf = ByteBuffer.wrap(readBuf);

                    // receiving
                    float x = buf.getFloat();
                    float y = buf.getFloat();
                    float z = buf.getFloat();

                    if(mDataSource == BLUETOOTH) {
                        updateAccelUI(x, y, 0);
                    }

                    Log.d(TAG, "X: " + Float.toString(x));
                    Log.d(TAG, "Y: " + Float.toString(y));
                    Log.d(TAG, "Z: " + Float.toString(z));
                    //updateAccelUI(x_val, y_val, z_val);

                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(mContext, "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(mContext, msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    // connects to a device
    public void connectDevice(BluetoothDevice device) {

        // Attempt to connect to the device
        mBluetoothService.connect(device, false);
    }

    /**
     * Restores game state from the indicated Bundle. Typically called when
     * the Activity is being restored after having been previously
     * destroyed.
     *
     * savedState - Bundle containing the game state

    public synchronized void restoreState(Bundle savedState) {
        synchronized (mHolder) {
            // pause the game
            // get all vars from the savedState;
        }
    }

    /**
     * Dump game state to the provided Bundle. Typically called when the
     * Activity is being suspended.
     *
     * return - Bundle with this view's state

    public Bundle saveState(Bundle map) {
        synchronized (mHolder) {
            if (map != null) {
                // save all the vars that your going to need to restore the game
                // use - map.putInt(KEY_DIFFICULTY, Integer.valueOf(mDifficulty));
            }
        }
        return map;
    }*/

    /* Callback invoked when the surface dimensions change. */
    public void setSurfaceSize(int width, int height) {
        // synchronized to make sure these all change atomically
        synchronized (mHolder) {

            mAccelRadius = 50;

            Log.d(TAG, "setting surface sizes");

            mXCenter = width/2;
            mYCenter = height/2 + OFFSET;  // off set for the settings

            mX = mXCenter;
            mY = mYCenter- mAccelRadius;

            // don't forget to resize the background image
            BackgroundImage = Bitmap.createScaledBitmap(BackgroundImage, width, height, true);

            mBoardRadius = mXCenter - X_PADDING;
        }
    }
}
