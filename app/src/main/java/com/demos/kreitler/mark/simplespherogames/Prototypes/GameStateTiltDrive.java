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
import android.view.MotionEvent.PointerCoords;
import android.view.View;

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
import com.orbotix.common.sensor.DeviceSensorsData;
import com.orbotix.common.sensor.SensorFlag;
import com.orbotix.subsystem.SensorControl;

import java.util.Vector;

import static android.content.Context.SENSOR_SERVICE;

/**
 * Created by Mark on 6/22/2016.
 */
public class GameStateTiltDrive extends GameStateBase implements IWidgetListener {
    // Interface ///////////////////////////////////////////////////////////////////////////////////
    // Static --------------------------------------------------------------------------------------
    // Instance ------------------------------------------------------------------------------------
    private final int FONT_SIZE                 = 33;
    private final int LIST_VERTICAL_SPACING     = 40;
    private final float COLLISION_THRESH        = 140.0f;
    private final float COLLISION_THRESH_LG     = 180.0f;
    private final float SPEED_SCALAR            = 1.0f / 10.0f;

    private WidgetList listOptions      = null;
    private boolean bStarted            = false;
    private SensorManager sensorManager = null;
    double ax                           = 0.0;
    double ay                           = 0.0;
    double az                           = 0.0;
    int elapsedMS                       = 0;
    int colSmall                        = 0;
    int colMid                          = 0;
    int colLarge                        = 0;

    ConvenienceRobot robot          = null;
    Vector<WidgetLabel>options      = null;

    public GameStateTiltDrive(GameView.GameThread game) {
        super(game);

        Resources res = game.getContext().getResources();

        sensorManager=(SensorManager) game.getContext().getSystemService(SENSOR_SERVICE);

        String titleText = res.getString(R.string.tilt_to_drive_title);
        WidgetLabel titleLabel = new WidgetLabel(null, 0, 0, titleText, FONT_SIZE, "yellow", "fonts/FindleyBold.ttf");

        options = new Vector<WidgetLabel>();
        String optionText = res.getString(R.string.tilt_to_drive_data);
        options.add(new WidgetLabel(null, 0, 0, optionText, FONT_SIZE, "white", "fonts/FindleyBold.ttf"));

        optionText = res.getString(R.string.tilt_to_drive_col_sm);
        options.add(new WidgetLabel(null, 0, 0, optionText, FONT_SIZE, "white", "fonts/FindleyBold.ttf"));

        optionText = res.getString(R.string.tilt_to_drive_col_md);
        options.add(new WidgetLabel(null, 0, 0, optionText, FONT_SIZE, "white", "fonts/FindleyBold.ttf"));

        optionText = res.getString(R.string.tilt_to_drive_col_lg);
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

        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);

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
        robot                   = game.getRobot();

        assert(robot != null);
    }

    @Override
    public void Exit() {
        robot.stop();
        robot.getSensorControl().disableSensors();
        robot.enableStabilization(true);
        robot.setBackLedBrightness(0.0f);
        sensorManager.unregisterListener(this);
    }

    @Override
    public void Draw(Canvas c) {
        c.save();

        GameView._paint.setStyle(Paint.Style.FILL);
        c.drawARGB(255, 0, 0, 0);
        listOptions.Draw(c);

        c.restore();
    }

    @Override
    public boolean Update(int dtMS) {
        int currentMS = 0;
        boolean bResult = true;

        if (bStarted) {
            float heading = (float)Math.atan2(ay, -ax);
            float speed = (float)Math.sqrt(ax * ax + ay * ay);

            heading = heading * 180.0f / (float)Math.PI;

            while (heading < 0.0f) {
                heading += 360.0f;
            }
            while (heading > 360.0f) {
                heading -= 360.0f;
            }

            elapsedMS += dtMS;
            currentMS = elapsedMS;

            SetTimeText(currentMS, options.elementAt(0));

            robot.drive(heading, speed * SPEED_SCALAR);

            SetDebugText(1, R.string.tilt_to_drive_col_sm, "" + colSmall);
            SetDebugText(2, R.string.tilt_to_drive_col_md, "" + colMid);
            SetDebugText(3, R.string.tilt_to_drive_col_lg, "" + colLarge);
        }
        else {
            SetDebugText(0, R.string.tilt_to_drive_data, "" + ax + "   " + ay + "   " + az);
        }

        return bResult;
    }

    @Override
    public boolean OnTouch(View v, MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            if (!bStarted) {
                robot.setZeroHeading();
                bStarted = true;
                robot.setBackLedBrightness(0.0f);
                robot.enableStabilization(true);
                elapsedMS = 0;
                colSmall = 0;
                colMid = 0;
                colLarge = 0;
            }
            else {
                game.setState("GameStateMainMenu");
            }
        }
        else if (bStarted && e.getAction() == MotionEvent.ACTION_MOVE) {
        }
        else if (bStarted && (e.getAction() == MotionEvent.ACTION_BUTTON_RELEASE || e.getAction() == MotionEvent.ACTION_CANCEL)) {
        }

        return true;
    }

    @Override
    public void HandleSensorData(DeviceSensorAsyncMessage message) {
    }

    public void HandleCollisionMessage(CollisionDetectedAsyncData colData) {
        int impactX = colData.getImpactPower().x;
        int impactY = colData.getImpactPower().y;

        if (impactX * impactX + impactY * impactY > COLLISION_THRESH_LG * COLLISION_THRESH_LG) {
            // Large collision.
            colLarge += 1;
        }
        else if (impactX * impactX + impactY * impactY > COLLISION_THRESH * COLLISION_THRESH) {
            // Medium collision.
            colMid += 1;
        }
        else {
            // Small collision.
            colSmall += 1;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
            ax=event.values[0];
            ay=event.values[1];
            az=event.values[2];
        }
    }

    // IWidgetListener -----------------------------------------------------------------------------
    public boolean OnWidgetTouchStart(WidgetBase widget, int localX, int localY) {
        return true;
    }

    public boolean OnWidgetTouchEnd(WidgetBase widget, int localX, int localY) {
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
    private void DebugSensorData() {
        WidgetLabel accelLabel = options.elementAt(0);
        assert(accelLabel != null);

        Resources res = game.getContext().getResources();
        accelLabel.SetText("");
    }

    private void SetDebugText(int index, int resID, String strData) {
        WidgetLabel accelLabel = options.elementAt(index);
        Resources res = game.getContext().getResources();
        accelLabel.SetText(res.getString(resID) + ": " + strData);
    }
}
