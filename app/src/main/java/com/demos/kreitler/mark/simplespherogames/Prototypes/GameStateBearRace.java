package com.demos.kreitler.mark.simplespherogames.Prototypes;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
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
public class GameStateBearRace extends GameStateBase implements IWidgetListener {
    // Interface ///////////////////////////////////////////////////////////////////////////////////
    // Static --------------------------------------------------------------------------------------
    private static float _difficultyScalar      = 0.f;

    // Instance ------------------------------------------------------------------------------------
    private final int FONT_SIZE                 = 33;
    private final int LIST_VERTICAL_SPACING     = 40;

    private final int SS_NONE                   = 0;
    private final int SS_AWAITING_THROW         = 1;
    private final int SS_IN_FLIGHT              = 2;

    private final int STATE_BLOCK_TIME          = 115;
    private final float THROW_THRESH            = 1.5f;
    private final int IMPACT_POWER_THRESH       = 100;
    private final int COLLISION_WAIT_TIME       = 67;
    private final int ANIM_TIME_MS              = 33;
    private final int STARTING_SPAWN_TIME_MS    = 500;
    private final int END_GAME_DELAY            = 3000;
    private final int BEAR_WALK_TIME_MS_MIN     = 10000;
    private final int BEAR_WALK_TIME_MS_MAX     = 5000;
    private final float TREE_POS_X_FACTOR       = 1.2f;
    private final int BEAR_FRAME_TIME_MS        = 250;
    private final int TREE_FALL_TIME            = 333;
    private final float BEAR_SQUASHED_SCALE     = 0.2f;
    private final int GROUND_DY                 = 4;
    private final float SAW_ANCHOR_X            = 0.2f;
    private final int CHOP_DOWN_CUTS            = 10;

    private WidgetList listOptions      = null;
    private WidgetBase lastTouched      = null;
    private String selectedOption       = null;
    private boolean bThrowingLeft       = true;
    private int stateBlockTimer         = 0;
    private boolean bCollisionOccurred  = false;
    private int collisionWaitTimer      = 0;

    private Sprite bear                 = null;
    private BitmapDrawable[] bearFrames = {null, null};
    private Sprite treeBottom           = null;
    private Sprite treeTop              = null;
    private Sprite saw                  = null;
    private int sawState                = 0;
    private float bearWalkTimerMS       = 0.0f;
    private int cuts                    = 0;
    private boolean bTreeFalling        = false;

    private int animTimerMS             = 0;

    private int subState            = SS_NONE;
    ConvenienceRobot robot          = null;
    Vector<WidgetLabel>options      = null;
    float accelX                    = 0.0f;
    float accelY                    = 0.0f;
    float accelZ                    = 0.0f;
    int animFrame                   = 0;

    float spawnTimerMS              = 0.0f;
    int bottomY                     = 0;
    int topY                        = 0;
    private int score               = 0;
    private boolean bGameOver       = false;
    private int endGameTimerMS      = 0;
    private float treePos           = 0.f;
    private int bearWalkTimeMS      = 0;
    private int treeFallTimerMS     = 0;
    private boolean bTreeFell       = false;
    private WidgetLabel titleLabel  = null;

    public GameStateBearRace(GameView.GameThread game) {
        super(game);

        Resources res = game.getContext().getResources();

        String titleText = res.getString(R.string.game_over);
        titleLabel = new WidgetLabel(null, 0, 0, titleText, FONT_SIZE, "black", "fonts/FindleyBold.ttf");

        topY = game.height() / 4;
        bottomY = game.height() * 3 / 4;

        bearFrames[0] = (BitmapDrawable)res.getDrawable(R.drawable.bear01, null);
        bearFrames[1] = (BitmapDrawable)res.getDrawable(R.drawable.bear02, null);

        bear = new Sprite(bearFrames[0], game.width(), game.height(), 1.0f, 1.f);
        treeBottom = new Sprite(res.getDrawable(R.drawable.tree_bottom, null), 0, 0, 0.5f, 1.f);
        treeTop = new Sprite(res.getDrawable(R.drawable.tree_top, null), 0, 0, 0.5f, 1.f);
        treePos = treeBottom.width() * TREE_POS_X_FACTOR;

        treeBottom.setPosition(treePos, game.height());
        treeTop.setPosition(treePos, game.height() - treeBottom.height());
        saw = new Sprite(res.getDrawable(R.drawable.saw, null), 0, 0, SAW_ANCHOR_X, 0.5f);

        options = new Vector<WidgetLabel>();
        String optionText = res.getString(R.string.player_won);
        options.add(new WidgetLabel(null, 0, 0, optionText, FONT_SIZE, "white", "fonts/FindleyBold.ttf"));

        listOptions = new WidgetList(null, 0.5f, LIST_VERTICAL_SPACING, titleLabel, options);
        listOptions.AddListener(this);

        listOptions.SetPosition(game.width() / 2, game.height() / 2);
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
        spawnTimerMS = STARTING_SPAWN_TIME_MS;
        score = 0;
        bGameOver = false;
        endGameTimerMS = END_GAME_DELAY;
        bearWalkTimeMS = Math.round((float)BEAR_WALK_TIME_MS_MIN * _difficultyScalar + (float)BEAR_WALK_TIME_MS_MAX *(1.0f - _difficultyScalar));
        bearWalkTimerMS = bearWalkTimeMS;
        treeFallTimerMS = TREE_FALL_TIME;
        bTreeFell = false;
        bTreeFalling = false;
        cuts = 0;
        bear.setScale(1.f, 1.f);
        setSawState(0);

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
            if (!bTreeFell) {
                c.drawARGB(255, 255, 0, 0);
            }
        }

        if (!bGameOver || bTreeFell) {
            c.drawARGB(255, 0, 32, 0);

            GameView._paint.setStyle(Paint.Style.FILL);
            GameView._paint.setARGB(255, 32, 32, 0);
            c.drawRect(0, game.height() - GROUND_DY, game.width(), game.height(), GameView._paint);

            drawTree(c);
            saw.draw(c);
            bear.draw(c);
        }

        if (bGameOver) {
            listOptions.Draw(c);
        }

        c.restore();
    }

    @Override
    public boolean Update(int dtMS) {
        boolean bResult = true;

        if (!bGameOver) {
            updateBear(dtMS);
            updateTree(dtMS);

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
            subState = SS_AWAITING_THROW;
            bThrowingLeft = !bThrowingLeft;
            stateBlockTimer = STATE_BLOCK_TIME;
            bCollisionOccurred = false;

            if (!bTreeFell) {
                setSawState(1 - sawState);
                ++cuts;
                if (!bTreeFalling && cuts >= CHOP_DOWN_CUTS) {
                    bTreeFalling = true;
                }
            }
        }

        return true;
    }

    private void SetDebugText(int resID) {
        WidgetLabel accelLabel = options.elementAt(0);
        Resources res = game.getContext().getResources();
        accelLabel.SetText(res.getString(resID));
    }

    private void endGame(boolean bPlayerWon) {
//        Resources res = game.getContext().getResources();
//        String optionText = res.getString(R.string.score) + score;
//        options.elementAt(0).SetText(optionText);

        Resources res = game.getContext().getResources();
        if (bPlayerWon) {
            titleLabel.SetColor("green");
            options.elementAt(0).SetText(res.getString(R.string.player_won));
        }
        else {
            options.elementAt(0).SetText(res.getString(R.string.player_lost));
            titleLabel.SetColor("black");
        }

        game.playSound(R.raw.destroy);
        bGameOver = true;
    }

    private void updateBear(int dtMS) {
        if (!bTreeFell) {
            bearWalkTimerMS -= dtMS;

            bearWalkTimerMS = Math.max(0.f, bearWalkTimerMS);

            if (bearWalkTimerMS == 0.f) {
                endGame(false);
            }

            float p = bearWalkTimerMS / (float) bearWalkTimeMS;
            bear.setPosition(game.width() * p + (treePos + treeBottom.width()) * (1.0f - p), bear.y());

            animFrame = Math.round(bearWalkTimerMS / BEAR_FRAME_TIME_MS) % 2;

            bear.setDrawable(bearFrames[animFrame]);
        }
    }

    private void updateTree(int dtMS) {
        if (bTreeFalling && !bTreeFell) {
            treeFallTimerMS -= dtMS;
            treeFallTimerMS = Math.max(treeFallTimerMS, 0);

            bTreeFell = treeFallTimerMS <= 0;

            if (bTreeFell) {
                bear.setScale(1.0f, BEAR_SQUASHED_SCALE);
                endGame(true);
            }
        }
    }

    private void drawTree(Canvas c) {
        c.save();

        treeBottom.draw(c);

        float treeX = treeTop.x();
        float treeY = treeTop.y();

        c.translate(treeX, treeY);
        treeTop.setPosition(0, 0);

        float p = 1.f - (float)treeFallTimerMS / (float)TREE_FALL_TIME;

        c.rotate(90.0f * p);

        treeTop.draw(c);
        c.restore();

        treeTop.setPosition(treeX, treeY);
    }

    private void setSawState(int state) {
        if (state == 0) {
            sawState = 0;
            saw.setAnchor(SAW_ANCHOR_X, 0.5f);
            saw.setPosition(treeBottom.x() - treeBottom.width() / 2, treeBottom.y() - treeBottom.height());
        }
        else {
            sawState = 1;
            saw.setAnchor(1.f - SAW_ANCHOR_X, 0.5f);
            saw.setPosition(treeBottom.x() + treeBottom.width() / 2, treeBottom.y() - treeBottom.height());
        }
    }
}
