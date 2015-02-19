package com.bme.shawn.wobble;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.gesture.GestureOverlayView;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.opengl.R;

import java.util.HashMap;

public class WobbleActivity extends Activity implements View.OnClickListener {

    private String TAG = "WobbleActivity";

    private WobbleView wobbleView;
    private TextView mStabilityView;
    private TextView mScalarView;
    private TextView mRefractoryView;
    private TextView mDeltaView;
    private TextView mThresholdView;

    private EditText mRecordNameView;

    private Menu mMenu;

    private boolean mSizeLock;

    // bluetooth adapter that collects data from devices bluetooth
    private BluetoothAdapter mBluetoothAdapter;

    // request bluetooth code
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 2;

    private static final int SWIPE_MIN_DISTANCE = 50;
    private static final int SWIPE_THRESHOLD_VELOCITY = 100;
    private GestureDetector gestureDetector;
    View.OnTouchListener gestureListener;
    private View mSettingsView;

    @Override
    public void onClick(View view) {

    }

    class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                // right to left swipe
                if(e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    Log.d(TAG, "swipe up");
                    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, 0);
                    mSettingsView.setLayoutParams(p);
                    mSettingsView.requestLayout();
                }  else if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    Log.d(TAG, "swipe down");
                    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    mSettingsView.setLayoutParams(p);
                    mSettingsView.requestLayout();
                }
            } catch (Exception e) {
                // nothing
            }
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wobble);

        // stop the screen from turning off
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        gestureDetector = new GestureDetector(this, new MyGestureDetector());
        gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        };
        mSettingsView = findViewById(R.id.settings);

        // get view and thread
        wobbleView = (WobbleView) findViewById(R.id.wobble);
        wobbleView.setOnClickListener(WobbleActivity.this);
        wobbleView.setOnTouchListener(gestureListener);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {

            // alert the user of bluetooth failure
            Toast.makeText(this, "Bluetooth is not available, using internal devices sensors", Toast.LENGTH_LONG).show();

            // set the data source to internal sensors - so we'll just use the devices accel
            // wobbleThread.setDataSource(WobbleThread.INTERNAL_SENSORS);
            wobbleView.setDataSource(WobbleThread.INTERNAL_SENSORS);

        // bluetooth is supported so make sure its enabled and
        }else{

            // make sure bluetooth is enabled on the device
            if (!mBluetoothAdapter.isEnabled()) {
                Log.d(TAG, "starting request to enable bluetooth");
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            }

            // all is well with bluetooth - use bluetooth
            Log.d(TAG, "setting bluetooth to bluetooth");
            wobbleView.setDataSource(WobbleThread.BLUETOOTH);
            //wobbleThread.setDataSource(WobbleThread.BLUETOOTH);
        }

        // get the seek bar texts
        mStabilityView = (TextView) findViewById(R.id.stability_text);
        mScalarView = (TextView) findViewById(R.id.scalar_text);
        mRefractoryView = (TextView) findViewById(R.id.refractory_text);
        mDeltaView = (TextView) findViewById(R.id.delta_text);
        mThresholdView = (TextView) findViewById(R.id.threshold_text);

        // get and give the text views to the view
        HashMap<String, TextView> textViews = new HashMap<String, TextView>();
        textViews.put("weight_text", mStabilityView);
        textViews.put("size_text", mScalarView);
        textViews.put("accel_text", (TextView) findViewById(R.id.accel_text));
        textViews.put("bluetooth_text", (TextView) findViewById(R.id.bluetooth_text));
        wobbleView.setTextView(textViews);

        // get the edit text for the recording name
        mRecordNameView = (EditText) findViewById(R.id.record_name);

        // init the seek bars
        setSeekBars();

        // init the external storage
        setExternalStorage();

        if (savedInstanceState == null) {} else {}
    }

    // checks mount status and gives it to the view
    private void setExternalStorage(){
        // make sure we can read and write to external storage
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)){
            wobbleView.setExternalStorageState();
            Log.i(TAG, "Media Mounted.");// + String.valueOf(Environment.MEDIA_MOUNTED.equals(state)));
        }else{
            Log.e(TAG, "Media state false");
        }
    }

    // initialize the settings seek bars
    // TODO put this in settings on swipe out
    private void setSeekBars(){

        // configure spike threshold seek bar
        SeekBar thresholdSeekBar = (SeekBar)findViewById(R.id.thresholdSeekBar);
        thresholdSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                wobbleView.setThreshold(progress);
                mThresholdView.setText("Threshold: " + Integer.toString(progress));
            }
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // configure refractory seek bar
        SeekBar refractorySeekBar = (SeekBar)findViewById(R.id.refractorySeekBar);
        refractorySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                wobbleView.setRefractory(progress);
                mRefractoryView.setText("Refractory: " + Integer.toString(progress));
            }
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });


        // configure delta seek bar
        SeekBar deltaSeekBar = (SeekBar)findViewById(R.id.deltaSeekBar);
        deltaSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                wobbleView.setDelta(progress);
                mDeltaView.setText("Delta: " + Integer.toString(progress));
            }
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // configure weight seek bar
        SeekBar stabilityBar = (SeekBar)findViewById(R.id.stabilitySeekBar);
        stabilityBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                wobbleView.setStability(progress);
                mStabilityView.setText("Stability: " + Integer.toString(progress));
            }
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // configure board size see bar
        SeekBar SizeBar = (SeekBar)findViewById(R.id.sizeSeekBar);
        SizeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean formUser){
                if(!mSizeLock){
                    wobbleView.setScalar(progress);
                    mScalarView.setText("Scalar: " + Integer.toString(progress));
                }
            }
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    // if device not discoverable then ask the user to change it
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    // handle the result of the bluetooth connect and search activities
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {

                    // Get the device MAC address
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

                    // Get the BluetoothDevice object
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

                    // connects the device in the Bluetooth Service
                    wobbleView.connectDevice(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    //wobbleThread.setDataSource(WobbleThread.BLUETOOTH);
                    wobbleView.setDataSource(WobbleThread.BLUETOOTH);
                } else {
                    // user did not enable Bluetooth or an error occurred
                    //wobbleThread.setDataSource(WobbleThread.INTERNAL_SENSORS);
                    wobbleView.setDataSource(WobbleThread.INTERNAL_SENSORS);
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                }
        }
    }

    // start recording from start button
    public void startRecording(View v){
        String recordName = mRecordNameView.getText().toString();
        if(recordName.length() > 0) {
            Log.i(TAG, "Start button pressed.");
            wobbleView.startRecording(recordName);
        }else{
            Toast.makeText(this, "You must enter a record name to start recording.", Toast.LENGTH_SHORT).show();
        }
    }
    // stop recording from stop button
    public void stopRecording(View v){
        Log.i(TAG, "Stop button pressed.");
        wobbleView.stopRecording();
    }

    @Override
    // invoked when the activity is partially visible
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        wobbleView.pauseThread();
    }

    @Override
    protected void onResume(){
        super.onResume();
        Log.d(TAG, "onResume");
        wobbleView.unpauseThread();
    }

    @Override
    protected void onDestroy(){
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    /**
     * Notification that something is about to happen, to give the Activity a
     * chance to save state.
     *
     * @param outState a Bundle into which this Activity should save its state
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // just have the View's thread save its state into our Bundle
        super.onSaveInstanceState(outState);
        //wobbleThread.saveState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        mMenu = menu;
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.wobble, menu);
        return true;
    }

    //Handles action bar items clicked
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Intent serverIntent = null;
        // get size lock menu item
        MenuItem sizeLockItem = mMenu.findItem(R.id.sizeLock);

        switch (item.getItemId()){
            case R.id.restart:
                Intent intent = getIntent();
                finish();
                startActivity(intent);
                break;
            case R.id.bluetooth_toggle:
                wobbleView.toggleBluetooth();
                return true;
            case R.id.connect_scan:
                // Launch the DeviceListActivity to see devices and do scan
                serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            case R.id.discoverable:
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            case R.id.sizeLock:
                if (mSizeLock){
                    mSizeLock = false;
                    sizeLockItem.setTitle("Lock");
                }else{
                    mSizeLock = true;
                    sizeLockItem.setTitle("Unlock");
                    mScalarView.setText("Locked");
                }
                return true;
            case R.id.openGL:
                startActivity(new Intent(this, OpenGLES20Activity.class));
                return true;
            case R.id.recordings:
                startActivity(new Intent(this, RecordingsActivity.class));
                return true;
        }
        return false;
    }
}