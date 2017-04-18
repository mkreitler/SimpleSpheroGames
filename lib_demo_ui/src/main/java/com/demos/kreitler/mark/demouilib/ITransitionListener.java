package com.demos.kreitler.mark.demouilib;

/**
 * Created by Mark on 6/28/2016.
 */
public interface ITransitionListener {
    void OnTransitionStart(float value);
    void OnTransitionEnd(float value);
    void OnTransitionUpdate(float value);
}
