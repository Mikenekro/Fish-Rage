package app.pearson.gameengine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Ref;
import java.util.ArrayList;
import java.util.List;


import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.renderscript.Float2;
import android.renderscript.Float3;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.view.View.OnTouchListener;


public abstract class Engine extends Activity implements Runnable, OnTouchListener, SensorEventListener {

    //************************************************
    //Private data members
    //************************************************
    private SurfaceView p_view;
    private Canvas p_canvas;
    private Thread p_thread;
    private boolean p_running, p_paused;
    private int p_pauseCount;
    private Paint p_paintDraw, p_paintFont;
    private Typeface p_typeface;
    private Point[] p_touchPoints;
    private int p_numPoints;

    // Current frames
    private static int p_frameCount;
    private float p_gravity;

    private long p_preferredFrameRate, p_sleepTime;

    private int screenWidth;
    private int screenHeight;

    private ArrayList<Button> pressed;

    private static boolean loaded;
    private static Engine eng;
    private static boolean keyPressed;
    private static boolean emulator;
    private static String saveFile;

    // Sensor Information
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGeomagnetic;
    public float[] accel;
    public float[] geomag;


    //***********************************************
    // * Engine constructor
    //***********************************************
    public Engine() {
        Log.d("Engine", "Engine constructor");

        pressed = new ArrayList<>();
        mSensorManager = null;
        mAccelerometer = null;
        mGeomagnetic = null;
        p_view = null;
        p_canvas = null;
        p_thread = null;
        p_running = false;
        p_paused = false;
        p_paintDraw = null;
        p_paintFont = null;
        p_numPoints = 0;
        p_typeface = null;
        p_preferredFrameRate = 40;
        p_sleepTime = 1000 / p_preferredFrameRate;
        p_pauseCount = 0;
        p_gravity = 5.0f;
        saveFile = "Highscore.txt";
        eng = this;
    }

    /**
     * Abstract methods that must be implemented in the sub-class!
     */
    public abstract void init();

    public abstract void load();

    public abstract void draw();

    public abstract void update();

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.d("Engine", "Engine.onCreate start");

        //disable the title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);


        //set default screen orientation
        setScreenOrientation(ScreenModes.LANDSCAPE);

        /**
         * Call abstract init method in sub-class!
         */

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;

        if (!Build.FINGERPRINT.contains("generic")) {
            // Attempt to use the sensors
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mGeomagnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            emulator = false;
        } else {
            // Use the Keyboard instead of sensors for movement
            mSensorManager = null;
            mAccelerometer = null;
            mGeomagnetic = null;
            emulator = true;
        }

        init();

        //create the view object
        p_view = new SurfaceView(this);
        setContentView(p_view);

        //turn on touch listening
        p_view.setOnTouchListener(this);

        //create the points array
        p_touchPoints = new Point[5];
        for (int n = 0; n < 5; n++) {
            p_touchPoints[n] = new Point(0, 0);
        }

        //create Paint object for drawing styles
        p_paintDraw = new Paint();
        p_paintDraw.setColor(Color.WHITE);

        //create Paint object for font settings
        p_paintFont = new Paint();
        p_paintFont.setColor(Color.WHITE);
        p_paintFont.setTextSize(24);

        /**
         * Call abstract load method in sub-class!
         */
        load();

        //launch the thread
        p_running = true;
        p_thread = new Thread(this);
        p_thread.start();

        p_view.setKeepScreenOn(true);
        p_view.setFocusable(true);

        loaded = true;
        Log.d("Engine", "Engine.onCreate end");
    }

    public boolean isEmulator() {
        return emulator;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.engine, menu);
        return true;
    }

    @Override
    public void run()
    {
        Log.d("Engine", "Engine.run start");

        Timer frameTimer = new Timer();
        int frameCount = 0;
        long startTime = 0;
        long timeDiff = 0;

        while (p_running)
        {

            /**
             * Process frame only if not paused.
             */
            if (p_paused)
                continue;

            /**
             * Calculate frame rate
             */
            frameCount++;
            startTime = frameTimer.getElapsed();
            if (frameTimer.stopwatch(1000))
            {
                p_frameCount = frameCount;
                frameCount = 0;

                //reset touch input count
                p_numPoints = 0;
            }


            /**
             * Call abstract update method in sub-class!
             */
            update();

            /**
             * Rendering section, lock the canvas.
             * Only proceed if the SurfaceView is valid.
             */
            if (beginDrawing())
            {

                p_canvas.drawColor(Color.BLUE);

                /**
                 * Call abstract draw method in sub-class!
                 */
                draw();

                /**
                 * Complete the rendering process by
                 * unlocking the canvas.
                 */
                endDrawing();
            }

            /**
             * Calculate frame update time and sleep if necessary.
             */
            timeDiff = frameTimer.getElapsed() - startTime;
            long updatePeriod = p_sleepTime - timeDiff;
            if (updatePeriod > 0) {
                try {
                    Thread.sleep(updatePeriod);
                } catch (InterruptedException e) {

                }
            }

        }
        Log.d("Engine", "Engine.run end");

        System.exit(RESULT_OK);
    }

    /**
     * BEGIN RENDERING
     * Verify that the surface is valid and then lock the canvas.
     */
    private boolean beginDrawing()
    {
        if (!p_view.getHolder().getSurface().isValid()) {
            return false;
        }
        p_canvas = p_view.getHolder().lockCanvas();
        return true;
    }

    /**
     * END RENDERING
     * Unlock the canvas to free it for future use.
     */
    private void endDrawing() {
        p_view.getHolder().unlockCanvasAndPost(p_canvas);
    }

    /**
     * Activity.onResume event method
     */
    @Override
    public void onResume()
    {
        Log.d("Engine", "Engine.onResume");
        super.onResume();
        p_paused = false;
        if (!emulator) {
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
            mSensorManager.registerListener(this, mGeomagnetic, SensorManager.SENSOR_DELAY_UI);
        }
    }

    /**
     * Activity.onPause event method
     */
    @Override
    public void onPause()
    {
        Log.d("Engine", "Engine.onPause");
        super.onPause();
        p_paused = true;
        p_pauseCount++;
        if (!emulator)
            mSensorManager.unregisterListener(this);
    }

    /**
     * pauses or resumes the game (Based on if the game is currently paused or playing
     */
    public void setPause()
    {
        if (p_paused)
            onResume();
        else
            onPause();
    }

    /**
     * OnTouchListener.onTouch event method
     */
    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        boolean val = false;


        switch (event.getAction())
        {
            // When we press the button
            case MotionEvent.ACTION_DOWN:

                //count the touch inputs
                p_numPoints = event.getPointerCount();
                if (p_numPoints > 5)
                    p_numPoints = 5;

                //store the input values
                for (int n = 0; n < p_numPoints; n++)
                {
                    p_touchPoints[n].x = (int) event.getX(n);
                    p_touchPoints[n].y = (int) event.getY(n);
                    tryClick(new Vector2(p_touchPoints[n].x, p_touchPoints[n].y));
                }
                val = true;
                break;

            // When we release the button
            case MotionEvent.ACTION_UP:

                for (int i = 0; i < pressed.size(); ++i)
                {
                    pressed.get(i).setState(Button.State.UP);
                }

                pressed.clear();

                val = false;
                break;

            default:
                break;
        }

        return val;
    }

    public static boolean isLoaded()
    {
        return loaded;
    }

    public void tryClick(Vector2 pos)
    {
        boolean touched;
        Button cur;
        ArrayList<Button> instances = Button.getInstances();

        try
        {
            for (int i = 0; i < instances.size(); ++i)
            {
                cur = instances.get(i);
                touched = cur.checkTouch(pos);

                // Press the button
                if (touched)
                {
                    pressed.add(cur);
                    break;
                }
            }

        }
        catch (Exception ex)
        {
            Log.e("CLICK ERROR!", ex.getMessage());
        }
    }

    /**
     * Shortcut methods to duplicate existing Android methods.
     */
    public void fatalError(String msg) {
        Log.e("FATAL ERROR", msg);
        System.exit(0);
    }

    /**
     * Drawing helpers
     */
    public void drawText(String text, int x, int y) {
        p_paintFont.setTextSize(50);
        p_canvas.drawText(text, x, y, p_paintFont);
    }

    /**
     * Engine helper get/set methods for private properties.
     */

    public static Engine getEngine() {
        return eng;
    }


    public static float getGravity() {
        return eng.getPGravity();
    }

    public float getPGravity() {
        return p_gravity;
    }

    public void setGravity(float grav) {
        p_gravity = grav;
    }


    public static boolean isKeyPressed() {
        return keyPressed;
    }

    public static void setKeyPressed(boolean pressed) {
        keyPressed = pressed;
    }

    public SurfaceView getView() {
        return p_view;
    }

    public Canvas getCanvas() {
        return p_canvas;
    }

    public void setFrameRate(int rate) {
        p_preferredFrameRate = rate;
        p_sleepTime = 1000 / p_preferredFrameRate;
    }

    public static int getFrameRate() {
        return p_frameCount;
    }

    public int getTouchInputs() {
        return p_numPoints;
    }

    public Point getTouchPoint(int index) {
        if (index > p_numPoints)
            index = p_numPoints;
        return p_touchPoints[index];
    }

    public void setDrawColor(int color) {
        p_paintDraw.setColor(color);
    }

    public void setTextColor(int color) {
        p_paintFont.setColor(color);
    }

    public void setTextSize(int size) {
        p_paintFont.setTextSize((float) size);
    }

    public void setTextSize(float size) {
        p_paintFont.setTextSize(size);
    }


    public static Vector2 getScreenSize() {
        return eng.getPScreenSize();
    }

    public Vector2 getPScreenSize() {
        Vector2 screenSize = new Vector2();
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();

        // Get the size of the display
        display.getSize(size);

        // Set the size of the Vector2
        screenSize.setX(size.x);
        screenSize.setY(size.y);

        // Return the Vector2
        return screenSize;
    }

    public static Vector2 getCenterScreen() {
        return eng.getPCenterScreen();
    }

    /**
     * Returns the center position of the screen
     *
     * @return
     */
    public Vector2 getPCenterScreen() {
        Vector2 center = getScreenSize();

        center.setX(center.getX() / 2.0f);
        center.setY(center.getY() / 2.0f);

        return center;
    }

    /**
     * Font style helper
     */
    public enum FontStyles {
        NORMAL(Typeface.NORMAL),
        BOLD(Typeface.BOLD),
        ITALIC(Typeface.ITALIC),
        BOLD_ITALIC(Typeface.BOLD_ITALIC);
        int value;

        FontStyles(int type) {
            this.value = type;
        }
    }

    public void setTextStyle(FontStyles style) {
        p_typeface = Typeface.create(Typeface.DEFAULT, style.value);
        p_paintFont.setTypeface(p_typeface);
    }

    /**
     * Screen mode helper
     */
    public enum ScreenModes {
        LANDSCAPE(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE),
        PORTRAIT(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        int value;

        ScreenModes(int mode) {
            this.value = mode;
        }
    }

    public void setScreenOrientation(ScreenModes mode) {
        setRequestedOrientation(mode.value);
    }

    /**
     * Round to a default 2 decimal places
     */
    public double round(double value) {
        return round(value, 2);
    }

    /**
     * Round to any number of decimal places
     */
    public double round(double value, int precision) {
        try {
            BigDecimal bd = new BigDecimal(value);
            BigDecimal rounded = bd.setScale(precision, BigDecimal.
                    ROUND_HALF_UP);
            return rounded.doubleValue();
        } catch (Exception e) {
            Log.e("Engine", "round: error rounding number");
        }
        return 0;
    }

    /**
     * String conversion helpers
     */
    public static String toString(int value) {
        return Integer.toString(value);
    }

    public static String toString(float value) {
        return Float.toString(value);
    }

    public static String toString(double value) {
        return Double.toString(value);
    }

    public String toString(Float2 value) {
        String s = "X:" + round(value.x) + "," +
                "Y:" + round(value.y);
        return s;
    }

    public String toString(Float3 value) {
        String s = "X:" + round(value.x) + "," +
                "Y:" + round(value.y) + "," +
                "Z:" + round(value.z);
        return s;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    /**
     * Checks the collision between two sprites
     *
     * @param obj
     * @param objCol
     * @return
     */
    public static boolean checkCollision(Sprite obj, Sprite objCol)
    {
        boolean collision = false;

        Rect collisionBounds;

        // Get the bounds for each of the sprites
        RectF bounds1 = new RectF(obj.position.getX(), obj.position.getY(),
                obj.position.getX() + obj.getTexture().getBitmap().getWidth(),
                obj.position.getY() + obj.getTexture().getBitmap().getHeight());
        RectF bounds2 = new RectF(objCol.position.getX(), objCol.position.getY(),
                objCol.position.getX() + objCol.getTexture().getBitmap().getWidth(),
                objCol.position.getY() + objCol.getTexture().getBitmap().getHeight());

        try
        {
            // If the rectangles of the bitmaps are overlapping
            if (RectF.intersects(bounds1, bounds2))
            {
                // Get the overlapping bounds
                collisionBounds = getCollisionBounds(bounds1, bounds2);

                // Loop through each pixel, left to right
                for (int i = collisionBounds.left; i < collisionBounds.right; ++i)
                {
                    // Loop through each pixel, top to bottom
                    for (int j = collisionBounds.top; j < collisionBounds.bottom; ++j)
                    {
                        // Return the color of each pixel in the images
                        int sPixel1 = obj.getTexture().getBitmap().getPixel((int) (i - obj.position.getX()), (int) (j - obj.position.getY()));
                        int sPixel2 = objCol.getTexture().getBitmap().getPixel((int) (i - objCol.position.getX()), (int) (j - objCol.position.getY()));

                        // Make sure both pixels have a color value
                        if (isFilled(sPixel1) && isFilled(sPixel2))
                        {
                            // We are colliding with an object
                            collision = true;
                            break;
                        }
                    }

                    // Leave if worlds collide
                    if (collision)
                        break;
                }
            }
        }
        catch (Exception ex)
        {
            collision = true;
        }

        return collision;
    }

    /**
     * Get the collision bounds of both objects
     *
     * @param rect1
     * @param rect2
     * @return
     */
    private static Rect getCollisionBounds(RectF rect1, RectF rect2) {
        int left = (int) Math.max(rect1.left, rect2.left);
        int top = (int) Math.max(rect1.top, rect2.top);
        int right = (int) Math.max(rect1.right, rect2.right);
        int bottom = (int) Math.max(rect1.bottom, rect2.bottom);

        return new Rect(left, top, right, bottom);
    }

    /**
     * Is the pixel part of the image or is it transparent?
     *
     * @param pixel
     * @return
     */
    private static boolean isFilled(int pixel) {
        return pixel != Color.TRANSPARENT;
    }

    /**
     * Saves the games high score to load next time
     *
     * @param highScore
     */
    public static void saveHighScore(float highScore)
    {
        String val = toString(highScore);

        try
        {
            OutputStreamWriter writer = new OutputStreamWriter(getEngine().openFileOutput(saveFile, Context.MODE_PRIVATE));
            writer.write(val);
            writer.close();
            Log.w("SAVING: ", "Saved high score of: " + val);
        } catch (Exception ex) {
            Log.e("SAVE FAILED: ", "Writing high score failed: " + ex.getMessage());
        }
    }

    /**
     * Loads the games high score to load next time
     *
     * @return
     */
    public static float loadHighScore()
    {
        float val = 0.0f;
        String receive = "";

        try
        {
            // The file must exist before we can attempt to load
            if (fileExists(getEngine(), saveFile))
            {
                InputStream input = getEngine().openFileInput(saveFile);

                if (input != null)
                {
                    InputStreamReader isReader = new InputStreamReader(input);
                    BufferedReader buffer = new BufferedReader(isReader);
                    StringBuilder sb = new StringBuilder();

                    while ((receive = buffer.readLine()) != null) {
                        sb.append(receive);
                    }

                    input.close();
                    val = Float.valueOf(sb.toString());
                    Log.w("LOADING: ", "Loaded high score of: " + val);
                }
            }

        } catch (Exception ex) {
            Log.e("LOAD FAILED: ", "Reading high score failed: " + ex.getMessage());
        }

        return val;
    }

    /**
     * Checks if the save file exists
     * @param context
     * @param fileName
     * @return
     */
    public static boolean fileExists(Context context, String fileName)
    {
        boolean exists = true;
        File file;

        try
        {
            file = context.getFileStreamPath(fileName);
            if (file == null || !file.exists())
                exists = false;

        }
        catch (Exception e)
        {
            exists = false;
            Log.e("FILE ERROR: ", e.getMessage());
        }

        return exists;
    }

}
