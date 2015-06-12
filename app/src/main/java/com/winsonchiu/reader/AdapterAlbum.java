package com.winsonchiu.reader;

import android.app.Activity;
import android.support.v4.view.PagerAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.imgur.Album;
import com.winsonchiu.reader.data.imgur.Image;

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
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {

        View view = LayoutInflater.from(activity).inflate(R.layout.view_image, container, false);

        final WebViewFixed webView = (WebViewFixed) view.findViewById(R.id.web_image);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.setBackgroundColor(0xFF0000);
        webView.setInitialScale(0);

        final ScrollView scrollView = (ScrollView) view.findViewById(R.id.scroll_image);
        View.OnTouchListener onTouchListener = new View.OnTouchListener() {

            float startY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startY = event.getY();

                        if ((scrollView.canScrollVertically(1) && scrollView.canScrollVertically(-1))) {
                            disallowListener.requestDisallowInterceptTouchEvent(true);
                            return true;
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

        Image image = album.getImages().get(position);

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

        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        View view = (View) object;
        WebView webView = (WebView) view.findViewById(R.id.web_image);
        webView.onPause();
        webView.destroy();
        container.removeView(view);
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
