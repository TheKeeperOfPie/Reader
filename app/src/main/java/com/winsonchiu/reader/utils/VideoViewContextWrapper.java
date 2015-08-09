package com.winsonchiu.reader.utils;

import android.content.Context;
import android.content.ContextWrapper;

/**
 * Wrapper for Context to prevent AudioManager memory leak
 * when using VideoView
 *
 * Created by TheKeeperOfPie on 8/8/2015.
 */
public class VideoViewContextWrapper extends ContextWrapper {

    public VideoViewContextWrapper(Context base) {
        super(base);
    }

    @Override
    public Object getSystemService(String name) {
        if (Context.AUDIO_SERVICE.equals(name)) {
            return getApplicationContext().getSystemService(name);
        }
        return super.getSystemService(name);
    }
}
