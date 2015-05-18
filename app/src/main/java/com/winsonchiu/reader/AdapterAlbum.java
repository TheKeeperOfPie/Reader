package com.winsonchiu.reader;

import android.app.Activity;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.imgur.Album;

import java.util.ArrayList;

/**
 * Created by TheKeeperOfPie on 3/19/2015.
 */
public class AdapterAlbum extends PagerAdapter {

    private static final String TAG = AdapterAlbum.class.getCanonicalName();
    private Activity activity;
    private ArrayList<View> views;
    private DisallowListener disallowListener;
    private Album album;
    private int oldPosition = -1;

    public AdapterAlbum(Activity activity, Album album, DisallowListener listener) {
        this.activity = activity;
        this.album = album;
        this.disallowListener = listener;
        views = new ArrayList<>(album.getImagesCount());
        for (int index = 0; index < album.getImagesCount(); index++) {
            final WebViewFixed webView = (WebViewFixed) LayoutInflater.from(activity).inflate(R.layout.web_view_image, null, false);
            webView.getSettings().setUseWideViewPort(true);
            webView.getSettings().setLoadWithOverviewMode(true);
            webView.getSettings().setBuiltInZoomControls(true);
            webView.getSettings().setDisplayZoomControls(false);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setDomStorageEnabled(true);
            webView.setBackgroundColor(0xFF0000);
            webView.setInitialScale(0);
            webView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    if (event.getAction() == MotionEvent.ACTION_DOWN) {

                        if ((webView.canScrollVertically(1) && webView.canScrollVertically(-1))) {
                            disallowListener.requestDisallowInterceptTouchEvent(true);
                        }
                        else {
                            disallowListener.requestDisallowInterceptTouchEvent(false);
                            if (webView.getScrollY() == 0) {
                                webView.setScrollY(1);
                            }
                            else {
                                webView.setScrollY(webView.getScrollY() - 1);
                            }
                        }
                    }
                    else if (event.getAction() == MotionEvent.ACTION_UP) {
                        disallowListener.requestDisallowInterceptTouchEvent(false);
                    }

                    return false;
                }
            });
            views.add(webView);
        }
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {

        final WebViewFixed webView = (WebViewFixed) LayoutInflater.from(activity).inflate(R.layout.web_view_image, container, false);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.setBackgroundColor(0xFF0000);
        webView.setInitialScale(0);
        webView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_DOWN) {

                    if ((webView.canScrollVertically(1) && webView.canScrollVertically(-1))) {
                        disallowListener.requestDisallowInterceptTouchEvent(true);
                    }
                    else {
                        disallowListener.requestDisallowInterceptTouchEvent(false);
                        if (webView.getScrollY() == 0) {
                            webView.setScrollY(1);
                        }
                        else {
                            webView.setScrollY(webView.getScrollY() - 1);
                        }
                    }
                }
                else if (event.getAction() == MotionEvent.ACTION_UP) {
                    disallowListener.requestDisallowInterceptTouchEvent(false);
                }

                return false;
            }
        });
        webView.loadData(Reddit.getImageHtml(album.getImages()
                .get(position)
                .getLink()), "text/html", "UTF-8");
        container.addView(webView);
        return webView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public int getCount() {
        return album.getImagesCount();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        super.setPrimaryItem(container, position, object);
    }
}
