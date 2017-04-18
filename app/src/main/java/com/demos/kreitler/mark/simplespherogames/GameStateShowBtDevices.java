package com.demos.kreitler.mark.simplespherogames;

import android.bluetooth.BluetoothDevice;
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

import com.demos.kreitler.mark.demouilib.IWidgetListener;
import com.demos.kreitler.mark.demouilib.WidgetBase;
import com.demos.kreitler.mark.demouilib.WidgetLabel;
import com.demos.kreitler.mark.demouilib.WidgetList;
import com.demos.kreitler.mark.lib_demo_bluetooth.BluetoothNetwork;
import com.demos.kreitler.mark.lib_demo_sprites.Sprite;

import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

/**
 * Created by Mark on 6/22/2016.
 */
public class GameStateShowBtDevices extends GameStateBase implements IWidgetListener {
    private WidgetLabel titleLabel = null;

    // Interface ///////////////////////////////////////////////////////////////////////////////////
    // Static --------------------------------------------------------------------------------------
    // Instance ------------------------------------------------------------------------------------
    public GameStateShowBtDevices(GameView.GameThread game) {
        super(game);

        Resources res = game.getContext().getResources();

        String titleText = res.getString(R.string.bt_menu_title);
        titleLabel = new WidgetLabel(null, 0, 0, titleText, FONT_SIZE, "yellow", "fonts/FindleyBold.ttf");

        String optionText = res.getString(R.string.main_action_start);
        startAction = new WidgetLabel(null, 0, 0, 0.0f, 0.0f, optionText, FONT_SIZE, "white", "fonts/FindleyBold.ttf");
        startAction.AddListener(this);
    }

    @Override
    public void Enter(GameView.GameThread game) {
        super.Enter(game);

        Vector<WidgetLabel>options = new Vector<WidgetLabel>();

        listOptions = new WidgetList(null, 0.5f, LIST_VERTICAL_SPACING, titleLabel, options);
        listOptions.AddListener(this);

        listOptions.SetPosition(game.width() / 2, game.height() / 2);
        startAction.SetPosition(game.width() - startAction.GetWorldBounds().width() * 11 / 10,
                game.height() - startAction.GetWorldBounds().height() * 11 / 10);

        if (BluetoothNetwork.Instance != null) {

            Set<BluetoothDevice> pairedDevices = BluetoothNetwork.Instance.GetPairedDevices();

            if (pairedDevices != null) {
                Iterator<BluetoothDevice> device = pairedDevices.iterator();
                while (device != null) {
                    String optionText = device.toString().split(" ")[0];
                    options.add(new WidgetLabel(null, 0, 0, optionText, FONT_SIZE, "white", "fonts/FindleyBold.ttf"));
                }
            }
        }

        lastTouched = null;
    }

    @Override
    public void Draw(Canvas c) {
        c.save();

        c.drawARGB(255, 0, 0, 0);
        listOptions.Draw(c);
        startAction.Draw(c);

        c.restore();
    }

    @Override
    public boolean OnTouch(View v, MotionEvent e) {
        return listOptions.OnTouch(e) || startAction.OnTouch(e);
    }

    // IWidgetListener -----------------------------------------------------------------------------
    public boolean OnWidgetTouchStart(WidgetBase widget, int localX, int localY) {
        boolean bHandled = false;

        Resources res = game.getContext().getResources();
        if (widget == startAction) {
            startAction.SetColor("green");
            lastTouched = null;
            bHandled = true;
        }
        else {
            if (lastTouched != null && widget != lastTouched) {
                lastTouched.SetColor("white");
            }
            lastTouched = widget;

            if (widget != null) {
                widget.SetColor("green");
                selectedOption = widget.GetText();
                bHandled = true;
            }
        }

        return bHandled;
    }

    public boolean OnWidgetTouchEnd(WidgetBase widget, int localX, int localY) {
        startAction.SetColor("white");

        if (selectedOption != null && widget == startAction) {
            // Load the appropriate state.
            switch (selectedOption.toLowerCase()) {
                case "hot potato": {
                    game.setState("GameStateHotPotatoIntro");
                    break;
                }

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
    private final int FONT_SIZE                 = 33;
    private final int LIST_VERTICAL_SPACING     = 40;

    private WidgetList listOptions      = null;
    private WidgetBase lastTouched      = null;
    private WidgetLabel startAction     = null;
    private String selectedOption       = null;
}
