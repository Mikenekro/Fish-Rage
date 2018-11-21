package app.pearson.gameengine;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;


public class Sprite
{
    private Engine p_engine;
    private Canvas p_canvas;
    private Texture p_texture;
    private Paint p_paint;
    private String p_fileName;
    private Vector2 center;
    public Vector2 position;

    public Sprite(Engine engine)
    {
        p_engine = engine;
        p_canvas = null;
        p_texture = new Texture(engine);
        p_paint = new Paint();
        p_paint.setColor(Color.WHITE);
        position = new Vector2(0, 0);
    }

    public void draw()
    {
        p_canvas = p_engine.getCanvas();
        p_canvas.drawBitmap(p_texture.getBitmap(), position.getX(),
                position.getY(), p_paint);
    }

    public void draw(Rect dst)
    {
        p_canvas = p_engine.getCanvas();
        p_canvas.drawBitmap(p_texture.getBitmap(), null,
                dst, p_paint);
    }

    /**
     * Color manipulation methods
     */
    public void setColor(int color) {
        p_paint.setColor(color);
    }

    public void setPaint(Paint paint) {
        p_paint = paint;
    }

    /**
     * common get/set methods
     */
    public void loadTexture(String filename)
    {
        if (p_texture.loadFromAsset(filename))
        {
            // anything we need to do after loading the bitmap
        }
    }

    public void setTexture(Texture texture)
    {
        p_texture = texture;
    }

    public Texture getTexture() {
        return p_texture;
    }

    public void setFileName(String name)
    {
        p_fileName = name;
    }
    public String getFileName()
    {
        return p_fileName;
    }

    public void setPosition(Vector2 position)
    {
            this.position = position;
    }

    public Vector2 getPosition() {
        return position;
    }

    public Vector2 getCenter()
    {
        return center;
    }

}

