package app.pearson.gameengine;

        import android.util.Log;

/**
 * Created by MikeGlidden on 3/20/2018.
 *
 * The Character Class is used to create and move a character in the game
 */
public class PCharacter extends GUI
{
    public enum LastWall
    {
        LEFT, NONE, RIGHT
    }

    private float speed;
    private float baseSpeed;
    private int health;
    private int baseHealth;
    private float points;
    private float basePoints;
    private static float lastHighScore;
    private float highScore;
    private int timeBetweenAnim;
    private int lastFrameCount;

    private Vector2 position;
    private Sprite[] anims; // Array of each animation image for this Character
    private Timer animTime;
    private boolean lastRight; // Were we moving right or left when we left off?
    private boolean isMoving; // Are we currently moving?
    private Vector2 noMove; // which directions are we prevented from moving in?
    private MovementDir movementDir; // Which direction are we moving?
    private MovementDir movementDirV; // Vertical Movement
    private LastWall lastWall; // The last wall we ran into

    private boolean hasIdle; // Do we have an idle image?

    public PCharacter()
    {
        // Call the main constructor to create the Character without an image
        this(null, 1, 1.0f, 100, false);
    }
    public PCharacter(String fileName, int numImage, float startSpeed, int startHealth, boolean idleImage)
    {
        // Call the constructor of the parent to set the characters image
        super(fileName, numImage, false);

        // Record time since we started
        animTime = new Timer();

        speed = startSpeed;
        health = startHealth;
        points = 0;
        loadHighScore(true);

        // Set the Characters default position to the center screen
        position = Engine.getCenterScreen();

        baseSpeed = speed;
        baseHealth = health;
        basePoints = points;
        timeBetweenAnim = 500; // default 500ms between animations
        lastFrameCount = 0;
        lastRight = true;
        noMove = new Vector2(0.0f, 0.0f);
        hasIdle = idleImage;

        if (numImage > 1)
            loadSprites(idleImage);
    }

    public void loadHighScore(boolean initial)
    {
        // Load the last high score here
        if (initial)
            lastHighScore = Engine.loadHighScore();

        if (highScore > lastHighScore)
            lastHighScore = highScore;
        else
            highScore = lastHighScore;
    }


    /**
     * Resets the character to a default state
     */
    public void resetCharacter()
    {
        speed = baseSpeed;
        health = baseHealth;
        points = basePoints;
        lastWall = LastWall.NONE;
        loadHighScore(false);
    }

    /**
     * Loads each Sprite into the Sprite array
     */
    public void loadSprites(boolean idleImage)
    {
        String fileName = currentFile;
        anims = new Sprite[totalSprites + ((idleImage)?(1):(0))];

        // Remove the animation count and .png from the fileName
        fileName = fileName.substring(0, fileName.indexOf(Engine.toString(currentSprite) + ".png"));

        if (idleImage)
            currentSprite = 0;

        for (int i = 0; i < totalSprites; ++i)
        {
            anims[i] = new Sprite(Engine.getEngine());
            anims[i].setTexture(new Texture(Engine.getEngine()));

            // Set the next image in the animation and loads it to the characters sprite
            currentFile = fileName + Engine.toString(currentSprite) + ".png";

            // Increment the animation image
            currentSprite++;

            anims[i].setFileName(currentFile);
            anims[i].loadTexture(currentFile);
        }

        Log.d("Character", "Loaded Sprites");
        currentFile = anims[0].getFileName();
        currentSprite = 1;
    }


    /**
     * Checks if we can move in the specified direction
     * @param move
     */
    public Vector2 checkMove(Vector2 move)
    {
        // If we are attempting to move right or left and we cannot move right or left
        if ((move.getX() > 0 && noMove.getX() > 0) || (move.getX() < 0 && noMove.getX() < 0))
        {
            // reset the movement speed and direction
            move.setX(0);
            movementDir = MovementDir.NONE;
        }
        if ((move.getY() > 0 && noMove.getY() > 0) || (move.getY() < 0 && noMove.getY() < 0))
        {
            // Reset the movement speed and direction
            move.setY(0);
            movementDirV = MovementDir.NONE;
        }

        return move;
    }

    /**
     * Calculates the X and Y Movement of the character and animates the character for the next frame if necessary
     * @param move
     */
    public Vector2 calcMovement(Vector2 move, boolean stopGravity)
    {
        Sprite image = getSprite();
        float gravity;
        float xMove;
        float yMove;

        move = checkMove(move);

        // Calculate the X and Y Movement speed of the player based on the directions we can move
        xMove = ((movementDir != MovementDir.NONE)?(move.getX() * speed):(0.0f));
        yMove = ((movementDirV != MovementDir.NONE)?(move.getY() * speed):(0.0f));

        if (noMove.getX() > 0 && xMove > 0 || noMove.getX() < 0 && xMove < 0)
            xMove = 0;

        if (noMove.getY() > 0 && yMove > 0 || noMove.getY() < 0 && yMove < 0)
            yMove = 0;

        // Do we want to apply gravity to the character?
        if (stopGravity || noMove.getY() != 0)
            gravity = 0.0f;
        else
            gravity = Engine.getGravity();

        // Set the new character position based on the movement and speed of character and the gravity of the world
        setPosition(image.getPosition().getX() + xMove, (image.getPosition().getY() + yMove + gravity));

        // If we are moving the character
        if (move.getX() != 0 || move.getY() != 0)
        {
            // If we are moving towards the right edge of the screen, flip right
            if (move.getX() > 0 || (move.getX() == 0 && lastRight))
            {
                image.getTexture().faceRight(true);
                lastRight = true;
            }
            // If we are moving towards the left edge of the screen, flip left
            else if (move.getX() < 0 || (move.getX() == 0 && !lastRight))
            {
                image.getTexture().faceRight(false);
                lastRight = false;
            }

            // If the time elapsed is at least 'timeBetweenAnim'
//            if (animTime.stopwatch(timeBetweenAnim))
            if (Engine.getFrameRate() >= lastFrameCount + 10)
            {
                // Move the players animation to the next image
                setNextImage();
            }
        }
        // If we are not moving the character
        else if (hasIdle && move.getX() == 0 && move.getY() == 0)
        {
            // Use the characters idle image if we have one
            setIdleImage();
        }

        return move;
    }

    public void setNextImage()
    {
        Vector2 pos = anims[currentSprite-1].getPosition();

        currentSprite++;
        if (currentSprite > totalSprites)
        {
            // If we have an idle image, start from the second sprite
            if (hasIdle)
                currentSprite = 2;
            else
                currentSprite = 1;
        }

        if (hasIdle && currentSprite != 1)
            anims[0].getTexture().faceRight(lastRight);
        loadImage(anims[currentSprite-1]);
    }

    // Moves the character to the idle image
    public void setIdleImage()
    {
        Vector2 pos = anims[0].getPosition();

        currentSprite = 1;
        loadImage(anims[0]);
    }


    /**
     * Sets the number of miliseconds between each animation change
     * @param ms
     */
    public void setTimeBetweenAnimation(int ms)
    {
        timeBetweenAnim = ms;
    }
    public int getTimeBetweenAnimation()
    {
        return timeBetweenAnim;
    }

    public void setSpeed(float charSpeed)
    {
        speed = charSpeed;
    }
    public float getSpeed()
    {
        return speed;
    }

    /**
     * Damages the character by 'val' points. If health drops below zero, calls the death routine
     * @param val
     */
    public void damageCharacter(int val)
    {
        health -= val;

        if (health <= 0)
        {
            health = 0;

            // Call the death routine
        }
    }
    public void kill()
    {
        health = 0;
    }
    public int getHealth()
    {
        return health;
    }

    public LastWall getLastWall()
    {
        return lastWall;
    }
    public void setLastWall(LastWall val)
    {
        lastWall = val;
    }

    /**
     * Adds 'val' points to the players total points
     * @param val
     */
    public void addPoints(float val)
    {
        points += val;
    }
    public float getPoints()
    {
        return points;
    }

    public float getHighScore() { return highScore; }
    public void setHighScore()
    {
        if (points > highScore)
            highScore = points;
    }

    public static float getLastHighScore()
    {
        return lastHighScore;
    }


    /**
     * Prevent the player from moving in these directions
     * @param dir
     */
    public void boundDirection(Vector2 dir)
    {
        noMove = dir;
    }

    public void setMoving(MovementDir dir)
    {
        movementDir = dir;
    }
    public MovementDir getMovementDir()
    {
        return movementDir;
    }
    public void setMovingVertical(MovementDir dir)
    {
        movementDirV = dir;
    }
    public MovementDir getMovementDirVertical()
    {
        return movementDirV;
    }
    public boolean getMoving()
    {
        // If we are moving horizontally or vertically
        if (movementDir != MovementDir.NONE || movementDirV != MovementDir.NONE)
            isMoving = true;
        else
            isMoving = false;

        return isMoving;
    }

}
