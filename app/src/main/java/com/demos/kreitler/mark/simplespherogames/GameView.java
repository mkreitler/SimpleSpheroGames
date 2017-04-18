/*
 * Copyright (C) 2007 The Android Open Source Project
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
package com.demos.kreitler.mark.simplespherogames;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import android.bluetooth.BluetoothAdapter;

import com.demos.kreitler.mark.demouilib.WidgetBase;
import com.demos.kreitler.mark.lib_demo_sprites.Sprite;
import com.demos.kreitler.mark.simplespherogames.Prototypes.*;
import com.orbotix.ConvenienceRobot;
import com.orbotix.async.CollisionDetectedAsyncData;
import com.orbotix.async.DeviceSensorAsyncMessage;
import com.orbotix.common.ResponseListener;
import com.orbotix.common.Robot;
import com.orbotix.common.RobotChangedStateListener;
import com.orbotix.common.internal.AsyncMessage;
import com.orbotix.common.internal.DeviceResponse;
import com.orbotix.common.sensor.DeviceSensorsData;
import com.orbotix.common.sensor.LocatorData;
import com.orbotix.common.sensor.SensorFlag;
import com.orbotix.subsystem.SensorControl;

import java.util.Hashtable;
import java.util.List;

import com.demos.kreitler.mark.simplespherogames.Prototypes.*;


/**
 * View that draws, takes keystrokes, etc. for a simple LunarLander game.
 *
 * Has a mode which RUNNING, PAUSED, etc. Has a x, y, dx, dy, ... capturing the
 * current ship physics. All x/y etc. are measured with (0,0) at the lower left.
 * updatePhysics() advances the physics based on realtime. draw() renders the
 * ship, and does an invalidate() to prompt another draw() as soon as possible
 * by the system.
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    public static Paint _paint      = new Paint();
    public static Typeface _font    = null;

    public class GameThread extends Thread implements RobotChangedStateListener, ResponseListener  {
        protected SurfaceHolder mSurfaceHolder  = null;
        protected Handler mHandler              = null;
        protected Context mContext              = null;
        protected boolean mRun                  = false;
        protected final Object mRunLock         = new Object();
        protected int mCanvasWidth              = 0;
        protected int mCanvasHeight             = 0;
        protected GameStateBase currentState    = null;
        protected ConvenienceRobot mRobot       = null;
        protected boolean bStarted              = false;

        protected Hashtable<String, GameStateBase> gameStateTable               = null;

        protected GameStateFail gameStateFail                                   = null;
        protected GameStateIntro gameStateIntro                                 = null;
        protected GameStateFindRobot gameStateFindRobot                         = null;
        protected GameStateMainMenu gameStateMainMenu                           = null;
        protected GameStateSideTossIntro gameStateSideTossIntro                 = null;
        protected GameStateSideToss gameStateSideToss                           = null;
        protected GameStateGyroCopterIntro gameStateGyroCopterIntro             = null;
        protected GameStateGyroCopter gameStateGyroCopter                       = null;
        protected GameStateFuseIntro gameStateFuseIntro                         = null;
        protected GameStateFuse gameStateFuse                                   = null;
        protected GameStateTiltDriveIntro gameStateTiltDriveIntro               = null;
        protected GameStateTiltDrive gameStateTiltDrive                         = null;
        protected GameStateSlingshotDriveIntro gameStateSlingshotDriveIntro     = null;
        protected GameStateSlingshotDrive gameStateSlingshotDrive               = null;
        protected GameStateMinefieldIntro gameStateMinefieldIntro               = null;
        protected GameStateMinefield gameStateMinefield                         = null;

        public GameThread(SurfaceHolder surfaceHolder, Context context,
                          Handler handler) {
            mSurfaceHolder = surfaceHolder;
            mHandler = handler;
            mContext = context;
        }

        // RobotChangedStateListener Interface /////////////////////////////////////////////////////////
        @Override
        public void handleRobotChangedState(Robot robot, RobotChangedStateListener.RobotChangedStateNotificationType type ) {
            Log.d("Sphero", ">>> Handle robot state change <<<");

            switch( type ) {
                case Offline:
                    Log.d("Sphero", ">>> Robot offline");
                    break;

                case Connecting:
                    gameStateFindRobot.Connecting();
                    Log.d("Sphero", ">>> Robot connecting");
                    break;

                case Connected:
                    gameStateFindRobot.Connected();
                    Log.d("Sphero", ">>> Robot connected");
                    break;

                case Online: {

                    // If robot uses Bluetooth LE, Developer Mode can be turned on.
                    // This turns off DOS protection. This generally isn't required.
                    // if( robot instanceof RobotLE) {
                    //    ( (RobotLE) robot ).setDeveloperMode( true );
                    // }
                    Log.d("Sphero", ">>> Robot online");

                    // Save the robot as a ConvenienceRobot for additional utility methods
                    mRobot = new ConvenienceRobot( robot );
                    long mask = SensorFlag.ACCELEROMETER_NORMALIZED.longValue() | SensorFlag.ATTITUDE.longValue() | SensorFlag.LOCATOR.longValue();
                    mRobot.enableSensors(mask, SensorControl.StreamingRate.STREAMING_RATE10);
                    mRobot.enableCollisions(true);
                    mRobot.addResponseListener(this);

                    gameStateFindRobot.Online();
                    break;
                }

                case Disconnected: {
                    Log.d("Sphero", ">>> Disconnected");
                    setState("GameStateFindRobot");
                    break;
                }

                case FailedConnect:
                    Fail("Failed to connect to robot.");
                    break;

                default: {
                    Fail("Unknown error during handleRobotChangedState.");
                    break;
                }
            }
        }

        public ConvenienceRobot getRobot() {
            return mRobot;
        }

        // ResponseListener Interface //////////////////////////////////////////////////////////////////
        private final float COLLISION_STRONG = 200.0f;
        private final float COLLISION_MEDIUM = 125.0f;
        private final float COLLISION_WEAK   = 75.0f;

        public void handleResponse(DeviceResponse response, Robot robot) {
            // Do something with the response here
        }

        @Override
        public void handleStringResponse(String stringResponse, Robot robot) {
            // Handle string responses from the robot here
        }

        @Override
        public void handleAsyncMessage(AsyncMessage asyncMessage, Robot robot) {
            // Log.d("GameView", "Incoming async message...");
            if(asyncMessage instanceof CollisionDetectedAsyncData) {
                //Collision occurred.
                if (currentState != null)
                currentState.HandleCollisionMessage((CollisionDetectedAsyncData)asyncMessage);
            }
            else if (asyncMessage instanceof DeviceSensorAsyncMessage) {
                if (currentState != null) {
                    currentState.HandleSensorData((DeviceSensorAsyncMessage) asyncMessage);
                }
            }
        }

        public Resources GetResources() {
            return mContext.getResources();
        }

        public void playSound(int soundID) {
            final MediaPlayer mp = MediaPlayer.create(getContext(), soundID);
            mp.start();
        }

        public void OnPause() {
            if (currentState != null) {
                currentState.OnPause();
            }
        }

        public void OnResume() {
            if (currentState != null) {
                currentState.OnResume();
            }
        }

        private void CreateStates() {
            gameStateTable  = new Hashtable<String, GameStateBase>();

            gameStateFail   = new GameStateFail(this);
            gameStateTable.put("gamestatefail", gameStateFail);

            gameStateIntro  = new GameStateIntro(this);
            gameStateTable.put("gamestateintro", gameStateIntro);

            gameStateFindRobot = new GameStateFindRobot(this, mContext);
            gameStateTable.put("gamestatefindrobot", gameStateFindRobot);

            gameStateMainMenu = new GameStateMainMenu(this);
            gameStateTable.put("gamestatemainmenu", gameStateMainMenu);

            gameStateSideTossIntro = new GameStateSideTossIntro(this);
            gameStateTable.put("gamestatesidetossintro", gameStateSideTossIntro);

            gameStateSideToss = new GameStateSideToss(this);
            gameStateTable.put("gamestatesidetoss", gameStateSideToss);

            gameStateGyroCopterIntro = new GameStateGyroCopterIntro(this);
            gameStateTable.put("gamestategyrocopterintro", gameStateGyroCopterIntro);

            gameStateGyroCopter = new GameStateGyroCopter(this);
            gameStateTable.put("gamestategyrocopter", gameStateGyroCopter);

            gameStateFuseIntro = new GameStateFuseIntro(this);
            gameStateTable.put("gamestatefuseintro", gameStateFuseIntro);

            gameStateFuse = new GameStateFuse(this);
            gameStateTable.put("gamestatefuse", gameStateFuse);

            gameStateTiltDriveIntro = new GameStateTiltDriveIntro(this);
            gameStateTable.put("gamestatetiltdriveintro", gameStateTiltDriveIntro);

            gameStateTiltDrive = new GameStateTiltDrive(this);
            gameStateTable.put("gamestatetiltdrive", gameStateTiltDrive);

            gameStateSlingshotDriveIntro = new GameStateSlingshotDriveIntro(this);
            gameStateTable.put("gamestateslingshotdriveintro", gameStateSlingshotDriveIntro);

            gameStateSlingshotDrive = new GameStateSlingshotDrive(this);
            gameStateTable.put("gamestateslingshotdrive", gameStateSlingshotDrive);

            gameStateMinefieldIntro = new GameStateMinefieldIntro(this);
            gameStateTable.put("gamestateminefieldintro", gameStateMinefieldIntro);

            gameStateMinefield = new GameStateMinefield(this);
            gameStateTable.put("gamestateminefield", gameStateMinefield);

            setState("gameStateIntro");
        }

        private void InitGraphics() {
            Sprite.setPaint(GameView._paint);
        }

        public Context getContext() {
            return mContext;
        }

        public void Fail(String message) {
            gameStateFail.SetMessage(message);
            setState("gameStateFail");
        }

        /**
         * Starts the game, setting parameters for the current difficulty.
         */
        public void doStart() {
            synchronized (mSurfaceHolder) {
                mRun = true;
            }
        }

        /**
         * Stops the game.
         */
        public void doStop() {
            BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
            if (bluetooth != null && bluetooth.isDiscovering()) {
                bluetooth.cancelDiscovery();
            }

            synchronized(mSurfaceHolder) {
                mRun = false;
            }
        }

        /**
         * Pauses the physics update & animation.
         */
        public void pause() {
            synchronized (mSurfaceHolder) {
            }
        }

        /**
         * Resumes from a pause.
         */
        public void unpause() {
            // Move the real time clock up to now
            synchronized (mSurfaceHolder) {
            }
        }

        /**
         * Restores game state from the indicated Bundle. Typically called when
         * the Activity is being restored after having been previously
         * destroyed.
         *
         * @param savedState Bundle containing the game state
         */
        public synchronized void restoreState(Bundle savedState) {
            synchronized (mSurfaceHolder) {
            }
        }

        public int width() {
            return mCanvasWidth;
        }

        public int height() {
            return mCanvasHeight;
        }

        @Override
        public void run() {
            long lastTimeMS = java.lang.System.currentTimeMillis();

            while (mRun) {
                Canvas c = null;
                try {
                    c = mSurfaceHolder.lockCanvas(null);
                    synchronized (mSurfaceHolder) {
                        // Critical section. Do not allow mRun to be set false until
                        // we are sure all canvas draw operations are complete.
                        //
                        // If mRun has been toggled false, inhibit canvas operations.
                        long currentTimeMS = java.lang.System.currentTimeMillis();
                        int dtMS = (int)(currentTimeMS - lastTimeMS);
                        lastTimeMS = currentTimeMS;

                        if (currentState != null) {
                            // TODO: check return value and exit it true?
                            currentState.Update(dtMS);
                        }

                        synchronized (mRunLock) {
                            if (mRun) doDraw(c);
                        }
                    }
                } finally {
                    // do this in a finally so that if an exception is thrown
                    // during the above, we don't leave the Surface in an
                    // inconsistent state
                    if (c != null) {
                        mSurfaceHolder.unlockCanvasAndPost(c);
                    }
                }
            }
        }

        /**
         * Dump game state to the provided Bundle. Typically called when the
         * Activity is being suspended.
         *
         * @return Bundle with this view's state
         */
        public Bundle saveState(Bundle map) {
            synchronized (mSurfaceHolder) {
                /*
                if (map != null) {
                    map.putInt(KEY_DIFFICULTY, Integer.valueOf(mDifficulty));
                    map.putDouble(KEY_X, Double.valueOf(mX));
                    map.putDouble(KEY_Y, Double.valueOf(mY));
                    map.putDouble(KEY_DX, Double.valueOf(mDX));
                    map.putDouble(KEY_DY, Double.valueOf(mDY));
                    map.putDouble(KEY_HEADING, Double.valueOf(mHeading));
                    map.putInt(KEY_LANDER_WIDTH, Integer.valueOf(mLanderWidth));
                    map.putInt(KEY_LANDER_HEIGHT, Integer
                            .valueOf(mLanderHeight));
                    map.putInt(KEY_GOAL_X, Integer.valueOf(mGoalX));
                    map.putInt(KEY_GOAL_SPEED, Integer.valueOf(mGoalSpeed));
                    map.putInt(KEY_GOAL_ANGLE, Integer.valueOf(mGoalAngle));
                    map.putInt(KEY_GOAL_WIDTH, Integer.valueOf(mGoalWidth));
                    map.putInt(KEY_WINS, Integer.valueOf(mWinsInARow));
                    map.putDouble(KEY_FUEL, Double.valueOf(mFuel));
                }
                */
            }
            return map;
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
            // are potentially in-flight. See doDraw().
            if (mRun != b) {
                synchronized (mRunLock) {
                    mRun = b;
                }
            }
        }

        /**
         * Sets the game mode. That is, whether we are running, paused, in the
         * failure state, in the victory state, etc.
         */
        public void setState(String stateName) {
            stateName = stateName != null ? stateName.toLowerCase() : null;
            GameStateBase newState = stateName != null ? gameStateTable.get(stateName) : null;

            synchronized (mSurfaceHolder) {
                if (currentState != newState) {
                    if (currentState != null) {
                        currentState.Exit();
                    }

                    if (newState != null) {
                        newState.Enter(this);
                    }

                    currentState = newState;
                }
            }
        }

        /* Callback invoked when the surface dimensions change. */
        public void setSurfaceSize(int width, int height) {
            // synchronized to make sure these all change atomically
            synchronized (mSurfaceHolder) {
                mCanvasWidth = width;
                mCanvasHeight = height;

                if (!bStarted) {
                    InitGraphics();
                    CreateStates();

                    bStarted = true;
                }

                /*
                // don't forget to resize the background image
                mBackgroundImage = Bitmap.createScaledBitmap(
                        mBackgroundImage, width, height, true);
                */
            }
        }

        public boolean onTouch(View v, MotionEvent e) {
            boolean bHandled = false;

            synchronized(mSurfaceHolder) {
                if (currentState != null) {
                    bHandled = currentState.OnTouch(v, e);
                }
            }

            return bHandled;
        }

        private void doDraw(Canvas canvas) {
            if (currentState != null) {
                currentState.Draw(canvas);
            }
        }
    }

    // GameView Interface //////////////////////////////////////////////////////////////////////////
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean bHandled = false;

        if (thread != null) {
            bHandled = thread.onTouch(this, event);
        }

        return bHandled;
    }

    /** Pointer to the text view to display "Paused.." etc. */
    /** The thread that actually draws the animation */
    private GameThread thread   = null;

    public GameView(Context context) {
        super(context);
        createThreadAndResources(context);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        createThreadAndResources(context);
    }

    public GameView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        createThreadAndResources(context);
    }

    protected void initUI(Context context) {
        WidgetBase.SetContext(context);
    }

    protected void createThreadAndResources(Context context) {
        Resources res = context.getResources();
        _font = Typeface.createFromAsset(res.getAssets(), "fonts/FindleyBold.ttf");

        initUI(context);

        // register our interest in hearing about changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        // create thread only; it's started in surfaceCreated()
        thread = new GameThread(holder, context, new Handler() {
            @Override
            public void handleMessage(Message m) {
            }
        });
        setFocusable(true); // make sure we get key events
    }

    /**
     * Fetches the animation thread corresponding to this LunarView.
     *
     * @return the animation thread
     */
    public GameThread getThread() {
        return thread;
    }

    public void onStop() {
        if (thread != null) {
            thread.doStop();
        }
    }

    public void onStart() {
        if (thread != null) {
            thread.doStart();
        }
    }

    /**
     * Standard window-focus override. Notice focus lost so we can pause on
     * focus lost. e.g. user switches to take a call.
     */
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (!hasWindowFocus) thread.pause();
    }

    /* Callback invoked when the surface dimensions change. */
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        thread.setSurfaceSize(width, height);
    }
    /*
     * Callback invoked when the Surface has been created and is ready to be
     * used.
     */
    public void surfaceCreated(SurfaceHolder holder) {
        // start the thread here so that we don't busy-wait in run()
        // waiting for the surface to be created
        thread.setRunning(true);
        thread.start();
    }
    /*
     * Callback invoked when the Surface has been destroyed and must no longer
     * be touched. WARNING: after this method returns, the Surface/Canvas must
     * never be touched again!
     */
    public void surfaceDestroyed(SurfaceHolder holder) {
        // we have to tell thread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
        boolean retry = true;
        thread.setRunning(false);
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
    }

    public void OnPause() {
        getThread().OnPause();
    }

    public void OnResume() {
        getThread().OnResume();
    }
}
