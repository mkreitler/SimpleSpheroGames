package com.demos.kreitler.mark.simplespherogames;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.demos.kreitler.mark.demouilib.WidgetLabel;
import com.demos.kreitler.mark.lib_demo_sprites.Sprite;

/**
 * Created by Mark on 6/22/2016.
 */
public class GameStateIntro extends GameStateBase {
    private final float WIDTH_SPACING   = 1.1f;
    private final int FONT_SIZE         = 33;

    private Sprite logo             = null;
    private WidgetLabel introLabel  = null;

    public GameStateIntro(GameView.GameThread game) {
        super(game);

        Resources res = game.getContext().getResources();
        logo = new Sprite(res.getDrawable(R.drawable.sphero_logo_blue, null), 0, 0, 0.5f, 0.5f);
        logo.setAnchor(0.5f, 0.5f);

        String introText = res.getString(R.string.intro_text);
        introLabel = new WidgetLabel(null, 0, 0, introText, FONT_SIZE, "white", "fonts/FindleyBold.ttf");
    }

    public void Draw(Canvas c) {
        c.save();

        c.drawARGB(1, 0, 0, 0);

        if (logo != null && GameView._font != null) {
            int w = c.getWidth();
            int h = c.getHeight();

            float scale = Math.min((float)(w / 2) / (float)logo.width(), (float)(h / 2) / (float)logo.height());
            logo.setScale(scale, scale);

            int x = w / 2;
            int y = h / 2;
            logo.setPosition(x, y);
            logo.draw(c);

            introLabel.SetPosition(Math.round(3 * c.getWidth() / 5), Math.round(3 * c.getHeight() / 4));
            introLabel.Draw(c);
        }
        else if (logo == null && GameView._font == null) {
            game.Fail("Logo and GameView._font not defined!");
        }
        else if (logo == null) {
            game.Fail("Logo no defined!");
        }
        else {
            game.Fail("GameView._font not defined!");
        }

        c.restore();
    }

    @Override
    public boolean OnTouch(View v, MotionEvent e) {
        boolean bHandled = false;

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                game.setState("gameStateFindRobot");
                bHandled = true;
            break;
        }
        return bHandled;
    }
}
