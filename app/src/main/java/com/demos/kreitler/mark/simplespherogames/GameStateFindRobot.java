package com.demos.kreitler.mark.simplespherogames;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;

import com.orbotix.DualStackDiscoveryAgent;
import com.orbotix.le.DiscoveryAgentLE;
import com.orbotix.common.RobotChangedStateListener;
import com.orbotix.le.RobotRadioDescriptor;

/**
 * Created by Mark on 6/24/2016.
 */
public class GameStateFindRobot extends GameStateBase {
    private class STAGE {
        public static final int SEARCHING   = 0;
        public static final int CONNECTING  = 1;
        public static final int CONNECTED   = 2;
        public static final int ONLINE      = 3;
    }

    private final int FONT_SIZE         = 50;
    private final int TIMEOUT_SEC       = 60;

    private long startTime              = 0;
    private Context appContext          = null;
    private int stage                   = STAGE.SEARCHING;

    public GameStateFindRobot(GameView.GameThread game, Context context) {
        super(game);

        appContext = context;

        DiscoveryAgentLE.getInstance().addRobotStateListener(game);
    }

    public void Connecting() {
        stage = STAGE.CONNECTING;
    }

    public void Connected() {
        stage = STAGE.CONNECTED;
    }

    public void Online() {
        stage = STAGE.ONLINE;
    }

    @Override
    public void Enter(GameView.GameThread game) {
        super.Enter(game);

        OnResume();
    }

    @Override
    public void Exit() {
        OnPause();
    }

    @Override
    public void OnPause() {
        if( DiscoveryAgentLE.getInstance().isDiscovering() ) {
            DiscoveryAgentLE.getInstance().stopDiscovery();
        }
    }

    @Override
    public void OnResume() {
        startTime = java.lang.System.currentTimeMillis();

        // Begin robot discovery.
        if( !DiscoveryAgentLE.getInstance().isDiscovering() ) {
            try {
                RobotRadioDescriptor robotRadioDescriptor = new RobotRadioDescriptor();
                robotRadioDescriptor.setNamePrefixes(new String[]{"SK-"});
                DiscoveryAgentLE.getInstance().setRadioDescriptor(robotRadioDescriptor);
                DiscoveryAgentLE.getInstance().startDiscovery(appContext);
            }
            catch (Exception e) {
                game.Fail("Failed to start discovery.");
            }
        }
    }

    @Override
    public boolean Update(int dtMS) {
        long curTime = java.lang.System.currentTimeMillis();

        if ((int)(curTime - startTime) /  1000 > TIMEOUT_SEC) {
            game.setState("gameStateIntro");
        }

        return false;
    }

    public void Draw(Canvas c) {
        long curTime = java.lang.System.currentTimeMillis();
        boolean bShowElapsedTime = true;

        c.save();

        Resources res = game.GetResources();
        GameView._paint.setTextSize(FONT_SIZE);
        GameView._paint.setTypeface(GameView._font);

        c.drawARGB(255, 0, 0, 0);

        int w = c.getWidth();
        int h = c.getHeight();

        GameView._paint.setStyle(Paint.Style.FILL);
        GameView._paint.setColor(Color.WHITE);

        int margin = 3 * FONT_SIZE / 4;
        switch(stage) {
            case STAGE.SEARCHING: {
                c.drawText(res.getString(R.string.search_stage_01), margin, margin, GameView._paint);
                break;
            }

            case STAGE.CONNECTING: {
                c.drawText(res.getString(R.string.search_stage_02), margin, margin, GameView._paint);
                break;
            }

            case STAGE.CONNECTED: {
                c.drawText(res.getString(R.string.search_stage_03), margin, margin, GameView._paint);
                break;
            }

            case STAGE.ONLINE: {
                GameView._paint.setColor(Color.YELLOW);
                DrawTextCentered(c, res.getString(R.string.tap_to_play));
                bShowElapsedTime = false;
                break;
            }
        }

        if (bShowElapsedTime) {
            c.drawText(res.getString(R.string.stage_time), margin, margin + FONT_SIZE, GameView._paint);

            String dtSec = " " + (int)(curTime - startTime) / 1000;
            Rect textBounds = GetTextSize(res.getString(R.string.stage_time) + " ");
            c.drawText(dtSec, margin + textBounds.width(), margin + FONT_SIZE, GameView._paint);
            c.drawText(res.getString(R.string.seconds),
                       margin + textBounds.width() + GetTextSize(dtSec).width(),
                       margin + FONT_SIZE,
                       GameView._paint);
        }

        c.restore();
    }

    public boolean OnTouch(View v, MotionEvent e) {
        boolean bHandled = false;

        switch(e.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                if (stage == STAGE.ONLINE) {
                    game.setState("GameStateMainMenu");
                    bHandled = true;
                }
                break;
            }
        }

        return bHandled;
    }
}
