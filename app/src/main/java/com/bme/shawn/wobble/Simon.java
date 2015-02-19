package com.bme.shawn.wobble;

import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.Log;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by shawn on 11/4/14.
 */
public class Simon {

    // paints
    private Paint mButtonPaint;
    private Paint mRedPaint;
    private Paint mGreenPaint;
    private Paint mBluePaint;
    private Paint mYellowPaint;

    // paths
    private Path mBluePath = new Path();
    private Path mYellowPath = new Path();
    private Path mGreenPath = new Path();
    private Path mRedPath = new Path();

    // ovals for drawing buttons
    private RectF mOutterOval;
    private RectF mInnerOval;
    private float mInnerRadius;
    private float mOuterRadius;

    // Drawing Constants
    private static final float X_PADDING = 20;
    private static final float SWEEP_SPREAD = 10;   // this is half of the total angle between buttons
    private static final float BUTTON_ANGLE_SWEEP = 90 - 2 * SWEEP_SPREAD;

    // constant buttons
    public static final int NO_BUTTON = 0;
    public static final int BLUE_BUTTON = 1;
    public static final int YELLOW_BUTTON = 2;
    public static final int GREEN_BUTTON = 3;
    public static final int RED_BUTTON = 4;

    // game statuses
    public static final int GAME_OVER = -1;
    public static final int GAME_RUNNING = 0;
    public static final int GAME_PAUSED = 1;
    public static final int GAME_RESTART = 2;
    public static final int GAME_EXIT = 3;

    // button polygons
    private Polygon mRedPoly;
    private Polygon mBluePoly;
    private Polygon mYellowPoly;
    private Polygon mGreenPoly;

    // delay on the move cycles
    private static final int STEP_TIME = 600;

    // current status of the game
    private int mStatus;

    // time of previous animation update (ms)
    private long mLastTime;

    // animation is on
    private boolean mShowingAnimation;

    // a button is turned on
    private boolean mButtonOn;

    // the button that is on
    private int mButton;

    // top score this session
    private int mTopScore;

    // the current score
    private int score;

    // the current place in the sequence
    private int index;

    // game user message
    private String message;

    // sequence pattern
    private ArrayList<Integer> pattern;

    public Simon() {

        // initiate pattern
        pattern = new ArrayList<Integer>();
        addMove();

        // set up some status variables
        mShowingAnimation = true;
        mButtonOn = false;
        mButton = 0;
        index = 0;
        mLastTime = System.currentTimeMillis() + 100;
        mStatus = GAME_RUNNING;
        message = "";

        // set scores
        mTopScore = 1;
        score = 0;

        // Simon button paint
        mButtonPaint = new Paint();
        mButtonPaint.setStyle(Paint.Style.FILL);
        mButtonPaint.setAntiAlias(true);
        mButtonPaint.setStrokeWidth(20);

        // red paint
        mRedPaint = new Paint(mButtonPaint);
        mRedPaint.setARGB(255, 204, 0, 0);

        // blue paint
        mBluePaint = new Paint(mButtonPaint);
        mBluePaint.setARGB(255, 51, 102, 255);

        // yellow paint
        mYellowPaint = new Paint(mButtonPaint);
        mYellowPaint.setARGB(255, 255, 204, 0);

        // green paint
        mGreenPaint = new Paint(mButtonPaint);
        mGreenPaint.setARGB(255, 77, 184, 112);
    }

    public void setRunning(){
        mStatus = GAME_RUNNING;
    }

    public void setPaused(){
        mStatus = GAME_PAUSED;
    }

    // returns the updated button to turn on
    public int getButton(){
        return mButton;
    }

    // returns the updated message
    public String getMessage(){
        return message;
    }

    // returns the current score
    public String getScore(){
        // todo give the top and current score here
        return Integer.toString(score);
    }

    // update the game
    public void update(int button) {

        // catch game status
        switch(mStatus) {
            case GAME_RESTART:
                reset();
                break;
            case GAME_OVER:
                message = "Game Over";
                break;
            case GAME_EXIT:
                message = "Exiting...";
        }

        // get current time
        long now = System.currentTimeMillis();

        // are we showing animation
        if(mShowingAnimation){
            updateAnimation(button, now);
        } else{
            handleUserInput(button, now);
        }
    }

    // handles updating the game based current button contact and time
    private void handleUserInput(int button, long now){

        // finished all steps
        if(index == score){

            // add a move
            addMove();

            // update game state
            index = 0;
            mShowingAnimation = true;
            mButtonOn = false;

            // give animation a little delay
            mLastTime = now + STEP_TIME;

        // touching a button and pass delay
        }else if(button != NO_BUTTON && !mButtonOn) {
            // wrong
            if (button != pattern.get(index)) {
                // game over
                mButton = NO_BUTTON;
                mStatus = GAME_OVER;
                message = "Game Over";
            // correct
            } else if (!mButtonOn){
                mButton = button;
                index++;
                mButtonOn = true;
                message = "Correct!";
            }
            mLastTime = now;
        }else if(button == NO_BUTTON){
            mButton = NO_BUTTON;
            mButtonOn = false;
        }
    }

    // updates the animation sequence
    private void updateAnimation(int button, long now){

        // give the graphics 100 ms to calculate (only when Simon object is first created)
        if (mLastTime > now)
            return;

        // set up animation mode
        if(button == NO_BUTTON && !mButtonOn) {
            mButton = NO_BUTTON;
            message = "watch...";
        }

        // update the animation
        if (now - mLastTime > STEP_TIME){
            if(index == score){
                mButtonOn = false;
                index = 0;
                mButton = NO_BUTTON;
                mShowingAnimation = false;
                message = "Go!";

            }else if(mButtonOn){
                mButtonOn = false;
                index++;
                mButton = NO_BUTTON;
            }else {
                mButtonOn = true;
                mButton = pattern.get(index);
            }
            mLastTime = now;
        }
    }

    // adds a move to the
    private void addMove(){
        Random rand = new Random();
        int button = rand.nextInt(4) + 1;
        pattern.add(button);
        score++;
    }

    // clears the pattern and restarts the game
    public void reset(){

        // set up some status variables
        mShowingAnimation = true;
        mButtonOn = false;
        mButton = 0;
        index = 0;
        message = "Tap the screen to start";

        // pause game
        mStatus = GAME_PAUSED;

        pattern.clear();
        score = 0;
    }

    // returns the button that the accelerometer locator is touching
    private int findContact(int x, int y){

        // is it blue button
        if(mBluePoly.contains(x, y))
            return BLUE_BUTTON;
        else if(mYellowPoly.contains(x, y))
            return YELLOW_BUTTON;
        else if(mGreenPoly.contains(x, y))
            return GREEN_BUTTON;
        else if(mRedPoly.contains(x, y))
            return RED_BUTTON;

        return NO_BUTTON;
    }

    // updates the screen adding STROKE to the specified paint
    private void updateButtonPaint(int contact){
        // Simon button out-line
        switch(contact){
            case 0:
                mBluePaint.setStyle(Paint.Style.FILL);
                mYellowPaint.setStyle(Paint.Style.FILL);
                mGreenPaint.setStyle(Paint.Style.FILL);
                mRedPaint.setStyle(Paint.Style.FILL);
                break;
            case 1:
                mBluePaint.setStyle(Paint.Style.FILL_AND_STROKE);
                mYellowPaint.setStyle(Paint.Style.FILL);
                mGreenPaint.setStyle(Paint.Style.FILL);
                mRedPaint.setStyle(Paint.Style.FILL);
                break;

            case 2:
                mBluePaint.setStyle(Paint.Style.FILL);
                mYellowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                mGreenPaint.setStyle(Paint.Style.FILL);
                mRedPaint.setStyle(Paint.Style.FILL);
                break;

            case 3:
                mBluePaint.setStyle(Paint.Style.FILL);
                mYellowPaint.setStyle(Paint.Style.FILL);
                mGreenPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                mRedPaint.setStyle(Paint.Style.FILL);
                break;

            case 4:
                mBluePaint.setStyle(Paint.Style.FILL);
                mYellowPaint.setStyle(Paint.Style.FILL);
                mGreenPaint.setStyle(Paint.Style.FILL);
                mRedPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                break;
        }
    }

    public void updateGraphics(int xCenter, int yCenter){

        // resize Simon Oval
        mOuterRadius = xCenter - X_PADDING;
        mInnerRadius = (xCenter - X_PADDING)/2;
        mOutterOval = new RectF(xCenter - mOuterRadius, yCenter - mOuterRadius, xCenter + mOuterRadius, yCenter + mOuterRadius);
        mInnerOval = new RectF(xCenter - mInnerRadius, yCenter - mInnerRadius, xCenter + mInnerRadius, yCenter + mInnerRadius);

        /*****************************************
         * update yellow path
         *******************************************/
        mYellowPath.reset();
        mYellowPath.addArc(mOutterOval, 90 + SWEEP_SPREAD / 2, BUTTON_ANGLE_SWEEP + SWEEP_SPREAD);
        mYellowPath.arcTo(mInnerOval, 180 - SWEEP_SPREAD, -BUTTON_ANGLE_SWEEP);
        mYellowPath.close();

        // store yellow polygon
        float[] yellow_x = new float[] {
                xCenter - mOuterRadius,
                xCenter - mInnerRadius,
                xCenter - SWEEP_SPREAD,
                xCenter - SWEEP_SPREAD
        };
        float[] yellow_y = new float[] {
                yCenter + SWEEP_SPREAD,
                yCenter + SWEEP_SPREAD,
                yCenter + mInnerRadius,
                yCenter + mOuterRadius
        };
        mYellowPoly = new Polygon(yellow_x, yellow_y, 4);

        /******************************************
         * update green path
         ********************************************/
        mGreenPath.reset();
        mGreenPath.addArc(mOutterOval, 180 + SWEEP_SPREAD / 2, BUTTON_ANGLE_SWEEP + SWEEP_SPREAD);
        mGreenPath.arcTo(mInnerOval, 270 - SWEEP_SPREAD, -BUTTON_ANGLE_SWEEP);
        mGreenPath.close();

        // store green polygon
        float[] green_x = new float[] {
                xCenter - mInnerRadius,
                xCenter - mOuterRadius,
                xCenter - SWEEP_SPREAD,
                xCenter - SWEEP_SPREAD
        };
        float[] green_y = new float[] {
                yCenter - SWEEP_SPREAD,
                yCenter - SWEEP_SPREAD,
                yCenter - mOuterRadius,
                yCenter - mInnerRadius
        };
        mGreenPoly = new Polygon(green_x, green_y, 4);

        /******************************************
         * update blue path
         *********************************************/
        mBluePath.reset();
        mBluePath.addArc(mOutterOval, SWEEP_SPREAD / 2, BUTTON_ANGLE_SWEEP + SWEEP_SPREAD);
        mBluePath.arcTo(mInnerOval, 90 - SWEEP_SPREAD, -BUTTON_ANGLE_SWEEP);
        mBluePath.close();

        // store blue polygon
        float[] blue_x = new float[] {
                xCenter + mInnerRadius,
                xCenter + mOuterRadius,
                xCenter + SWEEP_SPREAD,
                xCenter + SWEEP_SPREAD
        };
        float[] blue_y = new float[] {
                yCenter + SWEEP_SPREAD,
                yCenter + SWEEP_SPREAD,
                yCenter + mOuterRadius,
                yCenter + mInnerRadius
        };
        mBluePoly = new Polygon(blue_x, blue_y, 4);

        /*****************************************
         * update red path
         *********************************************/
        mRedPath.reset();
        mRedPath.addArc(mOutterOval, 270 + SWEEP_SPREAD / 2, BUTTON_ANGLE_SWEEP + SWEEP_SPREAD);
        mRedPath.arcTo(mInnerOval, 0 - SWEEP_SPREAD, -BUTTON_ANGLE_SWEEP);
        mRedPath.close();

        // store red polygon
        float[] red_x = new float[] {
                xCenter + SWEEP_SPREAD,
                xCenter + SWEEP_SPREAD,
                xCenter + mOuterRadius,
                xCenter + mInnerRadius
        };
        float[] red_y = new float[] {
                yCenter - mInnerRadius,
                yCenter - mOuterRadius,
                yCenter - SWEEP_SPREAD,
                yCenter - SWEEP_SPREAD
        };
        mRedPoly = new Polygon(red_x, red_y, 4);
    }

}

/*
        // draw
        canvas.drawPath(mBluePath, mBluePaint);
        canvas.drawPath(mYellowPath, mYellowPaint);
        canvas.drawPath(mGreenPath, mGreenPaint);
        canvas.drawPath(mRedPath, mRedPaint);
 */
