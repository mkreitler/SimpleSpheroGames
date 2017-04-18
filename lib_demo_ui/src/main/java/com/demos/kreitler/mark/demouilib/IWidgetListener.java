package com.demos.kreitler.mark.demouilib;

/**
 * Created by Mark on 6/26/2016.
 */
public interface IWidgetListener {
    boolean OnWidgetTouchStart(WidgetBase widget, int localX, int localY);
    boolean OnWidgetTouchEnd(WidgetBase widget, int localX, int localY);
    boolean OnWidgetDrag(WidgetBase widget, int localX, int localY);
    boolean OnWidgetTouchCancel(WidgetBase widget, int localX, int localY);
}
