package app.pearson.gameengine;


/**
 * Created by MikeGlidden on 3/20/2018.
 *
 * Use to create and position GUI Objects on the screen
 */
public class GUI
{
    protected String currentFile;
    protected int totalSprites;
    protected int currentSprite;
    private Texture image;
    private Sprite sprite;
    private boolean setImage;

    public GUI()
    {
        this(null, 1, false);
    }
    public GUI(String fileName, int numImage, boolean setScreenSize)
    {
        image = new Texture(Engine.getEngine());
        sprite = new Sprite(Engine.getEngine());

        setImage(fileName, numImage, setScreenSize, setScreenSize);
    }

    public GUI(String fileName, boolean stretchHorizontal, boolean stretchVertical)
    {
        image = new Texture(Engine.getEngine());
        sprite = new Sprite(Engine.getEngine());

        setImage(fileName, 1, stretchHorizontal, stretchVertical);
    }

    // Set the texture of the image and adjust the size if needed
    public void setImage(String fileName, int numImage, boolean horizontal, boolean vertical)
    {
        if (fileName != null && image.loadFromAsset(fileName))
        {
            totalSprites = numImage;
            currentFile = fileName;
            currentSprite = 1;

            // Set the size horizontally, vertically, or both
            if (horizontal && !vertical)
                image.setSize(new Vector2(Engine.getScreenSize().getX(), 10.0f), false);
            else if (vertical && !horizontal)
                image.setSize(new Vector2(10.0f, Engine.getScreenSize().getY()), false);
            else if (vertical && horizontal)
                image.setSize(Engine.getScreenSize(), false);

            // Set the texture of this sprite
            sprite.setTexture(image);
            setImage = true;
        }
    }



    /**
     * Loads the next image when called
     */
    protected void loadImage()
    {
        if (currentFile != null && image.loadFromAsset(currentFile))
        {
            sprite.setTexture(image);
            setImage = true;
        }
    }

    protected void loadImage(Sprite s)
    {
        if (currentFile != null)
        {
            // Set the sprites position before we switch sprites
            s.setPosition(sprite.position);

            sprite = s;
            image = s.getTexture();
            setImage = true;
        }
    }

    /**
     * Set the position of the GUI Element on the screen
     * @param x
     * @param x
     */
    public void setPosition(float x, float y)
    {
        Vector2 point = new Vector2(x, y);

        if (setImage)
        {
            sprite.setPosition(point);
        }
    }

    public void setPosition(Vector2 vec)
    {
        setPosition(vec.getX(), vec.getY());
    }

    /**
     * Returns the GUI Element
     * @return
     */
    public Sprite getSprite()
    {
        return sprite;
    }

    public Texture getTexture()
    {
        return image;
    }

}
