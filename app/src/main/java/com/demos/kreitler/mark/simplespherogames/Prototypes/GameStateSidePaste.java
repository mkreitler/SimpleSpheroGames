package com.demos.kreitler.mark.simplespherogames.Prototypes;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;

import com.demos.kreitler.mark.demouilib.ITransitionListener;
import com.demos.kreitler.mark.demouilib.IWidgetListener;
import com.demos.kreitler.mark.demouilib.TransitionTrig;
import com.demos.kreitler.mark.demouilib.WidgetBase;
import com.demos.kreitler.mark.demouilib.WidgetLabel;
import com.demos.kreitler.mark.lib_demo_sprites.Sprite;
import com.demos.kreitler.mark.simplespherogames.GameStateBase;
import com.demos.kreitler.mark.simplespherogames.GameView;
import com.demos.kreitler.mark.simplespherogames.R;
import com.orbotix.ConvenienceRobot;

/**
 * Created by Mark on 6/28/2016.
 */
public class GameStateSidePaste extends GameStateBase implements ITransitionListener, IWidgetListener {
    // Interface ///////////////////////////////////////////////////////////////////////////////////
    // Static --------------------------------------------------------------------------------------
    // Instance ------------------------------------------------------------------------------------
    private ConvenienceRobot robot = null;

    public GameStateSidePaste(GameView.GameThread game) {
        super(game);

        transition = new TransitionTrig(TRANSITION_DURATION);

        Resources res = game.getContext().getResources();

        infoSprite = new Sprite(res.getDrawable(R.drawable.intro_side_toss, null), 0, 0, 0.0f, 0.5f);
        float scaleX = (float)game.width() / (float)infoSprite.width();
        float scaleY = (float)game.height() / (float)infoSprite.height();
        float scale  = Math.min(scaleX, scaleY);
        infoSprite.setScale(scale, scale);

        String optionText = res.getString(R.string.main_action_start);
        startAction = new WidgetLabel(null, 0, 0, 0.0f, 0.0f, optionText, FONT_SIZE, "white", "fonts/FindleyBold.ttf");
        startAction.AddListener(this);
        startAction.SetPosition(game.width() - startAction.GetWorldBounds().width() * 11 / 10,
                                game.height() - startAction.GetWorldBounds().height() * 11 / 10);
        startAction.Hide();

        transition.AddListener(this);
    }

    @Override
    public void Enter(GameView.GameThread game) {
        phase = PHASE_INTRO;
        startAction.Hide();
        transition.Reset();
        transition.Start();

        robot = game.getRobot();
        assert(robot != null);

        // robot.setBackLedBrightness(1.0f);
    }

    @Override
    public boolean Update(int dtMS) {
        transition.Update();

        return false;
    }

    public void Draw(Canvas c) {
        c.drawARGB(255, 0, 0, 0);

        switch(phase) {
            case PHASE_INTRO:{
                int x = Math.round(c.getWidth() * (1.0f - transParam));
                int y = c.getHeight() / 2;
                infoSprite.setPosition(x, y);
                infoSprite.draw(c);
                break;
            }

            case PHASE_INPUT: {
                infoSprite.setPosition(0, c.getHeight() * 0.5f);
                infoSprite.draw(c);
                startAction.Draw(c);
                break;
            }

            case PHASE_OUTRO: {
                int x = Math.round(c.getWidth() * transParam);
                int y = c.getHeight() / 2;
                infoSprite.setPosition(x, y);
                infoSprite.draw(c);
                break;
            }

            default: {
                break;
            }
        }
    }

    @Override
    public boolean OnTouch(View v, MotionEvent e) {
        return startAction.OnTouch(e);
    }

    // ITransitionListener -------------------------------------------------------------------------
    public void OnTransitionStart(float value) {
        transParam = value;
    }

    public void OnTransitionEnd(float value) {
        transParam = value;

        if (phase == PHASE_INTRO) {
            phase = PHASE_INPUT;
            startAction.Show();
        }
        else if (phase == PHASE_OUTRO) {
            game.setState("GameStateSideToss");
        }
    }

    public void OnTransitionUpdate(float value) {
        transParam = value;
    }

    // IWidgetListener -----------------------------------------------------------------------------
    @Override
    public boolean OnWidgetTouchStart(WidgetBase widget, int localX, int localY) {
        if (widget == startAction && phase == PHASE_INPUT) {
            startAction.SetColor("green");
        }

        return true;
    }

    @Override
    public boolean OnWidgetTouchEnd(WidgetBase widget, int localX, int localY) {
        if (widget == startAction) {
            phase = PHASE_OUTRO;
            startAction.Hide();
            transition.Reset();
            transition.Start();
        }

        startAction.SetColor("white");

        return true;
    }

    @Override
    public boolean OnWidgetDrag(WidgetBase widget, int localX, int localY) {
        return true;
    }

    @Override
    public boolean OnWidgetTouchCancel(WidgetBase widget, int localX, int localY) {
        return OnWidgetTouchEnd(widget, localX, localY);
    }

    // Implementation //////////////////////////////////////////////////////////////////////////////
    // Static --------------------------------------------------------------------------------------
    // Instance ------------------------------------------------------------------------------------
    private final float TRANSITION_DURATION     = 0.5f;
    private final int FONT_SIZE                 = 33;
    private final int PHASE_INTRO               = 0;
    private final int PHASE_INPUT               = 1;
    private final int PHASE_OUTRO               = 2;

    private Sprite infoSprite           = null;
    private TransitionTrig transition   = null;
    private int phase                   = -1;
    private float transParam            = 0.0f;
    private WidgetLabel startAction     = null;
}
