/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.links;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.winsonchiu.reader.R;
import com.winsonchiu.reader.data.imgur.Album;
import com.winsonchiu.reader.data.imgur.Image;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.utils.CustomColorFilter;
import com.winsonchiu.reader.utils.DisallowListener;
import com.winsonchiu.reader.utils.OnTouchListenerDisallow;
import com.winsonchiu.reader.views.WebViewFixed;

import java.util.Stack;

/**
 * Created by TheKeeperOfPie on 3/19/2015.
 */
public class AdapterAlbum extends PagerAdapter {

    private static final String TAG = AdapterAlbum.class.getCanonicalName();
    private final EventListener eventListener;
    private final ViewPager viewPager;
    private int margin;
    private CustomColorFilter colorFilterIcon;
    private DisallowListener disallowListener;
    private Album album;
    private Stack<View> recycledViews;

    public AdapterAlbum(ViewPager viewPager,
            Album album,
            EventListener eventListener,
            DisallowListener disallowListener,
            CustomColorFilter colorFilterIcon) {
        this.viewPager = viewPager;
        this.album = album;
        this.eventListener = eventListener;
        this.disallowListener = disallowListener;
        this.colorFilterIcon = colorFilterIcon;
        recycledViews = new Stack<>();
        margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8,
                viewPager.getContext().getResources().getDisplayMetrics());

    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Override
    public Object instantiateItem(final ViewGroup container, int position) {

        final Image image = album.getImages().get(position);
        View view;

        if (recycledViews.isEmpty()) {
            view = LayoutInflater.from(container.getContext())
                    .inflate(R.layout.view_image, container, false);
            view.setTag(new ViewHolder(view, eventListener, disallowListener, colorFilterIcon));
        }
        else {
            view = recycledViews.pop();
        }

        final ViewHolder viewHolder = (ViewHolder) view.getTag();
        viewHolder.instantiate(image, position, album.getImagesCount());

        final WebViewFixed webView = WebViewFixed.newInstance(
                container.getContext().getApplicationContext(),
                false,
                new WebViewFixed.Listener() {
                    @Override
                    public void onFinished() {

                    }
                });
        webView.setVisibility(View.GONE);
        webView.setOnTouchListener(new OnTouchListenerDisallow(disallowListener));
        webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webView.setId(R.id.web);
        webView.setScrollbarFadingEnabled(false);
        webView.setScrollY(0);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onScaleChanged(WebView view, float oldScale, float newScale) {
                ((WebViewFixed) view).lockHeight();
                super.onScaleChanged(view, oldScale, newScale);
            }

            @Override
            public void onReceivedError(WebView view,
                    WebResourceRequest request,
                    WebResourceError error) {
                super.onReceivedError(view, request, error);
                Log.e(TAG, "WebView error: " + error);
            }

        });

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);

        ((RelativeLayout) view).addView(webView, 0, layoutParams);

        final CharSequence title =
                !TextUtils.isEmpty(image.getTitle()) && !"null".equals(image.getTitle()) ?
                        Html.fromHtml(image.getTitle()) : "";

        final CharSequence description = !TextUtils.isEmpty(image.getDescription()) && !"null"
                .equals(image.getDescription()) ? Html.fromHtml(image.getDescription()) : "";

        Log.d(TAG, "title: " + title);
        Log.d(TAG, "description: " + description);

        webView.setVisibility(View.VISIBLE);
        webView.post(new Runnable() {
            @Override
            public void run() {
                webView.loadData(
                        Reddit.getImageHtmlForAlbum(image.getLink(), title, description, 0xFFFFFFFF,
                                margin), "text/html; charset=UTF-8", "UTF-8");
            }
        });

        container.addView(view);
        container.requestLayout();

        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        View view = (View) object;
        WebView webView = (WebView) view.findViewById(R.id.web);
        if (webView != null) {
            webView.stopLoading();
            webView.onPause();
            webView.destroy();
            Reddit.incrementDestroy();
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

    public void setAlbum(Album album, CustomColorFilter colorFilterIcon) {
        this.album = album;
        this.colorFilterIcon = colorFilterIcon;
        destroyViews();
        notifyDataSetChanged();
    }

    public void destroyViews() {
//        for (View view : recycledViews) {
//            WebView webView = (WebView) view.findViewById(R.id.web);
//            if (webView != null) {
//                webView.onPause();
//                webView.destroy();
//                Reddit.incrementDestroy();
//                ((RelativeLayout) view).removeView(webView);
//            }
//        }
    }

    public interface EventListener {
        void downloadImage(String title, String fileName, String url);
    }

    public static class ViewHolder implements View.OnClickListener {

        private Image image;
        private EventListener eventListener;
        private DisallowListener disallowListener;

        protected RelativeLayout layoutRelative;
        protected RelativeLayout layoutDownloadImage;
        protected ImageButton buttonDownload;
        protected TextView textAlbumIndicator;
        protected WebViewFixed webView;

        public ViewHolder(View view,
                EventListener listener,
                DisallowListener disallowListener,
                CustomColorFilter colorFilterIcon) {
            this.eventListener = listener;
            this.disallowListener = disallowListener;
            layoutRelative = (RelativeLayout) view;
            textAlbumIndicator = (TextView) view.findViewById(R.id.text_album_indicator);
            layoutDownloadImage = (RelativeLayout) view.findViewById(R.id.layout_download_image);
            buttonDownload = (ImageButton) view.findViewById(R.id.button_download_image);

            buttonDownload.setColorFilter(colorFilterIcon);
            textAlbumIndicator.setTextColor(colorFilterIcon.getColor());

            buttonDownload.setOnClickListener(this);
        }

        public void instantiate(Image image, int position, int maxImages) {
            this.image = image;

            textAlbumIndicator.setText((position + 1) + " / " + maxImages);

        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_download_image:
                    eventListener.downloadImage(image.getTitle(), "Imgur" + image.getId(), image.getLink());
                    break;
            }
        }
    }

}
