package com.demos.kreitler.mark.simplespherogames.Prototypes;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.MotionEvent.PointerCoords;

import com.demos.kreitler.mark.demouilib.IWidgetListener;
import com.demos.kreitler.mark.demouilib.WidgetBase;
import com.demos.kreitler.mark.demouilib.WidgetLabel;
import com.demos.kreitler.mark.demouilib.WidgetList;
import com.demos.kreitler.mark.simplespherogames.GameStateBase;
import com.demos.kreitler.mark.simplespherogames.GameView;
import com.demos.kreitler.mark.simplespherogames.R;
import com.orbotix.ConvenienceRobot;
import com.orbotix.async.CollisionDetectedAsyncData;
import com.orbotix.async.DeviceSensorAsyncMessage;
import com.orbotix.common.sensor.DeviceSensorsData;
import com.orbotix.common.sensor.LocatorData;
import com.orbotix.common.sensor.SensorFlag;
import com.orbotix.subsystem.SensorControl;

import java.util.Vector;

import static android.content.Context.SENSOR_SERVICE;

/**
 * Created by Mark on 6/22/2016.
 */
public class GameStateMinefield extends GameStateBase implements IWidgetListener {
    // Interface ///////////////////////////////////////////////////////////////////////////////////
    // Static --------------------------------------------------------------------------------------
    // Instance ------------------------------------------------------------------------------------
    private final int FONT_SIZE                 = 33;
    private final int LIST_VERTICAL_SPACING     = 40;
    private final float COLLISION_THRESH        = 140.0f;
    private final float COLLISION_THRESH_LG     = 180.0f;
    private final int ID_EMPTY                  = 0;
    private final int ID_TREASURE_BLUE          = 1;
    private final int ID_TREASURE_GREEN         = 2;
    private final int ID_MINE_RED               = -1;
    private final float LOCATOR_DIST_PER_GRID   = 1.0f;
    private final float SPEED                   = 0.33f;
    private final int TREASURE_SCORE            = 100;
    private final int GAME_TIME                 = 3 * 60 * 1000;
    private final float EPSILON                 = 0.001f;
    private final int SPINUP_TIME               = 200;
    private final int DRIVE_TIME_PER_GRID_MS    = 100;
    private final float SPEED_THRESH            = 5.0f;

    private boolean bStarted            = false;
    private SensorManager sensorManager = null;
    int elapsedMS                       = 0;
    WidgetLabel titleLabel              = null;
    WidgetLabel scoreLabel              = null;
    WidgetLabel timeLabel               = null;
    int[][] grid                        = {
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0,},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0,},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0,},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0,},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0,},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0,},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0,},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0,},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0,},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0,},
    };

    ConvenienceRobot robot              = null;
    Vector<WidgetLabel>options          = null;
    Vector<Point>gridCells              = new Vector<>();
    int iRobotRow                       = -1;
    int iRobotCol                       = -1;
    PointerCoords pointerCoords         = new PointerCoords();
    float robotX                        = 0.0f;
    float robotY                        = 0.0f;
    float startX                        = 0.0f;
    float startY                        = 0.0f;
    float heading                       = 0.0f;
    float distance                      = 0.0f;
    boolean bMoving                     = false;
    int startRow                        = -1;
    int startCol                        = -1;
    float totalDist                     = 0.0f;
    int score                           = 0;
    int time                            = 0;
    boolean bGameOver                   = false;
    int spinUpTimeMS                    = 0;
    int rollTimeMS                      = 0;
    int totalRollTimeMS                 = 0;
    float velocityX                     = 0.0f;
    float velocityY                     = 0.0f;

    public GameStateMinefield(GameView.GameThread game) {
        super(game);

        Resources res = game.getContext().getResources();

        sensorManager=(SensorManager) game.getContext().getSystemService(SENSOR_SERVICE);

        String titleText = res.getString(R.string.minefield_title);
        titleLabel = new WidgetLabel(null, 0, 0, titleText, FONT_SIZE, "yellow", "fonts/FindleyBold.ttf");
        titleLabel.SetAnchor(1.0f, 0.0f);
        titleLabel.SetPosition(Math.round(0.95f * game.width()), Math.round(0.05f * game.height()));

        titleLabel.AddListener(this);
        titleLabel.SetTouchable(true);

        String scoreText = res.getString(R.string.score);
        scoreLabel = new WidgetLabel(null, 0, 0, scoreText + "0", FONT_SIZE, "yellow", "fonts/FindleyBold.ttf");
        scoreLabel.SetAnchor(0.0f, 0.0f);
        scoreLabel.SetPosition(Math.round(0.05f * game.width()), Math.round(0.05f * game.height()));

        String timeText = res.getString(R.string.time);
        timeLabel = new WidgetLabel(null, 0, 0, timeText + "0", FONT_SIZE, "yellow", "fonts/FindleyBold.ttf");
        timeLabel.SetAnchor(0.0f, 0.0f);
        timeLabel.SetPosition(Math.round(0.05f * game.width()), Math.round(0.25f * game.height()));

        for (int i=0; i<grid.length; ++i) {
            for (int j=0; j<grid[0].length; ++j) {
                if (i != grid.length / 2 || j != grid[0].length / 2) {
                    gridCells.add(new Point(j, i));
                }
            }
        }
    }

    @Override
    public void Enter(GameView.GameThread game) {
        super.Enter(game);

        assert(scoreLabel != null);
        assert(timeLabel != null);

        robot = game.getRobot();
        assert(robot != null);

        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);

        long sensorFlag = SensorFlag.QUATERNION.longValue()
                  | SensorFlag.LOCATOR.longValue()
                  | SensorFlag.VELOCITY.longValue()
//                | SensorFlag.ACCELEROMETER_NORMALIZED.longValue()
//                | SensorFlag.GYRO_NORMALIZED.longValue()
//                | SensorFlag.MOTOR_BACKEMF_NORMALIZED.longValue()
//                | SensorFlag.ATTITUDE.longValue()
        ;

        robot.getSensorControl().enableSensors(sensorFlag, SensorControl.StreamingRate.STREAMING_RATE20);
        robot.setBackLedBrightness(1.0f);
        robot.enableStabilization(false);
        robot.setLed(0.f, 0.f, 0.f);

        time                    = 0;
        score                   = 0;
        bStarted                = false;
        bMoving                 = false;
        robot                   = game.getRobot();
        iRobotRow               = grid.length / 2;
        iRobotCol               = grid[0].length / 2;
        startRow                = iRobotRow;
        startCol                = iRobotCol;
        distance                = 0.0f;
        totalDist               = 0.0f;
        bGameOver               = false;

        initGrid();

        Resources res = game.getContext().getResources();

        scoreLabel.SetText(res.getString(R.string.score) + score);
        SetTimeText(time, timeLabel);
    }

    @Override
    public void Exit() {
        robot.stop();
        robot.getSensorControl().disableSensors();
        robot.enableStabilization(true);
        robot.setBackLedBrightness(0.0f);
        sensorManager.unregisterListener(this);
    }

    @Override
    public void Draw(Canvas c) {
        c.save();

        GameView._paint.setStyle(Paint.Style.FILL);
        c.drawARGB(255, 0, 0, 0);

        DrawGrid(c);

        titleLabel.Draw(c);
        scoreLabel.Draw(c);
        timeLabel.Draw(c);

        c.restore();
    }

    @Override
    public boolean Update(int dtMS) {
        if (bMoving && !bGameOver) {
            Log.d("Sphero", "distance: " + distance + "   heading: " + heading);

            if (spinUpTimeMS > 0) {
                if (velocityX * velocityX + velocityY * velocityY > SPEED_THRESH * SPEED_THRESH) {
                    spinUpTimeMS = 0;
                }
            }
            else {
                rollTimeMS -= dtMS;
                rollTimeMS = Math.max(0, rollTimeMS);
            }

            if (rollTimeMS == 0) {
                bMoving = false;
                robot.drive(heading, 0);
            }

//            if (heading == 90.0f) {
//                float dx = Math.abs(robotX - startX);
//                distance -= dx;
//                startX = robotX;
//
//                if (distance <= 0.0f) {
//                    robot.drive(heading, 0);
//                    distance = 0.0f;
//                    bMoving = false;
//                }
//            }
//            else if (heading == 180.0f) {
//                float dy = Math.abs(robotY - startY);
//                distance -= dy;
//                startY = robotY;
//
//                if (distance <= 0.0f) {
//                    robot.drive(heading, 0);
//                    distance = 0.0f;
//                    bMoving = false;
//                }
//            }
//            else if (heading == 270.0f) {
//                float dx = Math.abs(robotX - startX);
//                distance -= dx;
//                startX = robotX;
//
//                if (distance <= 0.0f) {
//                    robot.drive(heading, 0);
//                    distance = 0.0f;
//                    bMoving = false;
//                }
//            }
//            else {
//                // heading == 0.0f;
//                float dy = Math.abs(robotY - startY);
//                distance -= dy;
//                startY = robotY;
//
//                if (distance <= 0.0f) {
//                    robot.drive(heading, 0);
//                    distance = 0.0f;
//                    bMoving = false;
//                }
//            }
        }

        updateColor();

        if (time > 0) {
            time -= dtMS;

            if (time <= 0) {
                time = 0;
                endGame();
            }

            SetTimeText(time, timeLabel);
        }

        if (!bGameOver) {
            int curRow = getRobotRow();
            int curCol = getRobotCol();

            bGameOver = checkMove(curRow, curCol);
        }

        SetTimeText(time, timeLabel);

        return true;
    }

    @Override
    public boolean OnTouch(View v, MotionEvent e) {
        boolean bDoDrive = false;

        if (!titleLabel.OnTouch(e) && !bMoving) {
            if (!bStarted) {
                robot.setZeroHeading();
                bStarted = true;
                time = GAME_TIME;
            }

            e.getPointerCoords(0, pointerCoords);
            int iRow = getRowFromTouch(Math.round(pointerCoords.y));
            int iCol = getColFromTouch(Math.round(pointerCoords.x));

            if (iRow >= 0 && iCol >= 0 && iRow < grid.length && iCol < grid[0].length) {
                if (iRobotRow == iRow && iRobotCol != iCol) {
                    if (iCol > iRobotCol) {
                        heading = 90.0f;
                    }
                    else {
                        heading = 270.0f;
                    }

//                    distance = Math.abs(iRobotCol - iCol) * LOCATOR_DIST_PER_GRID;
                    rollTimeMS = Math.abs(iRobotCol - iCol) * DRIVE_TIME_PER_GRID_MS;
                    bDoDrive = true;
                }
                else if (iRobotRow != iRow && iRobotCol == iCol) {
                    if (iRow > iRobotRow) {
                        heading = 180.0f;
                    }
                    else {
                        heading = 0.0f;
                    }

//                    distance = Math.abs(iRobotRow - iRow) * LOCATOR_DIST_PER_GRID;
                    rollTimeMS = Math.abs(iRobotRow - iRow) * DRIVE_TIME_PER_GRID_MS;
                    bDoDrive = true;
                }

                if (bDoDrive) {
                    startX = robotX;
                    startY = robotY;
                    Log.d("Sphero", ">>> startXX: " + startX + "   startY: " + startY);

                    robot.drive(heading, SPEED);

                    startRow = iRobotRow;
                    startCol = iRobotCol;

//                    totalDist = distance;
                    totalRollTimeMS = rollTimeMS;
                    spinUpTimeMS = SPINUP_TIME;

                    iRobotRow = iRow;
                    iRobotCol = iCol;
                    bMoving = true;
                }
            }
        }

        return true;
    }

    @Override
    public void HandleSensorData(DeviceSensorAsyncMessage message) {
        LocatorData locData = (message.getAsyncData().get(0)).getLocatorData();
        robotX = locData.getPositionX();
        robotY = locData.getPositionY();

        velocityX = locData.getVelocityX();
        velocityY = locData.getVelocityY();

        Log.d("Sphero", "velocityX: " + velocityX + "   velocityY: " + velocityY);
    }

    public void HandleCollisionMessage(CollisionDetectedAsyncData colData) {
        int impactX = colData.getImpactPower().x;
        int impactY = colData.getImpactPower().y;

        if (impactX * impactX + impactY * impactY > COLLISION_THRESH_LG * COLLISION_THRESH_LG) {
            // Large collision.
        }
        else if (impactX * impactX + impactY * impactY > COLLISION_THRESH * COLLISION_THRESH) {
            // Medium collision.
        }
        else {
            // Small collision.
        }
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
        }
    }

    // IWidgetListener -----------------------------------------------------------------------------
    public boolean OnWidgetTouchStart(WidgetBase widget, int localX, int localY) {
        return true;
    }

    public boolean OnWidgetTouchEnd(WidgetBase widget, int localX, int localY) {
        game.setState("GameStateMainMenu");

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
    private void DebugSensorData() {
        WidgetLabel accelLabel = options.elementAt(0);
        assert(accelLabel != null);

        Resources res = game.getContext().getResources();
        accelLabel.SetText("");
    }

    private void SetDebugText(int index, int resID, String strData) {
        WidgetLabel accelLabel = options.elementAt(index);
        Resources res = game.getContext().getResources();
        accelLabel.SetText(res.getString(resID) + ": " + strData);
    }

    private void initGrid() {
        int iRow = 0;
        int iCol = 0;

        for (iRow = 0; iRow < grid.length; ++iRow) {
            for (iCol = 0; iCol < grid[0].length; ++iCol) {
                grid[iRow][iCol] = ID_EMPTY;
            }
        }

        placeObject(ID_TREASURE_BLUE);
        placeObject(ID_TREASURE_GREEN);
        placeObject(ID_MINE_RED);
    }

    private void updateColor() {
        int red = 0;
        int green = 0;
        int blue = 0;
        int iRow = 0;
        int iCol = 0;
        float p = distance / Math.max(totalDist, EPSILON);

        if (bGameOver) {
            robot.setLed(0, 0, 0);
        }
        else {
            int curCol = getRobotCol();
            int curRow = getRobotRow();

            if (curRow >= 0 && curRow < grid.length && curCol >= 0 && curCol < grid[0].length) {
                for (iRow = 0; iRow < grid.length; ++iRow) {
                    if (grid[iRow][curCol] == ID_TREASURE_BLUE) {
                        blue = 255;
                    } else if (grid[iRow][curCol] == ID_TREASURE_GREEN) {
                        green = 255;
                    } else if (grid[iRow][curCol] == ID_MINE_RED) {
                        red = 255;
                    }
                }

                for (iCol = 0; iCol < grid[0].length; ++iCol) {
                    if (grid[curRow][iCol] == ID_TREASURE_BLUE) {
                        blue = 255;
                    } else if (grid[curRow][iCol] == ID_TREASURE_GREEN) {
                        green = 255;
                    } else if (grid[curRow][iCol] == ID_MINE_RED) {
                        red = 255;
                    }
                }
            }

            robot.setLed((float) red / 255.0f, (float) green / 255.0f, (float) blue / 255.0f);
        }
    }

    private void placeObject(int objectID) {
        int iRow = 0;
        int iCol = 0;
        boolean bPlaced = false;
        int iCell = 0;
        Point cell = null;

        randomizeGridCells();

        while (!bPlaced && !bGameOver) {
            cell = gridCells.elementAt(iCell);
            iRow = cell.y;
            iCol = cell.x;

            while (iRow == iRobotRow && iCol == iRobotCol) {
                iCell += 1;
                cell = gridCells.elementAt(iCell);
                iRow = cell.y;
                iCol = cell.x;
                if (iCell == gridCells.size()) {
                    endGame();
                    break;
                }
            }

            if (!bGameOver) {
                boolean bAbort = false;

                if (grid[iRow][iCol] == ID_EMPTY) {
                    for (int iRowOffset = -1; !bAbort && iRowOffset <= 1; ++iRowOffset) {
                        for (int iColOffset = -1; iColOffset <= 1; ++iColOffset) {
                            if (iRow + iRowOffset >= 0 &&
                                    iCol + iColOffset >= 0 &&
                                    iRow + iRowOffset < grid.length &&
                                    iCol + iColOffset < grid[0].length &&
                                    grid[iRow + iRowOffset][iCol + iColOffset] != ID_EMPTY) {
                                bAbort = true;
                                break;
                            }
                        }
                    }

                    if (!bAbort) {
                        grid[iRow][iCol] = objectID;
                        bPlaced = true;
                    }
                }

                iCell += 1;
                if (!bPlaced && iCell == gridCells.size()) {
                    endGame();
                }
            }
        }
    }

    private void DrawGrid(Canvas c) {
        float rowHeight = (float)game.height() / (float)grid.length;
        float rowWidth = rowHeight;
        int y0 = (int)(game.height() * 0.5f - rowHeight * grid.length * 0.5f);
        int x0 = (int)(game.width() * 0.5f - rowWidth * grid[0].length * 0.5f);

        GameView._paint.setStyle(Paint.Style.STROKE);
        GameView._paint.setColor(Color.GREEN);
        GameView._paint.setStrokeWidth(1.0f);

        for (int i=0; i<=grid[0].length; ++i) {
            int xOffset = x0 + (int)(rowWidth * i);
            c.drawLine(xOffset, y0, xOffset, y0 + rowHeight * grid.length, GameView._paint);
        }

        for (int i=0; i<=grid.length; ++i) {
            int yOffset = y0 + (int)(rowHeight * i);
            c.drawLine(x0, yOffset, x0 + rowWidth * grid[0].length, yOffset, GameView._paint);
        }

        if ( bGameOver) {
            GameView._paint.setStyle(Paint.Style.FILL);
            for (int iRow = 0; iRow < grid.length; ++iRow) {
                for (int iCol = 0; iCol < grid[0].length; ++iCol) {
                    int left = (int) (x0 + iCol * rowWidth) + 1;
                    int top = (int) (y0 + iRow * rowHeight) + 1;
                    if (grid[iRow][iCol] == ID_TREASURE_BLUE) {
                        GameView._paint.setColor(Color.BLUE);
                        c.drawRect(left, top, left + rowWidth - 2, top + rowHeight - 2, GameView._paint);
                    } else if (grid[iRow][iCol] == ID_TREASURE_GREEN) {
                        GameView._paint.setColor(Color.GREEN);
                        c.drawRect(left, top, left + rowWidth - 2, top + rowHeight - 2, GameView._paint);
                    } else if (grid[iRow][iCol] == ID_MINE_RED) {
                        GameView._paint.setColor(Color.RED);
                        c.drawRect(left, top, left + rowWidth - 2, top + rowHeight - 2, GameView._paint);
                    }
                }
            }
        }

        GameView._paint.setStyle(Paint.Style.STROKE);
        GameView._paint.setStrokeWidth(2.0f);
        GameView._paint.setColor(Color.BLUE);
        int left = Math.round(x0 + iRobotCol * rowWidth);
        int top = Math.round(y0 + iRobotRow * rowHeight);
        c.drawArc(left, top, left + rowWidth - 1, top + rowHeight - 1, 0.0f, 360.0f, true, GameView._paint);

        float p = distance / Math.max(totalDist, EPSILON);

        GameView._paint.setColor(Color.WHITE);
        int curCol = getRobotCol();
        int curRow = getRobotRow();
        left = Math.round(x0 + curCol * rowWidth);
        top = Math.round(y0 + curRow * rowHeight);
        c.drawArc(left, top, left + rowWidth - 1, top + rowHeight - 1, 0.0f, 360.0f, true, GameView._paint);
    }

    private int getRowFromTouch(int touchY) {
        float rowHeight = (float)game.height() / (float)grid.length;
        int row = (int)(touchY / rowHeight);

        return row;
    }

    private int getColFromTouch(int touchX) {
        float rowHeight = (float)game.height() / (float)grid.length;
        float rowWidth = rowHeight;

        int x0 = (int)(game.width() * 0.5f - rowWidth * grid[0].length * 0.5f);

        int col = (int)((touchX - x0) / rowWidth);

        return col;
    }

    private boolean checkMove(int iRow, int iCol) {
        if (grid[iRow][iCol] == ID_MINE_RED) {
            // End the game.
            game.playSound(R.raw.destroy);
            endGame();
        }
        else if (grid[iRow][iCol] == ID_TREASURE_BLUE ||
                 grid[iRow][iCol] == ID_TREASURE_GREEN) {
            score += TREASURE_SCORE;
            scoreLabel.SetText(game.getContext().getResources().getString(R.string.score) + score);
            robot.setLed(1.0f, 1.0f, 1.0f);
            game.playSound(R.raw.collect);

            if (grid[iRow][iCol] == ID_TREASURE_BLUE) {
                grid[iRow][iCol] = ID_EMPTY;
                placeObject(ID_TREASURE_BLUE);
            }
            else {
                grid[iRow][iCol] = ID_EMPTY;
                placeObject(ID_TREASURE_GREEN);
            }

            placeObject(ID_MINE_RED);
        }

        return bGameOver;
    }

    private void randomizeGridCells() {
        for (int i=0; i<gridCells.size(); ++i) {
            int cell = (int)Math.floor(Math.random() * (gridCells.size() - i)) + i;
            Point temp = gridCells.elementAt(i);
            gridCells.set(i, gridCells.elementAt(cell));
            gridCells.set(cell, temp);
        }
    }

    private void endGame() {
        bGameOver = true;
        robot.drive(heading, 0);
    }

    private int getRobotRow() {
        int row = -1;
//        float p = distance / Math.max(totalDist, EPSILON);
        float p = (float)rollTimeMS / (float)Math.max(totalRollTimeMS, 1);

        row = (int)Math.floor(p * startRow + (1.0f - p) * iRobotRow);

        row = Math.max(Math.min(row, grid.length - 1), 0);

        return row;
    }

    private int getRobotCol() {
        int col = -1;
        // float p = distance / Math.max(totalDist, EPSILON);
        float p = (float)rollTimeMS / (float)Math.max(totalRollTimeMS, 1);

        col = (int)Math.floor(p * startCol + (1.0f - p) * iRobotCol);

        col = Math.max(Math.min(col, grid[0].length - 1) , 0);

        return col;
    }
}
