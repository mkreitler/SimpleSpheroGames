package com.demos.kreitler.mark.demouilib;

import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;

import java.util.Vector;

/**
 * Created by Mark on 6/26/2016.
 */
public class WidgetList extends WidgetBase implements IWidgetListener {
    // Interface ///////////////////////////////////////////////////////////////////////////////////
    // Static --------------------------------------------------------------------------------------

    // Instance ------------------------------------------------------------------------------------
    public WidgetList(WidgetBase parent, float align, int vSpace, WidgetLabel title, Vector<WidgetLabel> entries) {
        super(parent, 0, 0, 0, 0);

        childAlign = align;
        vertSpace = vSpace;

        if (title != null) {
            title.SetTouchable(false);
            title.SetTouchable(false);
            AddChild(title);
        }

        if (entries != null) {
            for (int i = 0; i <entries.size(); ++i) {
                WidgetLabel child = entries.elementAt(i);
                child.AddListener(this);
                AddChild(child);
            }
        }
    }

    @Override
    public void AddChild(WidgetBase child) {
        super.AddChild(child);

        for (int i=0; i<children.size(); ++i) {
            WidgetBase ithChild = children.elementAt(i);
            float childAnchorX = ithChild.anchorX;
            int left = GetWorldBounds().left;
            int right = GetWorldBounds().right;

            ithChild.SetPosition(Math.round((right - left) * (1.0f - childAnchorX)), vertSpace / 2 + vertSpace * i);
        }
    }

    // IWidgetListener -----------------------------------------------------------------------------
    public boolean OnWidgetTouchStart(WidgetBase widget, int localX, int localY) {
        for (int i=0; i<listeners.size(); ++i) {
            listeners.elementAt(i).OnWidgetTouchStart(widget, localX, localY);
        }

        return true;
    }

    public boolean OnWidgetTouchEnd(WidgetBase widget, int localX, int localY) {
        for (int i=0; i<listeners.size(); ++i) {
            listeners.elementAt(i).OnWidgetTouchEnd(widget, localX, localY);
        }

        return true;
    }

    public boolean OnWidgetDrag(WidgetBase widget, int localX, int localY) {
        for (int i=0; i<listeners.size(); ++i) {
            listeners.elementAt(i).OnWidgetDrag(widget, localX, localY);
        }

        return true;
    }

    public boolean OnWidgetTouchCancel(WidgetBase widget, int localX, int localY) {
        for (int i=0; i<listeners.size(); ++i) {
            listeners.elementAt(i).OnWidgetTouchCancel(widget, localX, localY);
        }

        return true;
    }

    // Implementation //////////////////////////////////////////////////////////////////////////////
    // Static --------------------------------------------------------------------------------------

    // Instance ------------------------------------------------------------------------------------
    private float childAlign    = 0.5f;
    private int vertSpace       = 0;

    @Override
    protected void ComputeBounds() {
        if (children.size() > 0) {
            int maxWidth    = 0;
            int totalHeight = 0;

            for (int i=0; i<children.size(); ++i) {
                Rect childBounds = children.elementAt(i).GetWorldBounds();
                maxWidth = Math.max(maxWidth, childBounds.width());
                totalHeight += vertSpace;
                Log.d("DemoUiLib", ">>> maxWidth of " + children.elementAt(i).GetText() + ": " + maxWidth);

            }

            int left = GetWorldOriginX() + x - Math.round(anchorX * maxWidth);
            int top  = GetWorldOriginY() + y - Math.round(anchorY * totalHeight);
            int right = left + maxWidth;
            int bottom = top + totalHeight;

            bounds.set(left, top, right, bottom);

            super.ComputeBounds();
        }
    }
}
