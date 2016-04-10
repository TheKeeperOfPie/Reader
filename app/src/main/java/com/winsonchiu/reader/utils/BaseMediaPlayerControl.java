/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

import android.media.MediaPlayer;
import android.widget.MediaController;

/**
 * Created by TheKeeperOfPie on 4/9/2016.
 */
public abstract class BaseMediaPlayerControl implements MediaController.MediaPlayerControl {

    @Override
    public void start() {
        if (getMediaPlayer() != null) {
            getMediaPlayer().start();
        }
    }

    @Override
    public void pause() {
        if (getMediaPlayer() != null) {
            getMediaPlayer().pause();
        }
    }

    @Override
    public int getDuration() {
        return getMediaPlayer() != null ? getMediaPlayer().getDuration() : 0;
    }

    @Override
    public int getCurrentPosition() {
        return getMediaPlayer() != null ? getMediaPlayer().getCurrentPosition() : 0;
    }

    @Override
    public void seekTo(int pos) {
        if (getMediaPlayer() != null) {
            getMediaPlayer().seekTo(pos);
        }
    }

    @Override
    public boolean isPlaying() {
        return getMediaPlayer() != null && getMediaPlayer().isPlaying();
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return getMediaPlayer() != null ? getMediaPlayer().getAudioSessionId() : 0;
    }

    protected abstract MediaPlayer getMediaPlayer();
}
