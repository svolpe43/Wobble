package com.bme.shawn.wobble;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.example.android.opengl.R;

import org.w3c.dom.Text;


public class RecordDisplayActivity extends Activity {


    private Record record;

    private RecordDisplayView recordDisplayView;
    private TextView refractoryView;
    private TextView deltaView;
    private TextView dateView;
    private TextView durationView;
    private TextView nameView;
    private TextView stability;
    private TextView scalar;
    private TextView threshold;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_display);

        recordDisplayView = (RecordDisplayView) findViewById(R.id.canvas);

        // ge the text views
        refractoryView = (TextView) findViewById(R.id.refractory);
        deltaView = (TextView) findViewById(R.id.delta);
        stability = (TextView) findViewById(R.id.stability);
        threshold = (TextView) findViewById(R.id.threshold);
        scalar = (TextView) findViewById(R.id.scalar);

        nameView = (TextView) findViewById(R.id.name);
        dateView = (TextView) findViewById(R.id.date);
        durationView = (TextView) findViewById(R.id.duration);

        // make the record from the string bundle
        record = new Record(getIntent().getExtras().getString("record").split("[;]+"));
        recordDisplayView.setRecord(record);

        // set the text views
        refractoryView.setText("Refractory: " + record.refractory);
        deltaView.setText("Delta: " + record.delta);
        stability.setText("Stability: " + record.stability);
        scalar.setText("Scalar: " + record.scalar);
        threshold.setText("Threshold: " + record.threshold);

        nameView.setText(record.name);
        dateView.setText(record.time);
        durationView.setText(record.duration + " secs");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.record_display, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.wobble:
                startActivity(new Intent(this, WobbleActivity.class));
                return true;
            case R.id.recordings:
                startActivity(new Intent(this, RecordingsActivity.class));
                return true;
        }
        return false;
    }

}
