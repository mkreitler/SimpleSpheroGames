package com.demos.kreitler.mark.demouilib;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;

import java.util.List;
import java.util.Vector;

/**
 * Created by Mark on 6/24/2016.
 */
public class WidgetBase {
    // Interface ///////////////////////////////////////////////////////////////////////////////////
    // Static --------------------------------------------------------------------------------------
    public static void SetContext(Context context) {
        _context = context;
    }

    // Instance ------------------------------------------------------------------------------------
    public WidgetBase(WidgetBase parentWidget, int x, int y, int width, int height) {
        parent = parentWidget;
        bounds.set(x, y, x + width, y + height);
    }

    public void SetPosition(int x, int y) {
        if (x != this.x || y != this.y) {
            this.x = x;
            this.y = y;

            ComputeBounds();
            RecomputeChildBounds();
        }
    }

    public void SetTopLeft(int top, int left) {
        int width = bounds.width();
        int height = bounds.height();

        bounds.set(left, top, left + width, top + height);
    }

    public void SetAnchor(float x, float y) {
        anchorX = x;
        anchorY = y;

        ComputeBounds();
    }

    public void SetColor(String colorStr) {
        // Override in children.
    }

    public void SetDebugMode(boolean bModeOn) {
        bDebugMode = bModeOn;
    }

    public void AddListener(IWidgetListener newListener) {
        if (newListener != null) {
            listeners.add(newListener);
        }
    }

    public void RemoveListener(IWidgetListener oldListener) {
        listeners.remove(oldListener);
    }

    public void AddChild(WidgetBase newChild) {
        if (newChild != null) {
            children.add(newChild);
            newChild.SetParent(this);

            ComputeBounds();
            newChild.ComputeBounds();
        }
    }

    public boolean RemoveChild(WidgetBase removedChild) {
        boolean bRemoved = children.remove(removedChild);
        removedChild.SetParent(null);

        ComputeBounds();
        removedChild.RecomputePosition();

        return bRemoved;
    }

    public boolean IsVisible() {
        return bVisible;
    }

    public void SetVisible(boolean bWantsVisible) {
        bVisible = bWantsVisible;
    }

    public boolean IsTouchable() {
        return bTouchable;
    }

    public void Hide() {
        SetTouchable(false);
        SetVisible(false);
    }

    public void Show() {
        SetTouchable(true);
        SetVisible(true);
    }

    public void SetTouchable(boolean bWantsTouchable) {
        bTouchable = bWantsTouchable;
    }

    public void Draw(Canvas c) {
        if (IsVisible()) {
            DrawHierarchy(c);
        }
    }

    public Rect GetWorldBounds() {
        _workBounds.set(bounds.left, bounds.top, bounds.right, bounds.bottom);

        return _workBounds;
    }

    public void CopyWorldBounds(Rect boundsOut) {
        boundsOut.set(bounds.left, bounds.top, bounds.right, bounds.bottom);
    }

    public boolean ContainsScreenPoint(int x, int y) {
        Rect worldBounds = GetWorldBounds();

        return (x >= worldBounds.left &&
                y >= worldBounds.top &&
                x <= worldBounds.right &&
                y <= worldBounds.bottom);
    }

    public boolean OnTouch(MotionEvent e) {
        boolean bHandled = false;
        int x = Math.round(e.getX());
        int y = Math.round(e.getY());

        if (IsTouchable() && ContainsScreenPoint(x, y)) {
            WidgetBase touchWidget = OnTouchHierarchy(x, y, e);

            bHandled = touchWidget.DoTouch(x, y, e);
        }
        else {
            Log.d("DemoUiLib", ">>> Touchable: " + IsTouchable() + "   ContainsScreenPoint: " + ContainsScreenPoint(x, y));
        }

        return bHandled;
    }

    public String GetText() {
        // Override in children.
        return null;
    }

    // Implementation //////////////////////////////////////////////////////////////////////////////
    // Static --------------------------------------------------------------------------------------
    protected static Context _context   = null;
    protected static Paint _paint       = new Paint();
    protected static Rect _workBounds   = new Rect();

    // Instance ------------------------------------------------------------------------------------
    protected WidgetBase parent                     = null;
    protected Rect bounds                           = new Rect();
    protected Vector<WidgetBase> children           = new Vector<WidgetBase>();
    protected Vector<IWidgetListener> listeners     = new Vector<IWidgetListener>();
    protected float anchorX                         = 0.5f;
    protected float anchorY                         = 0.5f;
    protected int x                                 = 0;    // Horizontal location of anchor point
    protected int y                                 = 0;    // Vertical location of anchor point
    protected boolean bDebugMode                    = false;
    protected boolean bVisible                      = true;
    protected boolean bTouchable                    = true;

    protected void SetParent(WidgetBase newParent) {
        parent = newParent;
    }

    protected Rect ScreenPointToLocal(int x, int y) {
        int localX = x - GetWorldOriginX();
        int localY = y - GetWorldOriginY();

        _workBounds.set(localX, localY, localX, localY);

        return _workBounds;
    }

    protected int GetWorldOriginX() {
        int woX = 0;

        if (parent != null) {
            Rect parentBounds = parent.GetWorldBounds();
            woX += parentBounds.left;
        }

        return woX;
    }

    protected int GetWorldOriginY() {
        int woY = 0;

        if (parent != null) {
            Rect parentBounds = parent.GetWorldBounds();
            woY += parentBounds.top;
        }

        return woY;
    }

    protected void ComputeBounds() {
        bounds.offset(GetWorldOriginX(), GetWorldOriginY());
    }

    protected void Render(Canvas c) {
        // Draw debug info.
        // Usually, children will override this.
        if (bDebugMode) {
            _paint.setStyle(Paint.Style.STROKE);
            _paint.setColor(Color.YELLOW);
            c.drawRect(bounds.left, bounds.top, bounds.right, bounds.bottom, _paint);

            // Log.d("DemoUiLib", ">>> Bounds: (" + bounds.left + ", " + bounds.top + ", " + bounds.right + ", " + bounds.bottom + ")");
        }
    }

    protected void DrawHierarchy(Canvas c) {
        for (int i=0; i<children.size(); ++i) {
            children.elementAt(i).DrawHierarchy(c);
        }

        Render(c);
    }

    protected WidgetBase OnTouchHierarchy(int screenX, int screenY, MotionEvent e) {
        WidgetBase touchedWidget = this;

        for (int i=0; i<children.size(); ++i) {
            WidgetBase child = children.elementAt(i);

            if (child.IsTouchable() && child.ContainsScreenPoint(screenX, screenY)) {
                touchedWidget = child.OnTouchHierarchy(screenX, screenY, e);
                break;
            }
        }

        return touchedWidget;
    }

    protected boolean DoTouch(int worldX, int worldY, MotionEvent e) {
        boolean bHandled = false;
        Rect localPoint = ScreenPointToLocal(worldX, worldY);
        int localX = localPoint.left;
        int localY = localPoint.top;

        Log.d("DemoUiLib", ">>> Action: " + e.getAction());
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                bHandled = WidgetTouchStart(localX, localY);
                for (int i=0; i<listeners.size(); ++i) {
                    listeners.elementAt(i).OnWidgetTouchStart(this, localX, localY);
                }
                break;
            }

            case MotionEvent.ACTION_UP: {
                bHandled = WidgetTouchEnd(localX, localY);
                for (int i=0; i<listeners.size(); ++i) {
                    listeners.elementAt(i).OnWidgetTouchEnd(this, localX, localY);
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                bHandled = WidgetTouchDrag(localX, localY);
                for (int i=0; i<listeners.size(); ++i) {
                    listeners.elementAt(i).OnWidgetDrag(this, localX, localY);
                }
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                bHandled = WidgetTouchCancel(localX, localY);
                for (int i=0; i<listeners.size(); ++i) {
                    listeners.elementAt(i).OnWidgetTouchCancel(this, localX, localY);
                }
                break;
            }
        }

        return bHandled;
    }

    protected void RecomputePosition() {
        // When a child is removed from its parent, we want its position to stay
        // the same, so we have to recompute its anchor point.
        this.x = Math.round(bounds.left + (bounds.right - bounds.left) * this.anchorX);
        this.y = Math.round(bounds.top + (bounds.bottom - bounds.top) * this.anchorY);
    }

    protected void RecomputeChildBounds() {
        for (int i=0; i<children.size(); ++i) {
            children.elementAt(i).ComputeBounds();
        }
    }

    protected boolean WidgetTouchStart(int localX, int localY) {
        return true;
    }

    protected boolean WidgetTouchEnd(int localX, int localY) {
        return true;
    }

    protected boolean WidgetTouchDrag(int localX, int localY) {
        return true;
    }

    protected boolean WidgetTouchCancel(int localX, int localY) {
        return true;
    }

    protected int StringToColor(String colorStr) {
        int color = Color.BLACK;

        switch(colorStr) {
            case "black": {
                color = Color.BLACK;
                break;
            }

            case "blue": {
                color = Color.BLUE;
                break;
            }

            case "cyan": {
                color = Color.CYAN;
                break;
            }

            case "dkgray":case "dkgrey": {
                color = Color.DKGRAY;
                break;
            }

            case "gray":case "grey": {
                color = Color.GRAY;
                break;
            }

            case "green": {
                color = Color.GREEN;
                break;
            }

            case "ltgray":case "ltgrey": {
                color = Color.LTGRAY;
                break;
            }

            case "magenta": {
                color = Color.MAGENTA;
                break;
            }

            case "red": {
                color = Color.RED;
                break;
            }

            case "orange": {
                color = Color.argb(255, 255, 128, 0);
                break;
            }

            case "transparent": {
                color = Color.TRANSPARENT;
                break;
            }

            case "white": {
                color = Color.WHITE;
                break;
            }

            case "yellow": {
                color = Color.YELLOW;
                break;
            }

            default: {
                if (colorStr.substring(0, 0) == "#" && (colorStr.length() == 9 || colorStr.length() == 7)) {
                    int alpha = 0xff;

                    if (colorStr.length() == 9) {
                        // ARGB format.
                        alpha = Integer.parseInt("0000" + colorStr.substring(1, 2), 16);
                        colorStr = colorStr.substring(3);
                    }
                    else {
                        colorStr = colorStr.substring(1);
                    }

                    int red = Integer.parseInt("0000" + colorStr.substring(0, 1), 16);
                    int green = Integer.parseInt("0000" + colorStr.substring(2, 3), 16);
                    int blue = Integer.parseInt("0000" + colorStr.substring(4, 5), 16);

                    color = Color.argb(alpha, red, green, blue);
                }
                break;
            }
        }

        return color;
    }
}
