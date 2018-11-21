package com.android.fishrage.mikeglidden.fishrage;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.KeyEvent;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Formatter;

import app.pearson.gameengine.*;

public class RageMain extends Engine
{
    // Images and UI
    private GUI backgroundUI;
    private GUI mainMenu;
    private GUI gameOver;
    private GUI wallLeft;
    private GUI wallRight;
    private GUI wallTop;
    private GUI wallBottom;
    private Button upButton;
    private Button downButton;
    private Button restart;
    private Button start;
    private Button pause;
    private PCharacter player;
    private Timer roundTime;

    // Screen information
    private Vector2 screenSize;

    // Game Information
    private Vector2 moveDir;
    private boolean hitBottom;
    private boolean startGame;
    private boolean canMove;
    private boolean savedGame;
    private MovementDir lastHit;

    private NumberFormat formatter = new DecimalFormat("######0.00");

    @Override
    public void init()
    {
        setScreenOrientation(ScreenModes.LANDSCAPE);
        moveDir = new Vector2(0.0f, 0.0f);
        hitBottom = false;
        startGame = false;
        canMove = false;
    }

    @Override
    public void load()
    {
        // Get the size of the screen
        screenSize = getScreenSize();
        // Load the background UI
        backgroundUI = new GUI("Background/Tank.png", 1, true);
        mainMenu = new GUI("Background/Rage.png", 1, true);
        gameOver = new GUI("Background/GameOver.png", 1, true);

        // Load the Start and Replay buttons
        start = new Button("Buttons/Play.png");
        restart = new Button("Buttons/Replay.png");
        pause = new Button("Buttons/Pause.png");

        // Set buttons positions based on their center
        start.setPosition((getScreenWidth() / 2.0f) - (start.getSprite().getTexture().getBitmap().getWidth() / 2.0f), (getScreenHeight() / 2.0f) - (start.getSprite().getTexture().getBitmap().getHeight() / 2.0f));
        restart.setPosition((getScreenWidth() / 2.0f) - (restart.getSprite().getTexture().getBitmap().getWidth() / 2.0f), (getScreenHeight() / 1.25f) - (restart.getSprite().getTexture().getBitmap().getHeight() / 2.0f));
        pause.setPosition((getScreenWidth() / 2.0f) - (restart.getSprite().getTexture().getBitmap().getWidth() / 2.0f), 20);

        // Sets the methods that these buttons will call when pressed and the values the "whenVar" variables need to be at to call the method
        pause.setCallMethod("setPause", true);


        // Load the up and down buttons
        upButton = new Button("Buttons/UpArrow.png");
        downButton = new Button("Buttons/DownArrow.png");

        // Initialize the button positions
        upButton.setPosition(getScreenWidth() - 300.0f, getScreenHeight() - 350.0f);
        downButton.setPosition(100.0f, getScreenHeight() - 350.0f);

        // Load the walls of the tank
        wallLeft = new GUI("Background/WallLeft.png", false, true);
        wallRight = new GUI("Background/WallRight.png", false, true);
        wallTop = new GUI("Background/WallTop.png", true, false);
        wallBottom = new GUI("Background/WallBottom.png", true, false);

        // Initialize the wall positions
        wallLeft.setPosition(0, 0);
        wallRight.setPosition(getScreenWidth(), 0);
        wallTop.setPosition(0.0f, 0);
        wallBottom.setPosition(0.0f, getScreenHeight()-95.0f);

        // Load the players default image, number of animations, speed, and health
        player = new PCharacter("Fish/rageFish1.png", 3, 20.0f, 100, true);

        player.setPosition(new Vector2((getScreenWidth() / 2.0f) - (player.getTexture().getBitmap().getWidth() / 2.0f), (getScreenHeight() / 2.0f) - (player.getTexture().getBitmap().getHeight() / 2.0f)));

        roundTime = new Timer();

        // Default Values
        setFrameRate(60);
    }

    @Override
    public void draw()
    {
        if (!startGame)
        {
            mainMenu.getSprite().draw();
            start.getSprite().draw();
        }
        else if (player.getHealth() > 0)
        {
            // Draw the background
            backgroundUI.getSprite().draw();

            // Draw the player
            player.getSprite().draw();

            // Draw the buttons in their current states
            upButton.getSprite().draw();
            downButton.getSprite().draw();
            pause.getSprite().draw();

            // Draw the Walls
            wallLeft.getSprite().draw();
            wallRight.getSprite().draw();
            wallTop.getSprite().draw();
            wallBottom.getSprite().draw();

            drawText("Points: " + formatter.format(player.getPoints()), 50, 110);
            drawText("High Score: " + formatter.format(player.getHighScore()), 50, 165);
            drawText("Time: " + formatter.format(roundTime.getSeconds()), 50, 220);
        }
        else
        {
            gameOver.getSprite().draw();
            restart.getSprite().draw();
        }

        drawText("FrameRate: " + Engine.toString(Engine.getEngine().getFrameRate()), 50, 55);
    }

    @Override
    public void update()
    {

        if (!startGame)
        {
            if (canMove)
                canMove = false;

            // Check for the start button
            if (start.getTrigger())
            {
                start.resetTrigger();
                roundTime.resetTimer();
                startGame = true;
            }
        }
        else if (player.getHealth() > 0)
        {
            // Reset if we have hit the bottom
            if (hitBottom)
                hitBottom = false;
            // Enable movement
            if (!canMove)
                canMove = true;

            // Calculate the movement directions
            calcDirection();

            // Check if the player hit any walls
            checkCollisions();

            // Calculate any movement the player made this frame
            player.calcMovement(moveDir, hitBottom);
        }
        else // Player is dead, show death screen
        {
            // Stop player from moving
            if (canMove)
                canMove = false;

            // Save the game if needed
            if (!savedGame)
            {
                savedGame = true;
                saveHighScore(player.getHighScore());
            }

            // Check for restart button
            if (restart.getTrigger())
            {
                saveHighScore(player.getHighScore());
                savedGame = false;
                restart.resetTrigger();
                roundTime.resetTimer();
                player.setPosition(new Vector2((getScreenWidth() / 2.0f) - (player.getTexture().getBitmap().getWidth() / 2.0f), (getScreenHeight() / 2.0f) - (player.getTexture().getBitmap().getHeight() / 2.0f)));
                player.resetCharacter();
            }
        }
    }

    public void checkCollisions()
    {
        // If we try to move left or right while against a wall
        if (player.getMovementDir() != MovementDir.LEFT && Engine.checkCollision(player.getSprite(), wallRight.getSprite()))
        {
            moveDir.setPos(0.0f, moveDir.getY());

            lastHit = MovementDir.RIGHT;

            if (player.getLastWall() != PCharacter.LastWall.RIGHT)
            {
                // Add points to the player if we hit the opposite wall as last time (Based on the players position from the top of the tank)
                player.addPoints(0.01f * (player.getSprite().position.getY() - 600));
                player.setLastWall(PCharacter.LastWall.RIGHT);
                Log.w("DIRECTION: ", "Height " + Engine.toString(player.getSprite().position.getY()));
            }
        }
        else if (player.getMovementDir() != MovementDir.RIGHT && Engine.checkCollision(player.getSprite(), wallLeft.getSprite()))
        {
            moveDir.setPos(0.0f, moveDir.getY());

            lastHit = MovementDir.LEFT;

            if (player.getLastWall() != PCharacter.LastWall.LEFT)
            {
                // Add points to the player if we hit the opposite wall as last time (Based on the players position from the top of the tank)
                player.addPoints(0.01f * (player.getSprite().position.getY() - 600));
                player.setLastWall(PCharacter.LastWall.LEFT);
                Log.w("DIRECTION: ", "Height " + Engine.toString(player.getSprite().position.getY()));
            }
        }

        // If we try to move up or down while against a wall
        if (player.getMovementDirVertical() != MovementDir.DOWN && Engine.checkCollision(player.getSprite(), wallTop.getSprite()))
        {
            moveDir.setPos(moveDir.getX(), 0.0f);

            lastHit = MovementDir.UP;
        }
        else if (player.getMovementDirVertical() != MovementDir.UP && Engine.checkCollision(player.getSprite(), wallBottom.getSprite()))
        {
            moveDir.setPos(moveDir.getX(), 0.0f);

            lastHit = MovementDir.DOWN;
            hitBottom = true;
            player.kill();
        }
        else if (Engine.checkCollision(player.getSprite(), wallBottom.getSprite()))
        {
            hitBottom = true;
            player.kill();
        }

        // Attempt to set the new high score
        player.setHighScore();
    }

    public void calcDirection()
    {

        MovementDir mDirV;

        // Determine the on screen directional buttons
        if (upButton.getState() == Button.State.PRESSED)
        {
            moveDir.setPos(moveDir.getX(), -1.0f);
            mDirV = MovementDir.UP;
        }
        else if (downButton.getState() == Button.State.PRESSED)
        {
            moveDir.setPos(moveDir.getX(), 1.0f);
            mDirV = MovementDir.DOWN;
        }
        else
        {
            moveDir.setPos(moveDir.getX(), 0.0f);
            mDirV = MovementDir.NONE;
        }

        player.setMovingVertical(mDirV);
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        float r[] = new float[9];
        float orientation[] = new float[9];
        float roll;
        MovementDir mDir;

        if (isLoaded() && startGame && canMove)
        {
            // Do not attempt a sensor change if this is an emulator
            if (isEmulator())
                return;

            if (event.values == null)
                return;

            int sensorType = event.sensor.getType();
            switch (sensorType) {
                case Sensor.TYPE_ACCELEROMETER:
                    accel = event.values;
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    geomag = event.values;
                    break;
                default:
                    Log.w("SENDOR: ", "Unknown sensor type " + sensorType);
                    return;
            }
            if (accel == null) {
                Log.w("SENDOR: ", "mGravity is null");
                return;
            }
            if (geomag == null) {
                Log.w("SENDOR: ", "mGeomagnetic is null");
                return;
            }

            if (!SensorManager.getRotationMatrix(r, null, accel, geomag))
            {
                Log.w("SENSOR FAILED!", "getRotationMatrix() failed");
                return;
            }

            SensorManager.getOrientation(r, orientation);
            roll = orientation[1];

            if (roll < -0.1f)
            {
                roll *= 4.0f;
                moveDir.setPos(-roll, moveDir.getY());
                mDir = MovementDir.RIGHT;
//                Log.w("DIRECTION: ", "Right at " + Engine.toString(roll));
            }
            else if (roll > 0.1f)
            {
                roll *= 4.0f;
                moveDir.setPos(-roll, moveDir.getY());
                mDir = MovementDir.LEFT;
//                Log.w("DIRECTION: ", "Left at " + Engine.toString(roll));
            }
            else
            {
                moveDir.setPos(0.0f, moveDir.getY());
                mDir = MovementDir.NONE;
            }

            player.setMoving(mDir);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {
        boolean val = false;

        try
        {
            if (event.getAction()==KeyEvent.ACTION_DOWN)
            {
                val = KeyDown(event.getKeyCode(), event);
            }
        }
        catch (Exception ex)
        {
            return false;
        }

        return val;
    }


    /**
     * Runs when we are pressing a key down and the dispatchKeyEvent is called
     * @param keyCode
     * @param event
     * @return
     */
    public boolean KeyDown(int keyCode, KeyEvent event)
    {
        boolean val = false;
        MovementDir mDir = player.getMovementDir();

        // Only attempt to move with the keys if this is the emulator
        if (isEmulator())
        {
            // If we are pressing the Right arrow
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)
            {
                // Move the player Right (Stop moving if we go from Left to Right)
                moveDir.setPos(1.0f, moveDir.getY());

                val = true;
            }
            // If we are pressing the Left arrow
            else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT)
            {
                // Move the player Left (Stop moving if we go from Right to Left)
                moveDir.setPos(-1.0f, moveDir.getY());

                val = true;
            }

            // Which direction are we moving
            if (moveDir.getX() == 0)
                mDir = MovementDir.NONE;
            else if (moveDir.getX() > 0)
                mDir = MovementDir.RIGHT;
            else if (moveDir.getX() < 0)
                mDir = MovementDir.LEFT;

            // Is the player moving?
            player.setMoving(mDir);
        }

        return val;
    }

}
