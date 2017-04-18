package com.demos.kreitler.mark.lib_demo_sphero;

/**
 * Created by Mark on 7/6/2016.
 */
public interface IRobotCommandListener {
    void OnCommandEnter(IRobotCommand command);
    void OnCommandExit(IRobotCommand command);
}
