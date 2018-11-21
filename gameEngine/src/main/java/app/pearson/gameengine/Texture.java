package app.pearson.gameengine;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;

public class Texture {

    private Context p_context;
    private Bitmap p_bitmap;
    private Bitmap p_flipped; // The flipped image
    private Bitmap p_normal; // The normal facing image
    private boolean p_dirRight; // Are we facing to the right of the screen?

    public Texture(Context context)
    {
        p_context = context;
        p_bitmap = null;
        p_flipped = null;
        p_normal = null;
        p_dirRight = false;
    }

    public Bitmap getBitmap()
    {
        return p_bitmap;
    }

    public boolean loadFromAsset(String filename)
    {
        InputStream istream = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        try
        {
            istream = p_context.getAssets().open(filename);
            p_bitmap = BitmapFactory.decodeStream(istream, null, options);
            p_normal = p_bitmap;
            p_flipped = flipImage(false);
            istream.close();
            Log.d("Loaded", "Loaded new Bitmap");
        }
        catch (IOException e)
        {
            return false;
        }
        return true;
    }

    /**
     * Set the size of the Texture
     * @param x - The width to resize the texture to
     * @param y - The height to resize the texture to
     * @return was the resize successful
     */
    public boolean setSize(int x, int y)
    {
        boolean good = false;

        try
        {
            if (p_bitmap == null)
                throw new Exception("Texture Bitmap not set!");

            // Set the width and height of the bitmap
            p_bitmap = getResizedBitmap(p_bitmap, x, y);
            p_normal = p_bitmap;
            p_flipped = flipImage(false);

            good = true;
        }
        catch (Exception ex)
        {
            good = false;
        }

        return good;
    }
    public boolean setSize(Vector2 size, boolean flip)
    {
        boolean good = false;

        try
        {
            if (p_bitmap == null)
                throw new Exception("Texture Bitmap not set!");

            // Set the width and height of the bitmap
            p_bitmap = getResizedBitmap(p_bitmap, (int) size.getX(), (int) size.getY());
            p_normal = p_bitmap;
            if (flip)
                p_flipped = flipImage(false);

            good = true;
        }
        catch (Exception ex)
        {
            good = false;
        }

        return good;
    }


    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight)
    {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    /**
     * Flips the image horizontally. When dirRight is true, the image faces the right of the screen
     * @param dirRight
     * @return
     */
    public Bitmap flipImage(boolean dirRight)
    {
        int faceDir = 0;
        Bitmap rBit;
        Matrix matrix = new Matrix();

        if (dirRight)
            faceDir = 1;
        else
            faceDir = -1;

        matrix.postScale(faceDir, 1, p_bitmap.getWidth() / 2.0f, p_bitmap.getHeight() / 2.0f);
        rBit = Bitmap.createBitmap(p_bitmap, 0, 0, p_bitmap.getWidth(), p_bitmap.getHeight(), matrix, true);

        return rBit;
    }

    public void faceRight(boolean dirRight)
    {
        if (dirRight)
            p_bitmap = p_normal;
        else
            p_bitmap = p_flipped;
    }

}

