package com.bme.shawn.wobble;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Message;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import java.util.HashMap;

/**
 * Wobble View
 */
public class WobbleView extends SurfaceView implements SurfaceHolder.Callback{

    private String TAG = "WobbleView";

    private WobbleThread thread;

    // text views
    private TextView mAccelText;
    private TextView mBluetoothText;

    private boolean mExternalStorageState;

    private int mDataSource;

    // public constructor
    public WobbleView(Context context, AttributeSet attr) {
        super(context, attr);

        // adding the callback (this) to the surface holder to intercept events
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        // make the View focusable so it can handle events
        setFocusable(true);
    }

    // set the weight of the accel stabilizer
    public void setStability(int progress) {
        thread.setAccelWeight(progress);
    }

    // set the spike threshold
    public void setThreshold(int progress){
        thread.setThreshold(progress);
    }

    // set the size of the wobble board
    public void setScalar(int progress) {
        thread.setScalar(progress);
    }

    // set the weight of the accel stabilizer
    public void setRefractory(int progress) {
        thread.setRefractory(progress);
    }

    // set the size of the wobble board
    public void setDelta(int progress) {
        thread.setDelta(progress);
    }

    public void setDataSource(int source){
        mDataSource = source;
    }

    @Override
    // unpause
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            thread.setState(WobbleThread.STATE_RUNNING);
        }
        return true;
    }

    // installs a pointer to the text view used for messages.
    public void setTextView(HashMap<String, TextView> textViews) {
        // mSizeText = textViews.get("size_text");
        // mWeightText = textViews.get("weight_text");
        mAccelText = textViews.get("accel_text");
        mBluetoothText = textViews.get("bluetooth_text");
    }

    public void connectDevice(BluetoothDevice device){
        thread.connectDevice(device);
    }

    public void pauseThread(){
        if(thread != null)
            thread.setState(WobbleThread.STATE_PAUSE);
    }

    public void unpauseThread(){
        if(thread != null)
            thread.setState(WobbleThread.STATE_RUNNING);
    }

    // toggles the bluetooth signal and switches to accelerometer
    public void toggleBluetooth() {
        // going to internal sensors, turn 'ready to start' true
        if (mDataSource == WobbleThread.BLUETOOTH){
            mDataSource = WobbleThread.INTERNAL_SENSORS;
            thread.setThreadDataSource(WobbleThread.INTERNAL_SENSORS);
        // have to wait for bluetooth connection so don't make game ready just yet
        }else{
            mDataSource = WobbleThread.BLUETOOTH;
            thread.setThreadDataSource(WobbleThread.BLUETOOTH);
        }
    }

    // set the thread external storage state
    public void setExternalStorageState(){
        Log.i(TAG, "Setting External storage state true.");
        mExternalStorageState = true;
    }

    // start recording the accel data
    public void startRecording(String recordName){
        if(thread != null)
            thread.startRecording(recordName);
    }

    // stop recording the accel data
    public void stopRecording(){
        if(thread != null)
            thread.stopRecording();
    }


    // reset the thread - just a way to encapsulate the thread inside the view
    public void reset(){
        thread.reset();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

        Log.d(TAG, "surfaceCreated");

        // get score and game message
        thread = new WobbleThread(surfaceHolder, getContext(), new Handler(){
            @Override
            public void handleMessage(Message m){

                // set text views if ready isn't there
                //if(!m.getData().getBoolean("ready"))

                mBluetoothText.setText(m.getData().getString("bluetooth_text"));
                mAccelText.setText(m.getData().getString("accel_text"));
            }
        });

        // set the data source of the thread
        thread.setThreadDataSource(mDataSource);

        // start the bluetooth service
        thread.startBluetoothService();

        // initialize external storage if were g2g
        if(mExternalStorageState)
            thread.initExternalStorage();

        // start the thread
        thread.startThread();
    }

    // surfaceChanged is called at least once after surfaceCreated
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        Log.d(TAG, "surfaceChanged");

        // reset the surface size
        thread.setSurfaceSize(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

        Log.d(TAG, "surfaceDestroyed");

        // make sure to shut down the thread cleanly
        boolean retry = true;

        // stop the running thread
        thread.setRunning(false);

        // continuously try to shut down the thread
        while (retry){
                try{
                    // blocks calling thread until termination
                    thread.join();

                    // stop the bluetooth service
                    thread.stopBluetoothService();

                    //close the recording writer if open
                    thread.closeWriters();

                    retry = false;
                }catch(InterruptedException e){
                    //try to shut it down again
                }
        }
    }
}
