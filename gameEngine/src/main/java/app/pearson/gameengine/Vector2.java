package app.pearson.gameengine;

/**
 * Created by MikeGlidden on 3/20/2018.
 */

public class Vector2
{
    private float xPos;
    private float yPos;

    public Vector2()
    {
        this(0.0f, 0.0f);
    }
    public Vector2(float x, float y)
    {
        xPos = x;
        yPos = y;
    }

    public float getX()
    {
        return xPos;
    }
    public float getY()
    {
        return  yPos;
    }

    public void setX(float x)
    {
        xPos = x;
    }
    public void setY(float y)
    {
        yPos = y;
    }

    public void setPos(float x, float y)
    {
        xPos = x;
        yPos = y;
    }
}
