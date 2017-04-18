package com.demos.kreitler.mark.demouilib;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Log;

/**
 * Created by Mark on 6/26/2016.
 */
public class WidgetLabel extends WidgetBase {
    // Interface ///////////////////////////////////////////////////////////////////////////////////
    // Static --------------------------------------------------------------------------------------

    // Instance ------------------------------------------------------------------------------------
    public WidgetLabel(WidgetBase parent, int x, int y, float anchorX, float anchorY, String text, float textSize, String color, String fontName) {
        super(parent, 0, 0, 0, 0);

        SetFont(fontName);
        SetTextSize(textSize);
        SetColor(color);
        SetParent(parent);

        SetText(text);

        SetAnchor(anchorX, anchorY);
        SetPosition(x, y);

        bAutoBound = true;
        ComputeBounds();
    }

    public WidgetLabel(WidgetBase parent, int x, int y, String text, float textSize, String color, String fontName) {
        this(parent, x, y, 0.5f, 0.5f, text, textSize, color, fontName);
    }

    public void SetTextSize(float size) {
        if (size > 0.0f) {
            textSize = size;
        }
        else {
            textSize = DEFAULT_TEXT_SIZE;
            SetError(ERR_INVALID_TEXT_SIZE);
        }

        ComputeBounds();
    }

    public void SetText(String newText) {
        if (errMsg == null) {
            text = newText;

            ComputeBounds();
        }
    }

    public void SetParent(WidgetBase parent) {
        this.parent = parent;
        ComputeBounds();
    }

    @Override
    public void SetColor(String colorStr) {
        if (colorStr != null && errMsg == null) {
            color = StringToColor(colorStr);
            colorStr = colorStr.trim().toLowerCase();
        }
    }

    @Override
    public String GetText() {
        return text;
    }

    // Implementation //////////////////////////////////////////////////////////////////////////////
    // Static --------------------------------------------------------------------------------------
    private static final float DEFAULT_TEXT_SIZE        = 16.0f;
    private static final String ERR_INVALID_TEXT_SIZE   = "Invalid Text Size!";

    // Instance ------------------------------------------------------------------------------------
    private String text         = null;
    private Typeface font       = null;
    private float textSize      = 0.0f;
    private int color           = Color.WHITE;
    private String errMsg       = null;
    private boolean bAutoBound  = false;

    @Override
    protected void ComputeBounds() {
        if (bAutoBound) {
            _paint.setTextSize(textSize);
            _paint.getTextBounds(text, 0, text.length() - 1, bounds);

            bounds.set(x - Math.round(anchorX * bounds.width()),
                       y - Math.round(anchorY * bounds.height()),
                       x - Math.round(anchorX * bounds.width()) + bounds.width(),
                       y - Math.round(anchorY * bounds.height()) + bounds.height());

            super.ComputeBounds();
        }
    }

    @Override
    protected void Render(Canvas c) {
        if (c != null && _paint != null) {
            _paint.setTypeface(font);
            _paint.setStyle(Paint.Style.FILL);
            _paint.setColor(color);

            c.drawText(text, bounds.left, bounds.top - _paint.ascent(), _paint);
        }

        super.Render(c);
    }

    private void SetError(String errMsg) {
        SetColor("red");
        SetText(errMsg);

        Log.d("DemoUiLib", errMsg);
    }

    private void SetFont(String fontName) {
        if (fontName != null && _context != null) {
            Resources res = _context.getResources();
            font = Typeface.createFromAsset(res.getAssets(), fontName);
        }
        else {
            font = Typeface.DEFAULT;
        }
    }
}
