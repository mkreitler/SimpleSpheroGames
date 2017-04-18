package com.demos.kreitler.mark.demouilib;

/**
 * Created by Mark on 6/28/2016.
 */
public class TransitionTrig extends TransitionBase {
    // Interface ///////////////////////////////////////////////////////////////////////////////////
    // Static --------------------------------------------------------------------------------------
    // Instance ------------------------------------------------------------------------------------
    public TransitionTrig(float duration) {
        super(duration);
    }

    // Implementation //////////////////////////////////////////////////////////////////////////////
    // Static --------------------------------------------------------------------------------------
    // Instance ------------------------------------------------------------------------------------
    @Override
    protected float FilteredResult() {
        float result = super.FilteredResult();

        result = 0.5f * (1.0f - (float)Math.cos(param * Math.PI));

        return result;
    }
}
