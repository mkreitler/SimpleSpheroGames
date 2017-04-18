package com.demos.kreitler.mark.lib_demo_sprites;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

/**
 * Created by Mark on 6/15/2016.
 *
 * Convenience methods for BitmapDrawable objects
 */
public class Sprite {
    private static Rect _srcRect    = new Rect();
    private static Rect _destRect   = new Rect();
    private static Paint _paint     = null;
    public static void setPaint(Paint paint) {
        _paint = paint;
    }

    private Bitmap bmp          = null;
    private float anchorX       = 0.0f;     // 0 = draw from left side, 1 = draw from right side
    private float anchorY       = 0.0f;     // 0 = draw from top, 1 = draw from bottom
    private float positionX     = 0.0f;
    private float positionY     = 0.0f;
    private float scaleX        = 1.0f;
    private float scaleY        = 1.0f;
    private boolean bEnabled    = true;

    // Interface ///////////////////////////////////////////////////////////////////////////////////
    public void draw(Canvas c) {
        if (bEnabled && _paint != null && bmp != null && scaleX > 0.0f && scaleY > 0.0f) {
            int w = bmp.getWidth();
            int h = bmp.getHeight();

            _srcRect.set(0, 0, w, h);

            w = Math.round(w * scaleX);
            h = Math.round(h * scaleY);

            int x = Math.round(positionX - anchorX * w);
            int y = Math.round(positionY - anchorY * h);

            _destRect.set(x, y, x + w, y + h);

            c.drawBitmap(bmp, _srcRect, _destRect, _paint);
        }
        else if (_paint == null) {
            Log.d("Sprite", "!!! no _paint object defined!");
        }
        else if (bmp == null) {
            Log.d("Sprite", "!!! no bmp defined!");
        }
        else {
            Log.d("Sprite", "!!! invalid scales: " + scaleX + ", " + scaleY);
        }
    }

    // Accessors
    public int scaledWidth() {
        return Math.round(bmp.getWidth() * scaleX);
    }

    public int scaledHeight() {
        return Math.round(bmp.getHeight() * scaleY);
    }

    public int width() {
        return bmp.getWidth();
    }

    public int height() {
        return bmp.getHeight();
    }

    public float x() { return positionX; }

    public float y() { return positionY; }

    public boolean isEnabled() {
        return bEnabled;
    }

    public void enable() {
        bEnabled = true;
    }

    public void disable() {
        bEnabled = false;
    }

    public Bitmap bitmap() {return bmp;}

    public void setDrawable(Drawable drawable) {
        BitmapDrawable bmpDrawable = (BitmapDrawable)drawable;

        if (bmpDrawable != null) {
            bmp = bmpDrawable.getBitmap();
        }
    }

    public void setAnchor(float x, float y) {
        anchorX = x;
        anchorY = y;
    }

    public void setScale(float x, float y) {
        scaleX = x;
        scaleY = y;
    }

    public void setPosition(float x, float y) {
        positionX = x;
        positionY = y;
    }

    public void randomizeScale(float scaleSmall, float scaleLarge) {
        if (scaleSmall > 0.0f && scaleLarge > 0.0f && scaleLarge > scaleSmall) {
            float scale = (float)(scaleSmall + (scaleLarge - scaleSmall) * Math.random());
            setScale(scale, scale);
        }
    }

    public void move(float dx, float dy) {
        positionX += dx;
        positionY += dy;
    }

    // Constructors
    public Sprite(Drawable drawable, float x, float y, float anchorX, float anchorY, float scaleX, float scaleY) {
        BitmapDrawable bitmapDrawable = (BitmapDrawable)drawable;
        bmp = bitmapDrawable != null ? bitmapDrawable.getBitmap() : null;

        setPosition(x, y);
        setAnchor(anchorX, anchorY);
        setScale(scaleX, scaleY);
    }

    public Sprite(Drawable drawable, float x, float y, float anchorX, float anchorY) {
        BitmapDrawable bitmapDrawable = (BitmapDrawable)drawable;
        bmp = bitmapDrawable != null ? bitmapDrawable.getBitmap() : null;

        setPosition(x, y);
        setAnchor(anchorX, anchorY);
        setScale(1.0f, 1.0f);
    }

    public Sprite(Drawable drawable, float x, float y) {
        BitmapDrawable bitmapDrawable = (BitmapDrawable)drawable;
        bmp = bitmapDrawable != null ? bitmapDrawable.getBitmap() : null;

        setPosition(x, y);
        setAnchor(1.0f, 1.0f);
        setScale(1.0f, 1.0f);
    }
}
