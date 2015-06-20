package com.winsonchiu.reader;

import android.app.Activity;
import android.support.v4.view.PagerAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.imgur.Album;
import com.winsonchiu.reader.data.imgur.Image;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 3/19/2015.
 */
public class AdapterAlbum extends PagerAdapter {

    private static final String TAG = AdapterAlbum.class.getCanonicalName();
    private Activity activity;
    private DisallowListener disallowListener;
    private List<View> recycledViews;
    private Album album;

    public AdapterAlbum(Activity activity, Album album, DisallowListener listener) {
        this.activity = activity;
        this.album = album;
        this.disallowListener = listener;
        recycledViews = new ArrayList<>();
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {

        View view;
        final WebViewFixed webView;
        TextView textAlbumIndicator;
        final ScrollView scrollView;
        FrameLayout layoutFrame;

        if (!recycledViews.isEmpty()) {
            view = recycledViews.remove(0);
            webView = new WebViewFixed(activity.getApplicationContext());
            textAlbumIndicator = (TextView) view.findViewById(R.id.text_album_indicator);
            scrollView = (ScrollView) view.findViewById(R.id.scroll_image);
            layoutFrame = (FrameLayout) view.findViewById(R.id.layout_frame);
            layoutFrame.addView(webView);

        }
        else {
            view = LayoutInflater.from(activity)
                    .inflate(R.layout.view_image, container, false);
            textAlbumIndicator = (TextView) view.findViewById(R.id.text_album_indicator);

            webView = new WebViewFixed(activity.getApplicationContext());
            webView.getSettings()
                    .setUseWideViewPort(true);
            webView.getSettings()
                    .setLoadWithOverviewMode(true);
            webView.getSettings()
                    .setBuiltInZoomControls(true);
            webView.getSettings()
                    .setDisplayZoomControls(false);
            webView.getSettings()
                    .setDomStorageEnabled(true);
            webView.getSettings()
                    .setDatabaseEnabled(true);
            webView.getSettings()
                    .setAppCacheEnabled(true);
            webView.setBackgroundColor(0x000000);
            webView.setWebChromeClient(new WebChromeClient());
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onScaleChanged(WebView view, float oldScale, float newScale) {
                    webView.lockHeight();
                    super.onScaleChanged(view, oldScale, newScale);
                }
            });
            webView.setInitialScale(0);
            layoutFrame = (FrameLayout) view.findViewById(R.id.layout_frame);
            layoutFrame.addView(webView);

            scrollView = (ScrollView) view.findViewById(R.id.scroll_image);
            View.OnTouchListener onTouchListener = new View.OnTouchListener() {

                float startY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startY = event.getY();

                            if ((scrollView.canScrollVertically(1) && scrollView.canScrollVertically(
                                    -1))) {
                                disallowListener.requestDisallowInterceptTouchEvent(true);
                            }
                            else {
                                disallowListener.requestDisallowInterceptTouchEvent(false);
                            }
                            break;
                        case MotionEvent.ACTION_UP:
                            disallowListener.requestDisallowInterceptTouchEvent(false);
                            break;
                        case MotionEvent.ACTION_MOVE:
                            if (event.getY() - startY < 0 && scrollView.canScrollVertically(1)) {
                                disallowListener.requestDisallowInterceptTouchEvent(true);
                            }
                            else if (event.getY() - startY > 0 && scrollView.canScrollVertically(-1)) {
                                disallowListener.requestDisallowInterceptTouchEvent(true);
                            }
                            break;
                    }
                    return false;
                }
            };

            scrollView.setOnTouchListener(onTouchListener);
            webView.setOnTouchListener(onTouchListener);
        }

        Image image = album.getImages().get(position);

        scrollView.setScrollY(0);
        textAlbumIndicator.setText((position + 1) + " / " + album.getImages().size());

        webView.onResume();
        webView.resetMaxHeight();
        webView.loadData(Reddit.getImageHtml(image.getLink()), "text/html", "UTF-8");

        if (!TextUtils.isEmpty(image.getTitle()) && !"null".equals(image.getTitle())) {
            TextView textTitle = (TextView) view.findViewById(R.id.text_image_title);
            textTitle.setText(image.getTitle());
            textTitle.setVisibility(View.VISIBLE);
        }

        if (!TextUtils.isEmpty(image.getDescription()) && !"null".equals(image.getDescription())) {
            TextView textDescription = (TextView) view.findViewById(R.id.text_image_description);
            textDescription.setText(image.getDescription());
            textDescription.setVisibility(View.VISIBLE);
        }
        scrollView.measure(View.MeasureSpec.AT_MOST, View.MeasureSpec.EXACTLY);

        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        View view = (View) object;
        FrameLayout layoutFrame = (FrameLayout) view.findViewById(R.id.layout_frame);
        if (layoutFrame.getChildCount() > 0) {
            for (int index = 0; index < layoutFrame.getChildCount(); index++) {
                WebView webView = (WebView) layoutFrame.getChildAt(index);
                webView.onPause();
                webView.destroy();
            }
        }
        layoutFrame.removeAllViews();
        container.removeView(view);
        recycledViews.add(view);
    }

    @Override
    public int getCount() {
        return album == null ? 0 : album.getImagesCount();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    public void setAlbum(Album album) {
        this.album = album;
        destroyViews();
        notifyDataSetChanged();
    }

    public void destroyViews() {
        for (View view : recycledViews) {
            FrameLayout layoutFrame = (FrameLayout) view.findViewById(R.id.layout_frame);
            if (layoutFrame.getChildCount() > 0) {
                for (int index = 0; index < layoutFrame.getChildCount(); index++) {
                    WebView webView = (WebView) layoutFrame.getChildAt(index);
                    webView.onPause();
                    webView.destroy();
                }
            }
            layoutFrame.removeAllViews();
        }
    }

}
