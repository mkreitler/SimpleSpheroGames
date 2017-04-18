package com.demos.kreitler.mark.simplespherogames.Prototypes;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
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
import com.orbotix.common.sensor.DeviceSensorsData;
import com.orbotix.common.sensor.SensorFlag;
import com.orbotix.subsystem.SensorControl;

import java.util.Vector;

/**
 * Created by Mark on 6/22/2016.
 */
public class GameStateGyroCopter extends GameStateBase implements IWidgetListener {
    // Interface ///////////////////////////////////////////////////////////////////////////////////
    // Static --------------------------------------------------------------------------------------
    // Instance ------------------------------------------------------------------------------------
    private final int FONT_SIZE                 = 33;
    private final int LIST_VERTICAL_SPACING     = 40;
    private final float BLEND_RATE              = -0.075f;
    private final float COPTER_SCALE            = 0.25f;
    private final int ANIM_FRAME_RATE           = 30;
    private final float GYRO_VAL_TO_PERCENT     = 0.005f;
    private final int MAX_GYRO_VAL              = 80;
    private final float TERRAIN_SPEED_MIN       = 150.0f / 1000.0f;
    private final float TERRAIN_SPEED_MAX       = 600.0f / 1000.0f;
    private final float DESCENT_INTERP_RATE     = 2.0f;
    private final float ADVANCE_INTERP_TIME     = 30000;
    private final int GAME_OVER_DELAY           = 3000;

    private String selectedOption       = null;

    private WidgetList listOptions      = null;
    private WidgetBase lastTouched      = null;

    private ConvenienceRobot robot      = null;
    private Vector<WidgetLabel>options  = null;
    private Sprite copter               = null;
    private Drawable[] copterFrame      = {null, null};
    float gyroX                         = 0.0f;
    float gyroY                         = 0.0f;
    float gyroZ                         = 0.0f;
    int ceiling                         = 0;
    int floor                           = 0;
    int elapsedAnimTime                 = 0;
    int copterHeight                    = 0;
    int currentFrame                    = 0;

    private Sprite[] canopy             = {null, null, null, null, null, null};
    private Sprite[] ground             = {null, null, null, null, null, null};
    private float terrainSpeed          = TERRAIN_SPEED_MIN;
    private boolean bGameOver           = false;
    private int gameOverTimerMS         = 0;
    private float advanceInterp         = 0.f;
    private int score                   = 0;

    public GameStateGyroCopter(GameView.GameThread game) {
        super(game);

        Resources res = game.getContext().getResources();

        for (int i=0; i<canopy.length; ++i) {
            canopy[i] = new Sprite(res.getDrawable(R.drawable.canopy, null), game.width(), 0, 0.0f, 0.0f);
            canopy[i].setPosition(game.width() + canopy[i].width() * i, 0.0f);
        }

        for (int i=0; i<ground.length; ++i) {
            ground[i] = new Sprite(res.getDrawable(R.drawable.stones, null), game.width(), game.height(), 0.0f, 1.0f);
            ground[i].setPosition(game.width() + ground[i].width() * i, game.height());
        }

        String titleText = res.getString(R.string.gyro_copter_title);
        WidgetLabel titleLabel = new WidgetLabel(null, 0, 0, titleText, FONT_SIZE, "yellow", "fonts/FindleyBold.ttf");

        copterFrame[0] = res.getDrawable(R.drawable.copter_01, null);
        copterFrame[1] = res.getDrawable(R.drawable.copter_02, null);

        copter = new Sprite(copterFrame[0], 0, 0, 0.0f, 0.5f);
        copter.setScale(COPTER_SCALE, COPTER_SCALE);

        options = new Vector<WidgetLabel>();
        String optionText = res.getString(R.string.score) + " 0";
        options.add(new WidgetLabel(null, 0, 0, optionText, FONT_SIZE, "white", "fonts/FindleyBold.ttf"));

        listOptions = new WidgetList(null, 0.5f, LIST_VERTICAL_SPACING, titleLabel, options);
        listOptions.AddListener(this);

        listOptions.SetPosition(game.width() / 2, game.height() / 2);

        floor = game.height() - Math.round(copter.height() * COPTER_SCALE * 0.5f);
        ceiling = Math.round(copter.height() * COPTER_SCALE * 0.5f);
    }

    @Override
    public void Enter(GameView.GameThread game) {
        super.Enter(game);

        robot = game.getRobot();
        assert(robot != null);

        long sensorFlag = SensorFlag.QUATERNION.longValue()
//                | SensorFlag.ACCELEROMETER_NORMALIZED.longValue()
                | SensorFlag.GYRO_NORMALIZED.longValue()
//                | SensorFlag.MOTOR_BACKEMF_NORMALIZED.longValue()
//                | SensorFlag.ATTITUDE.longValue();
        ;

        gyroZ = 0.0f;

        robot.getSensorControl().enableSensors(sensorFlag, SensorControl.StreamingRate.STREAMING_RATE20);
        robot.enableStabilization(false);
        robot.setBackLedBrightness(1.0f);
        elapsedAnimTime = 0;
        copterHeight = currentFrame = 0;
        terrainSpeed = TERRAIN_SPEED_MIN;
        bGameOver = false;
        gameOverTimerMS = 0;
        advanceInterp = 0.f;
        score = 0;

        for (int i=0; i<canopy.length; ++i) {
            canopy[i].setPosition(game.width() + canopy[i].width() * i, 0.0f);
        }

        for (int i=0; i<ground.length; ++i) {
            ground[i].setPosition(game.width() + ground[i].width() * i, game.height());
        }

        for (int i=0; i<ground.length; ++i) {
            if (Math.random() < 0.5f) {
                canopy[i].enable();
                ground[i].disable();
            }
            else {
                canopy[i].disable();
                ground[i].enable();
            }
        }

        lastTouched = null;
    }

    @Override
    public void Exit() {
        robot.getSensorControl().disableSensors();
        robot.enableStabilization(true);
        robot.setBackLedBrightness(0.0f);
    }

    @Override
    public void Draw(Canvas c) {
        c.save();

        if (!bGameOver) {
            c.drawARGB(255, 0, 32, 0);
            drawTerrain(c);
            drawCopter(c);
        }
        else {
            c.drawARGB(255, 255, 0, 0);
            listOptions.Draw(c);
        }

        c.restore();
    }

    @Override
    public boolean Update(int dtMS) {
        boolean bResult = true;

        if (!bGameOver) {
            updateCopter(dtMS);
            updateTerrain(dtMS);
            checkCollisions(dtMS);
        }
        else {
            gameOverTimerMS += dtMS;

            if (gameOverTimerMS > GAME_OVER_DELAY) {
                game.setState("GameStateMainMenu");
            }
        }

        return bResult;
    }

    @Override
    public boolean OnTouch(View v, MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            game.setState("GameStateMainMenu");
        }

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
                gyroX = (float)data.getGyroData().getRotationRateFiltered().x;
                gyroY = (float)data.getGyroData().getRotationRateFiltered().y;
                gyroZ = (float)data.getGyroData().getRotationRateFiltered().z;
            }
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
        WidgetLabel omegaLabel = options.elementAt(0);
        assert(omegaLabel != null);

        Resources res = game.getContext().getResources();
        omegaLabel.SetText(res.getString(R.string.gyro_debug_omegaZ) + Math.round(gyroZ * GYRO_VAL_TO_PERCENT));
    }

    private void SetDebugText(int resID) {
        WidgetLabel accelLabel = options.elementAt(0);
        Resources res = game.getContext().getResources();
        accelLabel.SetText(res.getString(resID));
    }

    private float blendToDest(float current, float target, float dt) {
        float delta = target - current;
        float newDelta = delta * (float)Math.exp(-dt * BLEND_RATE);

        return current + newDelta;
    }

    private void updateCopter(int dtMS) {
        float boundGyroVal = Math.abs(gyroZ)* GYRO_VAL_TO_PERCENT;
        boundGyroVal = Math.min(boundGyroVal, MAX_GYRO_VAL);

        int oldFrame = 0;
        int newFrame = 0;

        float heightParam = Math.round(Math.abs(boundGyroVal));
        int height = (int)(heightParam * (ceiling - floor) / MAX_GYRO_VAL) + floor;

        float interp = (float)dtMS * DESCENT_INTERP_RATE * 0.001f;
        interp = (float)Math.max(0.f, Math.min(1.0, interp));
        height = Math.round(copter.y() * (1.f - interp) + height * interp);

        advanceInterp += dtMS / ADVANCE_INTERP_TIME;
        advanceInterp = Math.min(1.f, Math.max(advanceInterp, 0.f));
        float left = copter.width() * 1.1f * COPTER_SCALE;
        float right = game.width() - copter.width() * 2.f * COPTER_SCALE;

        copter.setPosition(Math.round(left * (1.f - advanceInterp) + right * advanceInterp), height);

        oldFrame = elapsedAnimTime * ANIM_FRAME_RATE / 1000;
        elapsedAnimTime += dtMS;
        newFrame = elapsedAnimTime * ANIM_FRAME_RATE / 1000;

        if ((newFrame != oldFrame) && ((newFrame - oldFrame) % 2 == 1)) {
            currentFrame = 1 - currentFrame;
        }

        copter.setDrawable(copterFrame[currentFrame]);
    }

    private void updateTerrain(int dtMS) {
        float dx = -terrainSpeed * dtMS;

        for (int i=0; i<ground.length; ++i) {
            ground[i].move(dx, 0.0f);

            if (ground[i].x() <= -ground[i].width()) {
                ground[i].move(ground[i].width() * ground.length, 0.f);

                if (Math.random() < Math.sqrt(advanceInterp)) {
                    if (Math.random() < 0.5f) {
                        ground[i].enable();
                        canopy[i].disable();
                    }
                    else {
                        ground[i].disable();
                        canopy[i].enable();
                    }
                }
                else {
                    ground[i].enable();
                    canopy[i].enable();
                }
            }
        }

        for (int i=0; i<canopy.length; ++i) {
            canopy[i].move(dx, 0.0f);

            if (canopy[i].x() <= -canopy[i].width()) {
                canopy[i].move(canopy[i].width() * canopy.length, 0.f);
            }
        }
    }

    private void drawCopter(Canvas c) {
        copter.draw(c);
    }

    private void drawTerrain(Canvas c) {
        for (int i=0; i<canopy.length; ++i) {
            canopy[i].draw(c);
        }

        for (int i=0; i<ground.length; ++i) {
            ground[i].draw(c);
        }
    }

    private void checkCollisions(int dtMS) {
        float copterDx = copter.width() * COPTER_SCALE;
        float copterDy = copter.height() * COPTER_SCALE;
        float copterX = copter.x() - copterDx * 0.5f;
        float copterY = copter.y() - copterDy * 0.5f;
        boolean bCollision = false;

        // Since copter is smaller than terrain tiles, we check
        // to see if any edge of the copter is within any edge
        // of the terrain.
        for (int i=0; !bCollision && i<canopy.length; ++i) {
            if (canopy[i].isEnabled()) {
                float cX1 = canopy[i].x();
                float cX2 = cX1 + canopy[i].width();
                float cY1 = canopy[i].y();
                float cY2 = cY1 + canopy[i].height();

                boolean bXin = (copterX >= cX1 && copterX <= cX2) || (copterX + copterDx >= cX1 && copterX + copterDx <= cX2);
                boolean bYin = (copterY >= cY1 && copterY <= cY2) || (copterY + copterDy >= cY1 && copterY + copterDy <= cY2);

                bCollision = bXin && bYin;
            }
        }

        for (int i=0; !bCollision && i<ground.length; ++i) {
            if (ground[i].isEnabled()) {
                float cX1 = ground[i].x();
                float cX2 = cX1 + ground[i].width();
                float cY1 = ground[i].y() - ground[i].height();
                float cY2 = cY1 + ground[i].height();

                boolean bXin = (copterX >= cX1 && copterX <= cX2) || (copterX + copterDx >= cX1 && copterX + copterDx <= cX2);
                boolean bYin = (copterY >= cY1 && copterY <= cY2) || (copterY + copterDy >= cY1 && copterY + copterDy <= cY2);

                bCollision = bXin && bYin;
            }
        }

        if (bCollision) {
            endGame();
        }
        else {
            score += dtMS;

            Resources res = game.getContext().getResources();
            options.elementAt(0).SetText(res.getString(R.string.score) + " " + score);
        }
    }

    private void endGame() {
        bGameOver = true;
        gameOverTimerMS = 0;
    }
}
