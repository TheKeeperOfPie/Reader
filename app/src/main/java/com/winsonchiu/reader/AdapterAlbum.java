package com.winsonchiu.reader;

import android.app.Activity;
import android.support.v4.view.PagerAdapter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

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
    private DisallowListenerAlbum disallowListener;
    private List<View> recycledViews;
    private Album album;

    public AdapterAlbum(Activity activity, Album album, DisallowListenerAlbum listener) {
        this.activity = activity;
        this.album = album;
        this.disallowListener = listener;
        recycledViews = new ArrayList<>();
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {


        Image image = album.getImages().get(position);
        View view;
        final WebViewFixed webView;
        final ScrollView scrollText;

        if (!recycledViews.isEmpty()) {
            view = recycledViews.remove(0);

        }
        else {
            view = LayoutInflater.from(activity)
                    .inflate(R.layout.view_image, container, false);
            view.setTag(new ViewHolder(view));
        }

        ViewHolder viewHolder = (ViewHolder) view.getTag();

        webView = new WebViewFixed(activity.getApplicationContext());
        webView.setId(R.id.web);
        webView.getSettings()
                .setUseWideViewPort(true);
        webView.getSettings()
                .setLoadWithOverviewMode(true);
        webView.getSettings()
                .setBuiltInZoomControls(true);
        webView.getSettings()
                .setDisplayZoomControls(false);
        webView.setBackgroundColor(0x000000);
        webView.setWebChromeClient(null);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onScaleChanged(WebView view, float oldScale, float newScale) {
                webView.lockHeight();
                super.onScaleChanged(view, oldScale, newScale);
            }

            @Override
            public void onReceivedError(WebView view,
                    int errorCode,
                    String description,
                    String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                Toast.makeText(activity, "WebView error: " + description, Toast.LENGTH_SHORT).show();
            }
        });
        webView.setInitialScale(0);

        View.OnTouchListener onTouchListenerWebView = new View.OnTouchListener() {

            float startY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getPointerCount() > 1) {
                    disallowListener.requestDisallowInterceptTouchEventViewPager(true);
                    return false;
                }

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startY = event.getY();

                        if ((webView.canScrollVertically(1) && webView.canScrollVertically(
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
                        if (event.getY() - startY < 0 && webView.canScrollVertically(1)) {
                            disallowListener.requestDisallowInterceptTouchEvent(true);
                        }
                        else if (event.getY() - startY > 0 && webView.canScrollVertically(-1)) {
                            disallowListener.requestDisallowInterceptTouchEvent(true);
                        }
                        break;
                }
                return false;
            }
        };

        webView.setOnTouchListener(onTouchListenerWebView);
        webView.setScrollY(0);
        webView.loadData(Reddit.getImageHtml(image.getLink()), "text/html", "UTF-8");

        scrollText = viewHolder.scrollText;
        View.OnTouchListener onTouchListenerScrollText = new View.OnTouchListener() {

            float startY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getPointerCount() > 1) {
                    disallowListener.requestDisallowInterceptTouchEventViewPager(true);
                    return false;
                }

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startY = event.getY();

                        if ((scrollText.canScrollVertically(1) && scrollText.canScrollVertically(
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
                        if (event.getY() - startY < 0 && scrollText.canScrollVertically(1)) {
                            disallowListener.requestDisallowInterceptTouchEvent(true);
                        }
                        else if (event.getY() - startY > 0 && scrollText.canScrollVertically(-1)) {
                            disallowListener.requestDisallowInterceptTouchEvent(true);
                        }
                        break;
                }
                return false;
            }
        };
        scrollText.setOnTouchListener(onTouchListenerScrollText);
        scrollText.setVisibility(View.GONE);

        viewHolder.textAlbumIndicator.setText((position + 1) + " / " + album.getImages()
                .size());
        viewHolder.textAlbumIndicator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.reload();
            }
        });

        if (!TextUtils.isEmpty(image.getTitle()) && !"null".equals(image.getTitle())) {
            TextView textTitle = (TextView) view.findViewById(R.id.text_image_title);
            textTitle.setText(image.getTitle());
            textTitle.setVisibility(View.VISIBLE);
            scrollText.setVisibility(View.VISIBLE);
        }

        if (!TextUtils.isEmpty(image.getDescription()) && !"null".equals(image.getDescription())) {
            TextView textDescription = (TextView) view.findViewById(R.id.text_image_description);
            textDescription.setText(image.getDescription());
            textDescription.setVisibility(View.VISIBLE);
            scrollText.setVisibility(View.VISIBLE);
        }

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.BELOW, scrollText.getId());
        ((RelativeLayout) view).addView(webView, layoutParams);

        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        View view = (View) object;
        WebView webView = (WebView) view.findViewById(R.id.web);
        if (webView != null) {
            webView.onPause();
            webView.destroy();
            ((RelativeLayout) view).removeView(webView);
        }
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
            WebView webView = (WebView) view.findViewById(R.id.web);
            if (webView != null) {
                webView.onPause();
                webView.destroy();
                ((RelativeLayout) view).removeView(webView);
            }
        }
    }

    public static class ViewHolder {

        protected ScrollView scrollText;
        protected TextView textAlbumIndicator;

        public ViewHolder(View view) {
            scrollText = (ScrollView) view.findViewById(R.id.scroll_text);
            textAlbumIndicator = (TextView) view.findViewById(R.id.text_album_indicator);
        }
    }

    public interface DisallowListenerAlbum extends DisallowListener{
        void requestDisallowInterceptTouchEventViewPager(boolean disallow);
    }

}
