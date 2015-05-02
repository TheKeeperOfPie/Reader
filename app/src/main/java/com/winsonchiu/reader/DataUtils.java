package com.winsonchiu.reader;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.widget.ImageView;

/**
 * Created by TheKeeperOfPie on 4/25/2015.
 */
public class DataUtils {

    private static final String TAG = DataUtils.class.getCanonicalName();

    public static void setNewImageBitmap(ImageView imageView, Bitmap newBitmap) {

        if (imageView.getDrawable() instanceof BitmapDrawable) {
            final Bitmap oldBitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            if (oldBitmap != null && oldBitmap != newBitmap) {
                imageView.setImageBitmap(null);
                imageView.post(new Runnable() {
                    @Override
                    public void run() {
                        oldBitmap.recycle();
                        Log.d(TAG, "oldBitmap recycled");
                    }
                });
            }
        }
        imageView.setImageBitmap(newBitmap);
    }

}
