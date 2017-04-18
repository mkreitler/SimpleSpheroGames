package com.demos.kreitler.mark.simplespherogames;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.MotionEvent;
import android.view.View;

import com.demos.kreitler.mark.demouilib.WidgetLabel;
import com.orbotix.async.CollisionDetectedAsyncData;
import com.orbotix.async.DeviceSensorAsyncMessage;
import com.orbotix.common.sensor.DeviceSensorsData;
import com.orbotix.common.sensor.LocatorData;

import java.util.List;

/**
 * Created by Mark on 6/15/2016.
 */
public class GameStateBase implements SensorEventListener {
    // Interface ///////////////////////////////////////////////////////////////////////////////////
    // Static --------------------------------------------------------------------------------------

    // Instance ------------------------------------------------------------------------------------
    public GameStateBase(GameView.GameThread gameThread) {
        game = gameThread;
    }

    public void Enter(GameView.GameThread game) {}
    public boolean Update(int dtMS) {return true;}
    public void Exit() {}
    public void Draw(Canvas c) {}
    public void OnPause() {}
    public void OnResume() {}

    public void HandleCollisionMessage(CollisionDetectedAsyncData colData) {}
    public void HandleSensorData(DeviceSensorAsyncMessage sensorData) {}

    public void onAccuracyChanged(Sensor arg0, int arg1) {}
    public void onSensorChanged(SensorEvent event) {}

    public boolean OnTouch(View v, MotionEvent e) {
        return false;
    }

    protected void SetTimeText(int timeMS, WidgetLabel timeLabel) {
        int hundredthSeconds;
        int tenthSeconds = 0;
        int seconds = 0;
        int tenSeconds = 0;
        int minutes = 0;
        int tenMinutes = 0;

        tenMinutes = timeMS / (1000 * 600);
        timeMS -= tenMinutes * 1000 * 600;

        minutes = timeMS / (1000 * 60);
        timeMS -= minutes * 1000 * 60;

        tenSeconds = timeMS / 10000;
        timeMS -= tenSeconds * 10000;

        seconds = timeMS / 1000;
        timeMS -= seconds * 1000;

        tenthSeconds = timeMS / 100;
        timeMS -= tenthSeconds * 100;

        hundredthSeconds = timeMS / 10;

        assert(timeLabel != null);

        timeLabel.SetText(tenMinutes + "" + minutes + ":" + tenSeconds + "" + seconds + "." + tenthSeconds + "" + hundredthSeconds);
    }

    // Implementation //////////////////////////////////////////////////////////////////////////////
    // Static --------------------------------------------------------------------------------------
    protected static Rect _bounds = new Rect();

    // Instance ------------------------------------------------------------------------------------
    protected GameView.GameThread game = null;
    protected Rect GetTextSize(String text) {
        text = text.replace(' ', 'w');
        GameView._paint.getTextBounds(text, 0, text.length(), _bounds);
        return _bounds;
    }

    protected void DrawTextCentered(Canvas c, String text) {
        GameView._paint.getTextBounds(text, 0, text.length(), _bounds);

        int x = (c.getWidth() / 2) - (_bounds.width() / 2);
        int y = (c.getHeight() / 2) - (_bounds.height() / 2) - 12;
        c.drawText(text, x, y + GameView._paint.getTextSize() / 2, GameView._paint);
    }

    protected void DrawTextCenteredAtY(Canvas c, String text, int y) {
        GameView._paint.getTextBounds(text, 0, text.length(), _bounds);

        int x = (c.getWidth() / 2) - (_bounds.width() / 2);
        c.drawText(text, x, y + GameView._paint.getTextSize() / 2, GameView._paint);
    }
}
