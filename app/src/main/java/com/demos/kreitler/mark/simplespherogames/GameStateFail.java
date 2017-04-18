package com.demos.kreitler.mark.simplespherogames;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

import com.demos.kreitler.mark.simplespherogames.GameView;
import com.demos.kreitler.mark.simplespherogames.MainActivity;

/**
 * Created by Mark on 6/16/2016.
 */
public class GameStateFail extends GameStateBase {
    private String failMessage = null;

    public GameStateFail(GameView.GameThread gameThread) {
        super(gameThread);

        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        if (bluetooth != null && bluetooth.isDiscovering()) {
            bluetooth.cancelDiscovery();
        }
    }

    @Override
    public void Draw(Canvas c) {
        c.drawARGB(255, 0, 0, 0);

        if (failMessage != null && failMessage.length() > 0) {
            GameView._paint.setStyle(Paint.Style.FILL);
            GameView._paint.setTextSize(25);
            GameView._paint.setColor(Color.RED);

            DrawTextCentered(c, "ERROR: " + failMessage);
        }
    }

    public void SetMessage(String message) {
        failMessage = message;
    }

    public boolean onTouch(View v, MotionEvent e) {
        boolean bHandled = false;

        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            MainActivity._finish();
            bHandled = true;
        }

        return bHandled;
    }
}
