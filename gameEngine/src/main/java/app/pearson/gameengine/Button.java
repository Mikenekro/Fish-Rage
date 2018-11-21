package app.pearson.gameengine;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by MikeGlidden on 4/4/2018.
 */

public class Button extends GUI
{
    private static ArrayList<Button> instances;


    private Sprite button;
    private Sprite buttonPressed;
    private State curState;
    private boolean trigger;
    private boolean init;

    private String callMethod;
    private boolean varySprite;
    private boolean varyState;

    public enum State
    {
        UP, PRESSED
    }

    public Button (String fileName)
    {
        super(fileName, 1, false);

        String pressedName = fileName.substring(0, fileName.indexOf(".png")) + "_Pressed.png";

        init = false;
        if (instances == null)
            instances = new ArrayList<>();
        curState = State.UP;

        button = new Sprite(Engine.getEngine());
        buttonPressed = new Sprite(Engine.getEngine());
        callMethod = null;

        button.setFileName(fileName);
        button.loadTexture(fileName);
        buttonPressed.setFileName(pressedName);
        buttonPressed.loadTexture(pressedName);

        currentFile = fileName;
        currentSprite = 1;
        loadImage(button);
        trigger = false;
        init = true;
        instances.add(this);
    }

    public void setState(State buttonState)
    {
        Method m;

        if (buttonState != curState)
        {
            if (curState == State.PRESSED)
            {
                Log.d("Engine", "Button Released: ");
                trigger = true;

                // If we have a method to call after the button is pressed
                if (callMethod != null)
                {
                    try
                    {
                        // Attempt to invoke the callMethod
                        if (varySprite)
                        {
                            setVaryState();
                            getSprite().draw();
                        }

                        Engine.getEngine().setPause();
//                        m = Engine.class.getMethod(callMethod, Void.TYPE);
//                        m.invoke(Engine.getEngine());
                    }
                    catch (Exception ex)
                    {
                        Log.e("Error:", ex.getMessage());


                    }
//                    catch (IllegalAccessException e)
//                    {
//                        Log.e("ILLEGAL_ACCESS: ","Error! " + e.getMessage());
//                        e.printStackTrace();
//                    }
//                    catch (NoSuchMethodException e)
//                    {
//                        Log.e("NO_METHOD: ","Error! " + e.getMessage());
//                        e.printStackTrace();
//                    }
//                    catch (InvocationTargetException e)
//                    {
//                        Log.e("INVOKE_METHOD: ", "Error!" + e.getMessage());
//                        e.printStackTrace();
//                    }
                } // End if CallMethod != null
            }
            else
            {
                Log.d("Engine", "Button Pressed: ");
            }

            curState = buttonState;
            loadImage(getSprite());
        }
    }

    public boolean getTrigger() { return trigger; }
    public void resetTrigger() { trigger = false; }

    public State getState()
    {
        return curState;
    }

    @Override
    public Sprite getSprite()
    {
        if ((curState == State.PRESSED && !varySprite) || (varySprite && varyState))
        {
            currentFile = buttonPressed.getFileName();
            return buttonPressed;
        }
        else
        {
            currentFile = button.getFileName();
            return button;
        }
    }

    public void setVaryState()
    {
        if (varyState)
            varyState = false;
        else
            varyState = true;
    }

    /**
     * Set the position of each Buttons image
     * @param x
     * @param x
     */
    @Override
    public void setPosition(float x, float y)
    {
        Vector2 point = new Vector2(x, y);

        if (init)
        {
            button.setPosition(point);
            buttonPressed.setPosition(point);
        }
    }


    /**
     * Checks if we have touched this button
     * @param pos
     * @return
     */
    public boolean checkTouch(Vector2 pos)
    {
        Sprite s = getSprite();
        boolean touched = false;

        Engine.getEngine().drawText("Checking Touch on " + button.getFileName() , 50, 250);

        // If the entered position falls between each of the specified sprites positions
        if ((pos.getX() >= s.position.getX() &&
                pos.getX() <= s.getTexture().getBitmap().getWidth() + s.position.getX())
            && (pos.getY() >= s.position.getY() &&
                pos.getY() <= s.getTexture().getBitmap().getHeight() + s.position.getY()))
        {
            // Press the button
            setState(State.PRESSED);
            touched = true;
        }
        // If we did not click within the button range and the state is not UP
        else if (curState == State.PRESSED)
        {
            trigger = true;
            // Play the normal State
            setState(State.UP);
        }

        return touched;
    }

    /**
     * Calls this method when the specified boolean equals this value
     * @param method
     */
    public void setCallMethod(String method, boolean vary)
    {
        callMethod = method;
        varySprite = vary;
        varyState = false;
    }

    public static ArrayList<Button> getInstances()
    {
        return instances;
    }


}
