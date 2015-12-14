/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.utils;

import com.google.android.youtube.player.YouTubePlayer;

/**
 * Created by TheKeeperOfPie on 7/7/2015.
 */
public class YouTubePlayerStateListener extends SimplePlayerStateChangeListener {

    private YouTubePlayer youTubePlayer;
    private boolean setFullscreen;
    private int seekToMillis;

    public YouTubePlayerStateListener(YouTubePlayer youTubePlayer, int seekToMillis, boolean setFullscreen) {
        super();
        this.youTubePlayer = youTubePlayer;
        this.seekToMillis = seekToMillis;
        this.setFullscreen = setFullscreen;
    }

    @Override
    public void onVideoStarted() {
        if (seekToMillis > 0) {
            youTubePlayer.seekToMillis(seekToMillis);
            seekToMillis = 0;
        }
        if (setFullscreen) {
            youTubePlayer.setFullscreen(true);
            setFullscreen = false;
        }
    }
}
