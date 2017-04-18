package com.demos.kreitler.mark.simplespherogames.Prototypes;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.MotionEvent.PointerCoords;

import com.demos.kreitler.mark.demouilib.IWidgetListener;
import com.demos.kreitler.mark.demouilib.WidgetBase;
import com.demos.kreitler.mark.demouilib.WidgetLabel;
import com.demos.kreitler.mark.demouilib.WidgetList;
import com.demos.kreitler.mark.simplespherogames.GameStateBase;
import com.demos.kreitler.mark.simplespherogames.GameView;
import com.demos.kreitler.mark.simplespherogames.R;
import com.orbotix.ConvenienceRobot;
import com.orbotix.async.CollisionDetectedAsyncData;
import com.orbotix.async.DeviceSensorAsyncMessage;
import com.orbotix.common.sensor.SensorFlag;
import com.orbotix.subsystem.SensorControl;

import java.util.Vector;

import static android.content.Context.SENSOR_SERVICE;

/**
 * Created by Mark on 6/22/2016.
 */
public class GameStateSlingshotDrive extends GameStateBase implements IWidgetListener {
    // Interface ///////////////////////////////////////////////////////////////////////////////////
    // Static --------------------------------------------------------------------------------------
    // Instance ------------------------------------------------------------------------------------
    private final int FONT_SIZE                 = 33;
    private final int LIST_VERTICAL_SPACING     = 40;
    private final float COLLISION_THRESH        = 140.0f;
    private final float COLLISION_THRESH_LG     = 180.0f;
    private final float SPEED_SCALAR            = 1.0f / 10.0f;
    private final int CENTER_RADIUS             = 10;
    private final float EPSILON                 = 0.001f;
    private final float K                       = 4.0f; // Newtons per pixel?
    private final float SIM_MASS                = 5.0f; // kg.
    private final float DRAG                    = 1.67f;
    private final float DIST_SCALAR             = 0.000025f;

    private boolean bStarted                        = false;
    private boolean bSimulating                     = false;
    private int elapsedMS                           = 0;
    private float power                             = 0.0f;
    private float angle                             = 0.0f;
    private int drawDx                              = 0;
    private int drawDy                              = 0;
    private float vel                               = 0.0f;
    private MotionEvent.PointerCoords pointerCoords = new MotionEvent.PointerCoords();
    private boolean bOriented                       = false;

    ConvenienceRobot robot          = null;
    WidgetLabel titleLabel          = null;

    public GameStateSlingshotDrive(GameView.GameThread game) {
        super(game);

        Resources res = game.getContext().getResources();

        String titleText = res.getString(R.string.slingshot_title);
        titleLabel = new WidgetLabel(null, 0, 0, titleText, FONT_SIZE, "yellow", "fonts/FindleyBold.ttf");
        titleLabel.SetPosition(game.width() / 2, FONT_SIZE);
        titleLabel.AddListener(this);
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
//                | SensorFlag.ATTITUDE.longValue()
        ;

        robot.getSensorControl().enableSensors(sensorFlag, SensorControl.StreamingRate.STREAMING_RATE20);

        robot.setBackLedBrightness(1.0f);
        robot.enableStabilization(false);
        bStarted                = false;
        bSimulating             = false;
        bOriented               = false;
        robot.setLed(0.0f, 0.0f, 1.0f);
    }

    @Override
    public void Exit() {
        robot.stop();
        robot.getSensorControl().disableSensors();
        robot.enableStabilization(true);
        robot.setBackLedBrightness(0.0f);
    }

    @Override
    public void Draw(Canvas c) {
        c.save();

        GameView._paint.setStyle(Paint.Style.FILL);
        c.drawARGB(255, 0, 0, 0);

        GameView._paint.setStyle(Paint.Style.FILL);
        GameView._paint.setColor(Color.WHITE);
        c.drawArc(game.width() / 2 - CENTER_RADIUS, game.height() / 2 - CENTER_RADIUS,
                game.width() / 2 + CENTER_RADIUS, game.height() / 2 + CENTER_RADIUS,
                0, 360, true, GameView._paint);

        int radius = Math.max(Math.abs(drawDx), Math.abs(drawDy));

        if (bStarted && power * Math.min(game.width() / 2, game.height() / 2) > CENTER_RADIUS) {
            GameView._paint.setStyle(Paint.Style.STROKE);
            GameView._paint.setColor(Color.RED);

            c.drawArc(game.width() / 2 - radius, game.height() / 2 - radius,
                      game.width() / 2 + radius, game.height() / 2 + radius,
                      0, 360, true, GameView._paint);
        }

        titleLabel.Draw(c);

        c.restore();
    }

    @Override
    public boolean Update(int dtMS) {
        boolean bResult = true;

        if (bStarted) {
            UpdateDraw(dtMS);
        }
        else if (bSimulating) {
            UpdateSimulation(dtMS);
        }

        return bResult;
    }

    @Override
    public boolean OnTouch(View v, MotionEvent e) {
        if (!titleLabel.OnTouch(e) && e.getAction() == MotionEvent.ACTION_DOWN) {
            if (!bStarted && !bSimulating) {
                e.getPointerCoords(0, pointerCoords);

                if (!bOriented) {
                    robot.setZeroHeading();
                    bOriented = true;
                }

                int limit = Math.min(game.width() / 2, game.height() / 2);

                if (Math.abs(pointerCoords.x - limit) >= CENTER_RADIUS ||
                    Math.abs(pointerCoords.y - limit) >= CENTER_RADIUS) {
                    bStarted = true;
                    elapsedMS = 0;
                    power = 0.0f;
                    angle = 0.0f;
                    drawDx = 0;
                    drawDy = 0;
                }
            }
        }
        else if (bStarted && e.getAction() == MotionEvent.ACTION_MOVE) {
            e.getPointerCoords(0, pointerCoords);
            int localX = Math.round(pointerCoords.x);
            int localY = Math.round(pointerCoords.y);

            int dx = localX - game.width() / 2;
            int dy = localY - game.height() / 2;
            int limit = Math.min(game.width() / 2, game.height() / 2);
            int dxSign = dx > 0 ? 1 : -1;
            int dySign = dy > 0 ? 1 : -1;

            dx = dxSign * Math.min(Math.abs(dx), limit);
            dy = dySign * Math.min(Math.abs(dy), limit);

            drawDx = dx;
            drawDy = dy;

            power = (float)Math.sqrt(dx * dx + dy * dy);
            angle =  90.0f - (float)(Math.atan2(dy, -dx) * 180.0 / Math.PI);
            while (angle < 0.0f) {
                angle += 360.0f;
            }
            while (angle > 360.0f) {
                angle -= 360.0f;
            }
        }
        else if (bStarted && (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_BUTTON_RELEASE || e.getAction() == MotionEvent.ACTION_CANCEL)) {
            if (bStarted) {
                robot.setBackLedBrightness(0.0f);
                robot.enableStabilization(true);
                vel = 0.0f;
                bStarted = false;
                bSimulating = true;
                robot.setLed(0.f, 1.f, 0.f);
            }
        }

        return true;
    }

    @Override
    public void HandleSensorData(DeviceSensorAsyncMessage message) {
    }

    public void HandleCollisionMessage(CollisionDetectedAsyncData colData) {
        int impactX = colData.getImpactPower().x;
        int impactY = colData.getImpactPower().y;
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
        }
    }

    // IWidgetListener -----------------------------------------------------------------------------
    public boolean OnWidgetTouchStart(WidgetBase widget, int localX, int localY) {
        return true;
    }

    public boolean OnWidgetTouchEnd(WidgetBase widget, int localX, int localY) {
        game.setState("GameStateMainMenu");
        return true;
    }

    public boolean OnWidgetDrag(WidgetBase widget, int localX, int localY) {
        return true;
    }

    public boolean OnWidgetTouchCancel(WidgetBase widget, int localX, int localY) {
        return OnWidgetTouchEnd(widget, localX, localY);
    }

    // Implementation //////////////////////////////////////////////////////////////////////////////
    // Static --------------------------------------------------------------------------------------
    // Instance ------------------------------------------------------------------------------------
    public void UpdateDraw(int dtMS) {
        robot.drive(angle, 0);
    }

    public void UpdateSimulation(int dtMS) {
        float dt = (float) dtMS * 0.001f;

        if (power > 0.0f) {
            float radius = Math.min(game.width() / 2, game.height() / 2) * DIST_SCALAR;
            float r0 = radius * power;
            float acc = r0 * K / SIM_MASS;
            float dv = acc * dt;
            float vAve = (vel + dv) * 0.5f;
            float dR = vAve * dt;

            if (dR > r0) {
                dR = r0;
            }

            vel += dv;
            power = (r0 - dR) / radius;
            if (power < EPSILON) {
                power = 0.0f;
                robot.setLed(1.0f, 0.0f, 0.0f);
            }

            Log.d("Sphero", ">>> Power: " + power + "   vel: " + vel);
        }
        else {
            float dv = vel * DRAG * dt;
            vel -= dv;

            Log.d("Sphero", "<<< vel: " + vel + "   dv: " + dv);

            if (vel <= EPSILON) {
                bSimulating = false;
                robot.setBackLedBrightness(1.0f);
                robot.enableStabilization(false);
                vel = 0.f;
                robot.drive(angle, 0);
                robot.setLed(0.0f, 0.0f, 1.0f);
            }
        }

        if (bSimulating) {
            robot.drive(angle, vel);
        }
    }
}
