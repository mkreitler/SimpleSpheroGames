package com.demos.kreitler.mark.simplespherogames.Prototypes;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.View;

import com.demos.kreitler.mark.demouilib.IWidgetListener;
import com.demos.kreitler.mark.demouilib.WidgetBase;
import com.demos.kreitler.mark.demouilib.WidgetLabel;
import com.demos.kreitler.mark.demouilib.WidgetList;
import com.demos.kreitler.mark.lib_demo_sprites.Sprite;
import com.demos.kreitler.mark.simplespherogames.GameStateBase;
import com.demos.kreitler.mark.simplespherogames.GameView;
import com.demos.kreitler.mark.simplespherogames.R;
import com.orbotix.ConvenienceRobot;
import com.orbotix.async.CollisionDetectedAsyncData;
import com.orbotix.async.DeviceSensorAsyncMessage;
import com.orbotix.common.sensor.SensorFlag;
import com.orbotix.subsystem.SensorControl;

import java.util.Vector;

/**
 * Created by Mark on 6/22/2016.
 */
public class GameStateFuse extends GameStateBase implements IWidgetListener {
    // Interface ///////////////////////////////////////////////////////////////////////////////////
    // Static --------------------------------------------------------------------------------------
    // Instance ------------------------------------------------------------------------------------
    private final int FONT_SIZE                 = 33;
    private final int LIST_VERTICAL_SPACING     = 40;
    private final int MIN_MOVE_THRESH           = 5;
    private final int MAX_NODES                 = 512;
    private final float VECTOR_CROSS_THRESH     = 0.5f;  //cosine of 45 degrees, squared
    private final float CONSUME_FACTOR          = 0.0001f;
    private final float ROBOT_SPEED             = 0.33f;
    private final int COLLISION_THRESH          = 100;
    private final int GAME_OVER_DELAY           = 3000;

    private final int FUSE_DELAY_TIME_MS        = 1000;
    private final float MIN_CONSUME_SPEED       = 10.0f / 100.0f;
    private final float MAX_CONSUME_SPEED       = 50.0f / 100.0f;
    private final float CONSUME_ACCEL           = 1.0f / 100.0f;

    private final float SCALE_SMALL             = 0.67f;
    private final float SCALE_LARGE             = 1.0f;

    private WidgetList listOptions      = null;
    private boolean bStarted            = false;
    private PointerCoords pointerCoords = new PointerCoords();
    private int lastX                   = -1;
    private int lastY                   = -1;
    private float driveTimer            = 0.0f;
    private float wantHeading           = 0.0f;

    private static NavNode[] NODES      = null;

    ConvenienceRobot robot          = null;
    Vector<WidgetLabel>options      = null;
    int headNode                    = -1;
    int tailNode                    = -1;
    int tailLength                  = 0;
    int fuseDelayTimer              = FUSE_DELAY_TIME_MS;
    float fuseConsumeSpeedMS        = MIN_CONSUME_SPEED;
    float fuseConsumed              = 0.0f;
    boolean bGameOver               = false;
    int     driveNode               = 0;
    int gameOverTimer               = 0;
    Sprite sparkSprite              = null;
    Sprite boomSprite               = null;

    public GameStateFuse(GameView.GameThread game) {
        super(game);

        if (NODES == null) {
            NODES = new NavNode[MAX_NODES];
            for (int i=0; i<MAX_NODES; ++i) {
                NODES[i] = new NavNode();
            }
        }

        Resources res = game.getContext().getResources();

        sparkSprite = new Sprite(res.getDrawable(R.drawable.spark, null), 0, 0, 0.5f, 0.5f);
        boomSprite = new Sprite(res.getDrawable(R.drawable.boom, null), game.width() / 2, game.height() / 2, 0.5f, 0.5f);

        String titleText = res.getString(R.string.fuse_title);
        WidgetLabel titleLabel = new WidgetLabel(null, 0, 0, titleText, FONT_SIZE, "yellow", "fonts/FindleyBold.ttf");

        options = new Vector<WidgetLabel>();
        String optionText = res.getString(R.string.fuse_instructions);
        options.add(new WidgetLabel(null, 0, 0, optionText, FONT_SIZE, "white", "fonts/FindleyBold.ttf"));

        listOptions = new WidgetList(null, 0.5f, LIST_VERTICAL_SPACING, titleLabel, options);
        listOptions.AddListener(this);

        listOptions.SetPosition(game.width() / 2, 0);
    }

    @Override
    public void Enter(GameView.GameThread game) {
        super.Enter(game);

        robot = game.getRobot();
        assert(robot != null);

        long sensorFlag = SensorFlag.QUATERNION.longValue()
                | SensorFlag.ACCELEROMETER_NORMALIZED.longValue()
//                | SensorFlag.GYRO_NORMALIZED.longValue()
//                | SensorFlag.MOTOR_BACKEMF_NORMALIZED.longValue()
//                | SensorFlag.ATTITUDE.longValue()
        ;

        robot.getSensorControl().enableSensors(sensorFlag, SensorControl.StreamingRate.STREAMING_RATE20);
        robot.setBackLedBrightness(1.0f);
        robot.enableStabilization(false);

        bStarted                = false;
        lastX                   = -1;
        lastY                   = -1;
        fuseDelayTimer          = FUSE_DELAY_TIME_MS;
        fuseConsumeSpeedMS      = MIN_CONSUME_SPEED;
        fuseConsumed            = 0.0f;
        bGameOver               = false;
        headNode                = -1;
        tailNode                = -1;
        tailLength              = 0;
        robot                   = game.getRobot();
        gameOverTimer           = GAME_OVER_DELAY;

        assert(robot != null);
    }

    @Override
    public void Exit() {
        robot.getSensorControl().disableSensors();
        robot.enableStabilization(true);
        robot.setBackLedBrightness(0.0f);
    }

    @Override
    public void Draw(Canvas c) {
        c.save();

        if (bGameOver) {
            GameView._paint.setStyle(Paint.Style.FILL);
            c.drawARGB(255, 255, 255, 0);
            boomSprite.draw(c);
        }
        else {
            GameView._paint.setStyle(Paint.Style.FILL);
            c.drawARGB(255, 0, 0, 0);

            GameView._paint.setStyle(Paint.Style.STROKE);
            if (headNode > 0) {
                if (headNode > tailNode) {
                    for (int i = tailNode; i <= headNode; ++i) {
                        // NODES[i].draw(c);
                        if (i > tailNode) {
                            NODES[i - 1].connect(c, NODES[i]);
                        }
                    }
                } else {
                    for (int i = tailNode; i < MAX_NODES; ++i) {
                        // NODES[i].draw(c);
                        if (i > tailNode) {
                            NODES[i - 1].connect(c, NODES[i]);
                        }
                    }
                    for (int i = 0; i <= headNode; ++i) {
                        // NODES[i].draw(c);
                        if (i > 0) {
                            NODES[i - 1].connect(c, NODES[i]);
                        }
                    }
                }
            }

            if (fuseDelayTimer <= 0) {
                sparkSprite.randomizeScale(SCALE_SMALL, SCALE_LARGE);
                sparkSprite.draw(c);
            }

            listOptions.Draw(c);
        }

        c.restore();
    }

    @Override
    public boolean Update(int dtMS) {
        boolean bResult = true;

        if (bStarted && !bGameOver) {
            UpdateNodePosition();
            UpdateDrivePosition(dtMS);
            UpdateFuse(dtMS);
        }
        else if (bGameOver) {
            gameOverTimer -= dtMS;

            if (gameOverTimer <= 0) {
                game.setState("GameStateMainMenu");
            }
        }

        return bResult;
    }

    @Override
    public boolean OnTouch(View v, MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            if (!bStarted) {
                robot.setZeroHeading();
                robot.setBackLedBrightness(1.0f);
                bStarted = true;
                e.getPointerCoords(0, pointerCoords);
                lastX = Math.round(pointerCoords.x);
                lastY = Math.round(pointerCoords.y);
                headNode = -1;
                tailNode = -1;
                driveNode = -1;

                robot.setBackLedBrightness(0.0f);
                robot.enableStabilization(true);
            }
            else {
                e.getPointerCoords(0, pointerCoords);
            }
        }
        else if (bStarted && e.getAction() == MotionEvent.ACTION_MOVE) {
            e.getPointerCoords(0, pointerCoords);
        }
        else if (bStarted && (e.getAction() == MotionEvent.ACTION_BUTTON_RELEASE || e.getAction() == MotionEvent.ACTION_CANCEL)) {
            // Explode.
            bStarted = false;
        }

        return true;
    }

    @Override
    public void HandleSensorData(DeviceSensorAsyncMessage message) {
    }

    public void HandleCollisionMessage(CollisionDetectedAsyncData colData) {
        int impactX = colData.getImpactPower().x;
        int impactY = colData.getImpactPower().y;

        if (impactX * impactX + impactY * impactY > COLLISION_THRESH * COLLISION_THRESH) {
            tailLength /= 2;

            tailNode = headNode - (tailLength - 1);
            if (tailNode < 0) {
                tailNode += MAX_NODES;
                NODES[tailNode].color = Color.RED;
            }

            validateDriveNode();
        }
    }

    // IWidgetListener -----------------------------------------------------------------------------
    public boolean OnWidgetTouchStart(WidgetBase widget, int localX, int localY) {
        return true;
    }

    public boolean OnWidgetTouchEnd(WidgetBase widget, int localX, int localY) {
        return true;
    }

    public boolean OnWidgetDrag(WidgetBase widget, int localX, int localY) {
        return true;
    }

    public boolean OnWidgetTouchCancel(WidgetBase widget, int localX, int localY) {
        return OnWidgetTouchEnd(widget, localX, localY);
    }

    // Implementation //////////////////////////////////////////////////////////////////////////////
    // Static --------------------------------------------------------------------------------------
    // Instance ------------------------------------------------------------------------------------
    private void UpdateFuse(int dtMS) {
        int oldTailNode = tailNode;

        if (fuseDelayTimer > 0) {
            fuseDelayTimer -= dtMS;
        }
        else {
            fuseConsumeSpeedMS += CONSUME_ACCEL * dtMS * 0.001f;
            fuseConsumeSpeedMS = Math.min(fuseConsumeSpeedMS, MAX_CONSUME_SPEED);

            fuseConsumed += Math.round(fuseConsumeSpeedMS * dtMS);

            while (tailNode >= 0 && NODES[tailNode].radius <= fuseConsumed) {
                fuseConsumed -= NODES[tailNode].radius;

                tailNode = tailNode + 1;
                tailLength -= 1;

                if (tailNode >= MAX_NODES) {
                    tailNode = 0;
                }

                if (tailNode != headNode) {
                    int intersectNode = FindIntersection(NODES[tailNode].x, NODES[tailNode].y);

                    if (intersectNode >= 0) {
                        tailNode = intersectNode;

                        tailLength = headNode - tailNode;
                        if (tailLength < 0) {
                            tailLength += MAX_NODES + 1;
                        } else {
                            tailLength += 1;
                        }

                        fuseConsumed = 0;
                    }
                }

                if (tailNode == headNode) {
                    bGameOver = true;
                    robot.stop();
                    break;
                }
                else {
                    NODES[tailNode].color = Color.RED;
                }
            }

            sparkSprite.setPosition(NODES[tailNode].x, NODES[tailNode].y);
            validateDriveNode();
        }
    }

    private void validateDriveNode() {
        if (tailNode > headNode) {
            if (driveNode < tailNode && driveNode > headNode) {
                driveNode = tailNode;
            }
        }
        else {
            if (driveNode < tailNode || driveNode > headNode) {
                driveNode = tailNode;
            }
        }
    }

    private void UpdateDrivePosition(int dtMS) {
        Log.d("Fuse", "<1>");
        if (driveTimer <= 0.0) {
            Log.d("Fuse", "<2>");
            if (driveNode != headNode) {
                Log.d("Fuse", "<3>");
                int iFromNode = driveNode;
                int iToNode = driveNode + 1;

                if (iToNode >= MAX_NODES) {
                    iToNode -= MAX_NODES;
                    assert(iToNode == 0);
                }

                NavNode fromNode = NODES[iFromNode];
                NavNode toNode = NODES[iToNode];

                float driveDx = toNode.x - fromNode.x;
                float driveDy = toNode.y - fromNode.y;

                float heading = (float) (Math.atan2(driveDx, -driveDy) * 180.0 / Math.PI);
                driveTimer = (float) (Math.sqrt(driveDx * driveDx + driveDy * driveDy) / ROBOT_SPEED * CONSUME_FACTOR);

                while (heading < 0) {
                    heading += 360.0f;
                }

                wantHeading = heading;

                Log.d("Fuse", "===========================");
                Log.d("Fuse", ">>> heading: " + wantHeading + "   driveTimer: " + driveTimer);

                // robot.drive(wantHeading, Math.round(ROBOT_SPEED * (1.0 + Math.abs(Math.cos(wantHeading * 180.0 / Math.PI))) / 2.0));
                robot.drive(wantHeading, ROBOT_SPEED);

                NODES[driveNode].color = Color.BLUE;

                driveNode += 1;
                if (driveNode >= MAX_NODES) {
                    driveNode -= MAX_NODES;
                }
            }
            else {
                robot.drive(wantHeading, 0);
                Log.d("Fuse", "+++++++++++++++++++++++++++");
                Log.d("Fuse", "Stop");
            }
        }
        else {
            driveTimer -= (dtMS * 0.001f);
        }
    }

    private void UpdateNodePosition() {
        int x = Math.round(pointerCoords.x);
        int y = Math.round(pointerCoords.y);
        int dx = Math.abs(Math.round(pointerCoords.x) - lastX);
        int dy = Math.abs(Math.round(pointerCoords.y) - lastY);
        int dMax = Math.max(dx, dy);

        if (bStarted) {
            if (dx > MIN_MOVE_THRESH || dy > MIN_MOVE_THRESH) {
                AddNode();

                NavNode curNode = NODES[headNode];

                curNode.x = x;
                curNode.y = y;
                curNode.color = Color.GREEN;

                curNode.radius = (float) dMax;

                lastX = curNode.x;
                lastY = curNode.y;

                // Log.d("Fuse", ">>> headNode: " + headNode + "    taieNode: " + tailNode);
            }
        }
    }

    private int FindIntersection(int x, int y) {
        int intersectNode = -1;

        if (headNode >= 0) {
            int searchNode = headNode - 1;

            if (searchNode < 0) {
                searchNode += MAX_NODES;
            }

            if (searchNode < tailNode) {
                for (int i = searchNode; i >= 0; --i) {
                    if (NODES[i].contains(x, y)) {
                        intersectNode = i;
                        break;
                    }
                }

                if (intersectNode < 0) {
                    for (int i = MAX_NODES - 1; i >= tailNode; --i) {
                        if (NODES[i].contains(x, y)) {
                            intersectNode = i;
                            break;
                        }
                    }
                }
            } else {
                for (int i = searchNode; i >= tailNode; --i) {
                    if (NODES[i].contains(x, y)) {
                        intersectNode = i;
                        break;
                    }
                }
            }
        }

        return intersectNode;
    }

    private void AddNode() {
        if (tailLength < MAX_NODES) {
            if (headNode < 0) {
                headNode = 0;
                tailLength = 1;
                driveNode = 0;
                driveTimer = 0.0f;
            } else {
                headNode += 1;
                tailLength += 1;
                if (headNode >= MAX_NODES) {
                    headNode = 0;
                }
            }

            if (tailNode < 0) {
                tailNode = headNode;
            }
        }
    }

    private void DebugSensorData() {
        WidgetLabel accelLabel = options.elementAt(0);
        assert(accelLabel != null);

        Resources res = game.getContext().getResources();
        accelLabel.SetText("");
    }

    private void SetDebugText(int resID) {
        WidgetLabel accelLabel = options.elementAt(0);
        Resources res = game.getContext().getResources();
        accelLabel.SetText(res.getString(resID));
    }

    private class NavNode {
        public int x                = 0;
        public int y                = 0;
        public float radius         = 0.0f;
        public int headingDegrees   = 0;
        public int color            = Color.GREEN;

        public void draw(Canvas c) {
            GameView._paint.setColor(color);
            c.drawArc(x - radius, y - radius, x + radius, y + radius, 0, 360, true, GameView._paint);
        }

        public void connect(Canvas c, NavNode nextNode) {
            GameView._paint.setColor(color);

            if (nextNode != null) {
                c.drawLine(x, y, nextNode.x, nextNode.y, GameView._paint);
            }
        }

        public boolean contains(int x, int y) {
            int dx = this.x - x;
            int dy = this.y - y;

            return dx * dx + dy * dy < radius * radius;
        }
    }
}
