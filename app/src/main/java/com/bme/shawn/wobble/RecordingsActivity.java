package com.bme.shawn.wobble;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.android.opengl.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class RecordingsActivity extends ListActivity {

    // name of recordings file
    private static final String RECORDINGS_FILE_NAME = "recordings.txt";

    // class tag
    private static final String TAG = "Recordings activity";

    // store the recordings
    ArrayList<Record> records;

    ArrayList<String> stringRecords;

    // custom list view adapter to old recording objects
    public class RecordingsAdapter extends ArrayAdapter<Record> {
        public RecordingsAdapter(Context context, ArrayList<Record> Recordings) {
            super(context, 0, Recordings);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            Record Recording = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.recording_view, parent, false);
            }

            // get text views
            TextView name = (TextView) convertView.findViewById(R.id.name);
            TextView time = (TextView) convertView.findViewById(R.id.time);
            TextView duration = (TextView) convertView.findViewById(R.id.duration);

            /*TextView refractory = (TextView) convertView.findViewById(R.id.refractory);
            TextView threshold = (TextView) convertView.findViewById(R.id.threshold);
            TextView scalar = (TextView) convertView.findViewById(R.id.scalar);
            TextView delta = (TextView) convertView.findViewById(R.id.delta);
            TextView stability = (TextView) convertView.findViewById(R.id.stability);*/


            // set text views
            name.setText(Recording.name);
            time.setText(Recording.time);
            duration.setText(Recording.duration + " secs");

            /*threshold.setText("Threshold: " + Recording.threshold);
            scalar.setText("Scalar: " + Recording.scalar);
            refractory.setText("Refractory: " + Recording.refractory);
            delta.setText("Delta: " + Recording.delta);
            stability.setText("Stability: " + Recording.stability);*/

            // Return the completed view to render on screen
            return convertView;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String filepath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/wobble/";

        // try to download the file and init UI
        try{
            File recordingFile = new File(filepath, RECORDINGS_FILE_NAME);
            if (!recordingFile.canWrite()) {
                Log.e(TAG, "Recordings file not writtable.");
            } else {

                // download the contents of the file
                records = downloadRecordings(new BufferedReader(new FileReader(recordingFile)));

                // create custom list view adapter and apply it
                RecordingsAdapter recordingAdapter = new RecordingsAdapter(this, records);
                setListAdapter(recordingAdapter);
                getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            }
        }catch(IOException e){
            Log.e(TAG, String.valueOf(e));
        }
    }

    // downloads the entire file specifed by the Buffer Reader
    // reads data in the form of - <timestamp>;<points>;<spike points>;stoptime\n
    private ArrayList<Record> downloadRecordings(BufferedReader reader) {

        // init
        ArrayList<Record> recordings = new ArrayList<Record>();
        stringRecords = new ArrayList<String>();
        String line;

        try{
            // read until the end of file
            while((line = reader.readLine()) != null){

                // comma delimited
                String[] parts = line.split("[;]+");

                // create and add recording
                if(parts.length == Record.numItems) {
                    Record record = new Record(parts);
                    recordings.add(record);

                    // save string record
                    stringRecords.add(line);
                }
            }

            // close BufferReader
            reader.close();
        }catch(IOException e){
            Log.e(TAG, String.valueOf(e));
        }
        return recordings;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        // give the string to the new activity
        Bundle bundle = new Bundle();
        bundle.putString("record", stringRecords.get(position));
        Intent intent = new Intent(this, RecordDisplayActivity.class).putExtras(bundle);
        startActivity(intent);
    }


    @Override
    public boolean onCreateOptionsMenu (Menu menu){
        getMenuInflater().inflate(R.menu.recordings, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.wobble:
                startActivity(new Intent(this, WobbleActivity.class));
                return true;
            case R.id.reload:
                Intent intent = getIntent();
                finish();
                startActivity(intent);
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

