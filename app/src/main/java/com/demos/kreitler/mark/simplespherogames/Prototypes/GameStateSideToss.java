package com.demos.kreitler.mark.simplespherogames.Prototypes;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.transition.Slide;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.demos.kreitler.mark.demouilib.IWidgetListener;
import com.demos.kreitler.mark.demouilib.WidgetBase;
import com.demos.kreitler.mark.demouilib.WidgetLabel;
import com.demos.kreitler.mark.demouilib.WidgetList;
import com.demos.kreitler.mark.lib_demo_sprites.Sprite;
import com.demos.kreitler.mark.simplespherogames.GameStateBase;
import com.demos.kreitler.mark.simplespherogames.GameView;
import com.demos.kreitler.mark.simplespherogames.R;
import com.orbotix.ConvenienceRobot;
import com.orbotix.async.CollisionDetectedAsyncData;
import com.orbotix.async.DeviceSensorAsyncMessage;
import com.orbotix.common.ResponseListener;
import com.orbotix.common.Robot;
import com.orbotix.common.internal.AsyncMessage;
import com.orbotix.common.internal.DeviceResponse;
import com.orbotix.common.sensor.DeviceSensorsData;
import com.orbotix.common.sensor.SensorFlag;
import com.orbotix.common.sensor.SensorFlag.*;
import com.orbotix.subsystem.SensorControl;

import java.util.Vector;

/**
 * Created by Mark on 6/22/2016.
 */
public class GameStateSideToss extends GameStateBase implements IWidgetListener {
    // Interface ///////////////////////////////////////////////////////////////////////////////////
    // Static --------------------------------------------------------------------------------------
    // Instance ------------------------------------------------------------------------------------
    private final int FONT_SIZE                 = 33;
    private final int LIST_VERTICAL_SPACING     = 40;

    private final int SS_NONE                   = 0;
    private final int SS_AWAITING_THROW         = 1;
    private final int SS_IN_FLIGHT              = 2;

    private final int STATE_BLOCK_TIME          = 150;
    private final float VERTICAL_THRESH         = 0.9f;
    private final float THROW_THRESH            = 1.5f;
    private final float CATCH_THRESH            = 1.25f;
    private final int IMPACT_POWER_THRESH       = 100;
    private final int COLLISION_WAIT_TIME       = 67;
    private final int ANIM_TIME_MS              = 33;
    private final int STARTING_SPAWN_TIME_MS    = 500;
    private final float SLOW_PIXELS_PER_MS      = 400.0f / 1000.0f;
    private final float FAST_PIXELS_PER_MS      = 800.0f / 1000.0f;
    private final int DIFFICULTY_PERIOD_MS      = 30000;
    private final int COLLISION_RADIUS          = 50;
    private final int END_GAME_DELAY            = 3000;

    private WidgetList listOptions      = null;
    private WidgetBase lastTouched      = null;
    private String selectedOption       = null;
    private boolean bThrowingLeft       = true;
    private int stateBlockTimer         = 0;
    private boolean bCollisionOccurred  = false;
    private int collisionWaitTimer      = 0;
    private Sprite dragonfly            = null;
    private Vector<Sprite> beetles      = new Vector<>();
    private Vector<Sprite> inFlight     = new Vector<>();
    BitmapDrawable[] dragonflyFrames    = {null, null};
    Drawable drawableBeetleBlue         = null;
    Drawable drawableBeetleGreen        = null;
    private Sprite[][] rows             = {
            {null, null, null, null, null, null, null, null, null, null},
            {null, null, null, null, null, null, null, null, null, null},
    };
    private int animTimerMS             = 0;

    private int subState            = SS_NONE;
    ConvenienceRobot robot          = null;
    Vector<WidgetLabel>options      = null;
    float accelX                    = 0.0f;
    float accelY                    = 0.0f;
    float accelZ                    = 0.0f;
    int animFrame                   = 0;
    float spawnTimerMS              = 0.0f;
    float dragonFlyPos              = 0.0f;
    int bottomY                     = 0;
    int topY                        = 0;
    private int gameTimerMS         = 0;
    private int score               = 0;
    private boolean bGameOver       = false;
    private int endGameTimerMS      = 0;

    public GameStateSideToss(GameView.GameThread game) {
        super(game);

        Resources res = game.getContext().getResources();

        String titleText = res.getString(R.string.game_over);
        WidgetLabel titleLabel = new WidgetLabel(null, 0, 0, titleText, FONT_SIZE, "black", "fonts/FindleyBold.ttf");

        topY = game.height() / 4;
        bottomY = game.height() * 3 / 4;

        dragonflyFrames[0] = (BitmapDrawable)res.getDrawable(R.drawable.dragonfly_01, null);
        dragonflyFrames[1] = (BitmapDrawable)res.getDrawable(R.drawable.dragonfly_02, null);
        dragonfly = new Sprite(dragonflyFrames[0], 0, bottomY, 0.0f, 0.5f);

        options = new Vector<WidgetLabel>();
        String optionText = res.getString(R.string.score) + "0";
        options.add(new WidgetLabel(null, 0, 0, optionText, FONT_SIZE, "white", "fonts/FindleyBold.ttf"));

        listOptions = new WidgetList(null, 0.5f, LIST_VERTICAL_SPACING, titleLabel, options);
        listOptions.AddListener(this);

        listOptions.SetPosition(game.width() / 2, game.height() / 2);

        drawableBeetleBlue = ((BitmapDrawable)res.getDrawable(R.drawable.beetle_blue, null));
        drawableBeetleGreen = ((BitmapDrawable)res.getDrawable(R.drawable.beetle_green, null));

        for (int i=0; i<rows.length; ++i) {
            for (int j=0; j<rows[0].length; ++j) {
                beetles.add(new Sprite(drawableBeetleBlue, game.width(), game.height() / 2, 0.0f, 0.5f));
            }
        }
    }

    @Override
    public void Enter(GameView.GameThread game) {
        super.Enter(game);

        robot = game.getRobot();
        assert(robot != null);

        long sensorFlag = SensorFlag.QUATERNION.longValue()
                | SensorFlag.ACCELEROMETER_NORMALIZED.longValue()
//                | SensorFlag.GYRO_NORMALIZED.longValue()
//                | SensorFlag.MOTOR_BACKEMF_NORMALIZED.longValue()
//                | SensorFlag.ATTITUDE.longValue();
        ;

        accelX = 0.0f;
        subState = SS_AWAITING_THROW;
        stateBlockTimer = 0;
        bCollisionOccurred = false;
        collisionWaitTimer = 0;
        animFrame = 0;
        animTimerMS = 0;
        gameTimerMS = 0;
        spawnTimerMS = STARTING_SPAWN_TIME_MS;
        score = 0;
        bGameOver = false;
        endGameTimerMS = END_GAME_DELAY;

        robot.getSensorControl().enableSensors(sensorFlag, SensorControl.StreamingRate.STREAMING_RATE20);
        robot.enableStabilization(false);

        lastTouched = null;
    }

    @Override
    public void Exit() {
        robot.getSensorControl().disableSensors();
        robot.enableStabilization(true);
        // robot.setBackLedBrightness(0.0f);
    }

    @Override
    public void Draw(Canvas c) {
        c.save();

        if (bGameOver) {
            c.drawARGB(255, 255, 0, 0);
            listOptions.Draw(c);
        }
        else {
            c.drawARGB(255, 0, 0, 0);

            drawBeetles(c);
            dragonfly.draw(c);
        }

        c.restore();
    }

    @Override
    public boolean Update(int dtMS) {
        boolean bResult = true;

        if (!bGameOver) {
            gameTimerMS += dtMS;

            updateAnimations(dtMS);
            updateSpawn(dtMS);
            updateBeetles(dtMS);

            switch (subState) {
                case SS_AWAITING_THROW: {
                    bResult = UpdateAwaitingThrow(dtMS);
                }
                break;

                case SS_IN_FLIGHT: {
                    bResult = UpdateInFlight(dtMS);
                }
                break;

                default: {
                }
                break;
            }

            checkCollisions();
        }
        else {
            endGameTimerMS -= dtMS;

            if (endGameTimerMS <= 0) {
                game.setState("gameStateMainMenu");
            }
        }

        return bResult;
    }

    @Override
    public boolean OnTouch(View v, MotionEvent e) {
        return listOptions.OnTouch(e);
    }

    @Override
    public void HandleSensorData(DeviceSensorAsyncMessage message) {
        if (message != null) {
            if (message.getAsyncData() != null
                    && !message.getAsyncData().isEmpty()
                    && message.getAsyncData().get(0) != null) {

                //Retrieve DeviceSensorsData from the async message
                DeviceSensorsData data = message.getAsyncData().get(0);

                //Extract the accelerometer data from the sensor data
                accelX = (float)data.getAccelerometerData().getFilteredAcceleration().x;
                accelY = (float)data.getAccelerometerData().getFilteredAcceleration().y;
                accelZ = (float)data.getAccelerometerData().getFilteredAcceleration().z;
            }
        }
    }

    public void HandleCollisionMessage(CollisionDetectedAsyncData colData) {
        int impactX = colData.getImpactPower().x;
        int impactY = colData.getImpactPower().y;

        if (collisionWaitTimer <= 0 &&
                impactX * impactX + impactY * impactY > IMPACT_POWER_THRESH * IMPACT_POWER_THRESH) {
            bCollisionOccurred = true;
        }
    }

    // IWidgetListener -----------------------------------------------------------------------------
    public boolean OnWidgetTouchStart(WidgetBase widget, int localX, int localY) {
        boolean bHandled = false;

        if (lastTouched != null && widget != lastTouched) {
            lastTouched.SetColor("white");
        }
        lastTouched = widget;

        if (widget != null) {
            widget.SetColor("green");
            selectedOption = widget.GetText();
            bHandled = true;
        }

        return bHandled;
    }

    public boolean OnWidgetTouchEnd(WidgetBase widget, int localX, int localY) {
        if (widget != null) {
            widget.SetColor("white");
        }

        if (selectedOption != null) {

            // Load the appropriate state.
            switch (selectedOption.toLowerCase()) {
                // TODO: add additional cases here...

                default: {
                    break;
                }
            }
        }

        return true;
    }

    public boolean OnWidgetDrag(WidgetBase widget, int localX, int localY) {
        boolean bHandled = true;

        if (widget != lastTouched) {
            if (lastTouched != null) {
                lastTouched.SetColor("white");
            }

            widget.SetColor("green");
            lastTouched = widget;

            selectedOption = widget.GetText();
        }

        return bHandled;
    }

    public boolean OnWidgetTouchCancel(WidgetBase widget, int localX, int localY) {
        return OnWidgetTouchEnd(widget, localX, localY);
    }

    // Implementation //////////////////////////////////////////////////////////////////////////////
    // Static --------------------------------------------------------------------------------------
    // Instance ------------------------------------------------------------------------------------
    private void DebugSensorData() {
        WidgetLabel accelLabel = options.elementAt(0);
        assert(accelLabel != null);

        Resources res = game.getContext().getResources();
        accelLabel.SetText(res.getString(R.string.side_toss_debug_ax) + accelX);
    }

    private boolean UpdateAwaitingThrow(int dtMS) {
        stateBlockTimer -= dtMS;

        if (stateBlockTimer <= 0 &&
                accelX * accelX + accelY * accelY + accelX * accelZ > THROW_THRESH * THROW_THRESH) {
            subState = SS_IN_FLIGHT;
            stateBlockTimer = STATE_BLOCK_TIME;
            collisionWaitTimer = COLLISION_WAIT_TIME;
        }

        return true;
    }

    private boolean UpdateInFlight(int dtMS) {
        stateBlockTimer -= dtMS;
        collisionWaitTimer -= dtMS;

        if (stateBlockTimer <= 0 && bCollisionOccurred) {
            if (dragonfly.y() < game.height() / 2) {
                dragonfly.setPosition(dragonfly.x(), bottomY);
            }
            else {
                dragonfly.setPosition(dragonfly.x(), topY);
            }
            subState = SS_AWAITING_THROW;
            bThrowingLeft = !bThrowingLeft;
            stateBlockTimer = STATE_BLOCK_TIME;
            bCollisionOccurred = false;
        }

        return true;
    }

    private void SetDebugText(int resID) {
        WidgetLabel accelLabel = options.elementAt(0);
        Resources res = game.getContext().getResources();
        accelLabel.SetText(res.getString(resID));
    }

    private void updateAnimations(int dtMS) {
        animTimerMS += dtMS;

        while (animTimerMS > ANIM_TIME_MS) {
            animTimerMS -= ANIM_TIME_MS;
            animFrame = 1 - animFrame;
        }

        dragonfly.setDrawable(dragonflyFrames[animFrame]);
    }

    private void spawnBeetles() {
        if (beetles.size() >= rows.length) {
            float easySpawn = 1.0f - ((float)gameTimerMS / (float)DIFFICULTY_PERIOD_MS);
            float medSpawn = 1.0f - ((float)gameTimerMS * 0.5f / (float)DIFFICULTY_PERIOD_MS);

            Resources res = game.getContext().getResources();

            easySpawn = Math.max(0.0f, easySpawn);

            float difficulty = (float)Math.random();

            if (difficulty < easySpawn) {
                // Spawn two blue beetles.
                if (Math.random() < 0.33f) {
                    for (int i = 0; i < rows.length; ++i) {
                        Sprite beetle = beetles.remove(0);
                        beetle.setDrawable(drawableBeetleBlue);
                        beetle.setPosition(game.width(), i == 0 ? topY : bottomY);
                        inFlight.add(beetle);
                    }
                }
                else if (Math.random() < 0.5f) {
                    Sprite beetle = beetles.remove(0);
                    beetle.setDrawable(drawableBeetleBlue);
                    beetle.setPosition(game.width(), topY);
                    inFlight.add(beetle);
                }
                else {
                    Sprite beetle = beetles.remove(0);
                    beetle.setDrawable(drawableBeetleBlue);
                    beetle.setPosition(game.width(), bottomY);
                    inFlight.add(beetle);
                }
            } else if (difficulty < medSpawn) {
                // Spawn blue beetle on top, green on bottom.
                if (Math.random() < 0.5f) {
                    if (Math.random() < 0.5f) {
                        for (int i = 0; i < rows.length; ++i) {
                            Sprite beetle = beetles.remove(0);
                            beetle.setDrawable(i == 0 ? drawableBeetleBlue : drawableBeetleGreen);
                            beetle.setPosition(game.width(), i == 0 ? topY : bottomY);
                            inFlight.add(beetle);
                        }
                    }
                    else {
                        for (int i = 0; i < rows.length; ++i) {
                            Sprite beetle = beetles.remove(0);
                            beetle.setDrawable(i == 0 ? drawableBeetleBlue : drawableBeetleGreen);
                            beetle.setPosition(game.width(), i == 0 ? bottomY : topY);
                            inFlight.add(beetle);
                        }
                    }
                } else if (Math.random() < 0.5f) {
                    Sprite beetle = beetles.remove(0);
                    beetle.setDrawable(Math.random() < 0.5 ? drawableBeetleBlue : drawableBeetleGreen);
                    beetle.setPosition(game.width(), topY);
                    inFlight.add(beetle);
                } else {
                    Sprite beetle = beetles.remove(0);
                    beetle.setDrawable(Math.random() < 0.5 ? drawableBeetleBlue : drawableBeetleGreen);
                    beetle.setPosition(game.width(), bottomY);
                    inFlight.add(beetle);
                }
            }
            else {
                if (Math.random() < 0.5f) {
                    for (int i = 0; i < rows.length; ++i) {
                        Sprite beetle = beetles.remove(0);
                        beetle.setDrawable(i == 0 ? drawableBeetleBlue : drawableBeetleGreen);
                        beetle.setPosition(game.width(), i == 0 ? topY : bottomY);
                        inFlight.add(beetle);
                    }
                }
                else {
                    for (int i = 0; i < rows.length; ++i) {
                        Sprite beetle = beetles.remove(0);
                        beetle.setDrawable(i == 0 ? drawableBeetleBlue : drawableBeetleGreen);
                        beetle.setPosition(game.width(), i == 0 ? bottomY : topY);
                        inFlight.add(beetle);
                    }
                }
            }
        }
    }

    private void updateSpawn(int dtMS) {
        spawnTimerMS -= dtMS;

        if (spawnTimerMS <= 0) {
            spawnTimerMS += STARTING_SPAWN_TIME_MS;
            spawnBeetles();
        }
    }

    private void updateBeetles(int dtMS) {
        float speed = (float)gameTimerMS / (float)DIFFICULTY_PERIOD_MS;
        speed = Math.min(1.0f, Math.max(speed, 0.f));
        speed = SLOW_PIXELS_PER_MS * (1.0f - speed) + FAST_PIXELS_PER_MS * speed;

        float dx = speed * (float)dtMS;

        for (int i=0; i<inFlight.size(); ++i) {
            Sprite beetle = inFlight.elementAt(i);
            float x = beetle.x();
            float y = beetle.y();

            beetle.setPosition(x - dx, y);

            if (beetle.x() < -beetle.width()) {
                inFlight.remove(beetle);
                beetles.add(beetle);
                beetle.setPosition(game.width(), beetle.y());
                i -= 1;
            }
        }
    }

    private void checkCollisions() {
        float playerX = dragonfly.x();
        float playerY = dragonfly.y();

        for (int i=0; i<inFlight.size(); ++i) {
            Sprite beetle = inFlight.elementAt(i);
            assert(beetle != null);

            float dx = playerX - beetle.x();
            float dy = playerY - beetle.y();

            if (dx * dx + dy * dy < COLLISION_RADIUS * COLLISION_RADIUS) {
                Bitmap safeBmp = ((BitmapDrawable)drawableBeetleBlue).getBitmap();

                if (beetle.bitmap() == safeBmp) {
                    score += 1;
                    game.playSound(R.raw.collect);
                }
                else {
                    bGameOver = true;
                }

                beetles.add(inFlight.remove(i));
                i = i - 1;
            }
        }

        if (bGameOver) {
            endGame();
        }
    }

    private void endGame() {
        Resources res = game.getContext().getResources();
        String optionText = res.getString(R.string.score) + score;
        options.elementAt(0).SetText(optionText);

        game.playSound(R.raw.destroy);

        while (inFlight.size() > 0) {
            beetles.add(inFlight.remove(0));
        }
    }

    private void drawBeetles(Canvas c) {
        for (int i=0; i<inFlight.size(); ++i) {
            inFlight.elementAt(i).draw(c);
        }
    }
}
