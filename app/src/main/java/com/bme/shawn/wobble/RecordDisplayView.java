package com.bme.shawn.wobble;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.example.android.opengl.R;

class RecordDisplayView extends View {

    private float XCenter;
    private float YCenter;
    private float BoardRadius;
    private float X_PADDING = 20f;
    private Bitmap BackgroundImage;

    private Record record;

    public RecordDisplayView(Context context, AttributeSet attrs) {
        super(context, attrs);

        //Going to want a black background
        Resources res = context.getResources();
        //BackgroundImage = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        BackgroundImage = BitmapFactory.decodeResource(res, R.drawable.black);
    }

    public void setRecord(Record _record){
        record = _record;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(record == null){
            return;
        }

        XCenter = getWidth()/2;
        YCenter = getHeight()/2;

        // don't forget to resize the background image
        //BackgroundImage = Bitmap.createScaledBitmap(BackgroundImage, getWidth(), getHeight(), true);

        BoardRadius = XCenter - X_PADDING;

        Paint boardPaint = new Paint();
        boardPaint.setStyle(Paint.Style.STROKE);
        boardPaint.setStrokeWidth(10);
        boardPaint.setARGB(255, 255, 255, 255);

        Paint pathPaint = new Paint();
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeWidth(10);
        pathPaint.setARGB(255, 200, 0, 0);

        Paint pointPaint = new Paint();
        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setARGB(255, 0, 102, 255);

        Paint spikePointPaint = new Paint();
        spikePointPaint.setStyle(Paint.Style.FILL);
        spikePointPaint.setARGB(255, 255, 153, 0);

        // draw all of the points
        int pointCount = record.points.size();
        for(int i = 0; i < pointCount - 1; i++){
            canvas.drawCircle(record.points.get(i).x, record.points.get(i).y, 5, pointPaint);

            // get point distance
            float deltaX = Math.abs(record.points.get(i).x - record.points.get(i + 1).x);
            float deltaY = Math.abs(record.points.get(i).y - record.points.get(i + 1).y);
            float distance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);

            canvas.drawLine(record.points.get(i).x, record.points.get(i).y, record.points.get(i + 1).x, record.points.get(i + 1).y, pathPaint);
        }

        int spikePointCount = record.spikePoints.size();
        for(int i = 0; i < spikePointCount - 1; i++){
            canvas.drawCircle(record.spikePoints.get(i).x, record.spikePoints.get(i).y, 5, spikePointPaint);
        }

        // draw the last point
        canvas.drawCircle(record.points.get(pointCount - 1).x, record.points.get(pointCount - 1).y, 10, pointPaint);

        // draw the board outline
        canvas.drawCircle(XCenter, YCenter, BoardRadius, boardPaint);
    }
}
