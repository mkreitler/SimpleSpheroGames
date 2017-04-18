package com.demos.kreitler.mark.demouilib;

import java.util.Vector;

/**
 * Created by Mark on 6/28/2016.
 */
public class TransitionBase {
    // Interface ///////////////////////////////////////////////////////////////////////////////////
    // Static --------------------------------------------------------------------------------------
    // Instance ------------------------------------------------------------------------------------
    public TransitionBase(float duration) {
        this.duration = duration;
    }

    public void Reset() {
        param = 0.0f;
        goal  = 1.0f;
        timer = 0.0f;
    }

    public void Start() {
        param = 1.0f - goal;
        timer = 0.0f;

        for (int i=0; i<listeners.size(); ++i) {
            listeners.elementAt(i).OnTransitionStart(FilteredResult());
        }

        lastTimeMS  = java.lang.System.currentTimeMillis();
    }

    public void Finish() {
        param = goal;
        timer = duration;

        for (int i=0; i<listeners.size(); ++i) {
            listeners.elementAt(i).OnTransitionEnd(FilteredResult());
        }
    }

    public void Reverse() {
        goal  = 1.0f - goal;
        timer = duration - timer;
    }

    public void Update() {
        if (timer <= duration) {
            long currentTimeMS  = java.lang.System.currentTimeMillis();
            int elapsedMS       = (int)(currentTimeMS - lastTimeMS);

            lastTimeMS = currentTimeMS;

            timer += elapsedMS / 1000.0f;

            if (timer >= duration) {
                Finish();
            }
            else {
                if (goal > 0.5f) {
                    param = timer / duration;
                }
                else {
                    param = 1.0f - timer / duration;
                }

                for (int i=0; i<listeners.size(); ++i) {
                    listeners.elementAt(i).OnTransitionUpdate(FilteredResult());
                }
            }
        }
    }

    public void AddListener(ITransitionListener newListener) {
        if (newListener != null) {
            listeners.add(newListener);
        }
    }

    public void RemoveListener(ITransitionListener oldListener) {
        listeners.remove(oldListener);
    }

    public void ClearListeners() {
        listeners.removeAllElements();
    }

    // Implementation //////////////////////////////////////////////////////////////////////////////
    // Static --------------------------------------------------------------------------------------
    // Instance ------------------------------------------------------------------------------------
    protected float duration    = 1.0f;
    protected long lastTimeMS   = 0;
    protected float param       = 0.0f;
    protected float goal        = 1.0f;
    protected float timer       = 0.0f;

    protected Vector<ITransitionListener>listeners = new Vector<ITransitionListener>();
    protected float FilteredResult() {
        return Math.max(Math.min(param, 1.0f), 0.0f);
    }
}
