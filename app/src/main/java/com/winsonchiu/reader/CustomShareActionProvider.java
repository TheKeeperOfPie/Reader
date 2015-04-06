package com.winsonchiu.reader;

import android.content.Context;
import android.support.v7.widget.ShareActionProvider;
import android.view.View;

/**
 * Created by TheKeeperOfPie on 4/1/2015.
 */
public class CustomShareActionProvider extends ShareActionProvider {

    public CustomShareActionProvider(Context context) {
        super(context);
    }

    @Override
    public View onCreateActionView() {
        return null;
    }
}
