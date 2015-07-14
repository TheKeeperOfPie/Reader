/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.links;

import android.graphics.Rect;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.winsonchiu.reader.utils.DisallowListener;
import com.winsonchiu.reader.utils.OnTouchListenerDisallow;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.views.WebViewFixed;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.imgur.Album;
import com.winsonchiu.reader.data.imgur.Image;

import java.util.Stack;

/**
 * Created by TheKeeperOfPie on 3/19/2015.
 */
public class AdapterAlbum extends PagerAdapter {

    private static final String TAG = AdapterAlbum.class.getCanonicalName();
    private final EventListener eventListener;
    private final ViewPager viewPager;
    private DisallowListener disallowListener;
    private Album album;
    private Stack<View> recycledViews;

    public AdapterAlbum(ViewPager viewPager, Album album, EventListener eventListener, DisallowListener disallowListener) {
        this.viewPager = viewPager;
        this.album = album;
        this.eventListener = eventListener;
        this.disallowListener = disallowListener;
        recycledViews = new Stack<>();
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
            view.setTag(new ViewHolder(view, eventListener, disallowListener));
        }
        else {
            view = recycledViews.pop();
        }

        final ViewHolder viewHolder = (ViewHolder) view.getTag();
        viewHolder.instantiate(image, position, album.getImagesCount());

        WebViewFixed webView = WebViewFixed.newInstance(
                container.getContext().getApplicationContext());
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
                    int errorCode,
                    String description,
                    String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                Log.e(TAG, "WebView error: " + description);
            }
        });

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.BELOW, R.id.text_image_title);

        ((RelativeLayout) view).addView(webView, 0, layoutParams);

        webView.loadData(Reddit.getImageHtml(image.getLink()), "text/html", "UTF-8");
        webView.setVisibility(View.VISIBLE);

        container.addView(view);

        container.requestLayout();
        viewHolder.textTitle.post(new Runnable() {
            @Override
            public void run() {
                if (!viewHolder.layoutInfo.isShown()) {
                    TextPaint textPaint = viewHolder.textTitle.getPaint();
                    Rect rect = new Rect();
                    textPaint.getTextBounds(viewHolder.textTitle.getText().toString(), 0,
                            viewHolder.textTitle.length(), rect);
                    if (rect.height() > viewHolder.textTitle.getHeight() || rect
                            .width() > viewHolder.textTitle.getWidth()) {
                        viewHolder.layoutInfo.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

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

    public void setAlbum(Album album) {
        this.album = album;
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
        void downloadImage(String fileName, String url);
    }

    public static class ViewHolder implements View.OnClickListener {

        private Image image;
        private EventListener eventListener;
        private DisallowListener disallowListener;

        protected RelativeLayout layoutRelative;
        protected TextView textTitle;
        protected ScrollView scrollDescription;
        protected TextView textTitleHidden;
        protected TextView textDescription;
        protected RelativeLayout layoutInfo;
        protected RelativeLayout layoutDownloadImage;
        protected ImageButton buttonInfo;
        protected ImageButton buttonDownload;
        protected TextView textAlbumIndicator;
        protected WebViewFixed webView;

        public ViewHolder(View view, EventListener listener, DisallowListener disallowListener) {
            this.eventListener = listener;
            this.disallowListener = disallowListener;
            layoutRelative = (RelativeLayout) view;
            textAlbumIndicator = (TextView) view.findViewById(R.id.text_album_indicator);
            textTitle = (TextView) view.findViewById(R.id.text_image_title);
            scrollDescription = (ScrollView) view.findViewById(R.id.scroll_description);
            textTitleHidden = (TextView) view.findViewById(R.id.text_title_hidden);
            textDescription = (TextView) view.findViewById(R.id.text_image_description);
            layoutInfo = (RelativeLayout) view.findViewById(R.id.layout_info);
            layoutDownloadImage = (RelativeLayout) view.findViewById(R.id.layout_download_image);
            buttonInfo = (ImageButton) view.findViewById(R.id.button_info);
            buttonDownload = (ImageButton) view.findViewById(R.id.button_download_image);

            view.setOnClickListener(this);
            buttonInfo.setOnClickListener(this);
            buttonDownload.setOnClickListener(this);
            textTitleHidden.setOnClickListener(this);
            textDescription.setOnClickListener(this);
            scrollDescription.setOnTouchListener(new OnTouchListenerDisallow(disallowListener));
        }

        public void instantiate(Image image, int position, int maxImages) {
            this.image = image;

            textAlbumIndicator.setText((position + 1) + " / " + maxImages);

            if (!TextUtils.isEmpty(image.getTitle()) && !"null".equals(image.getTitle())) {
                textTitle.setText(image.getTitle());
                textTitle.scrollTo(0, 0);
                textTitleHidden.setText(image.getTitle());
            }

            if (!TextUtils.isEmpty(image.getDescription()) && !"null".equals(image.getDescription())) {
                textDescription.setText(image.getDescription());
                layoutInfo.setVisibility(View.VISIBLE);
            }
            else {
                layoutInfo.setVisibility(View.GONE);
            }

            scrollDescription.scrollTo(0, 0);

            Linkify.addLinks(textDescription, Linkify.ALL);

        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_info:
                    scrollDescription.setVisibility(scrollDescription.isShown() ? View.GONE : View.VISIBLE);
                    textTitle.setVisibility(scrollDescription.isShown() ? View.INVISIBLE : View.VISIBLE);
                    break;
                case R.id.button_download_image:
                    eventListener.downloadImage("Imgur" + image.getId(), image.getLink());
                    break;
                default:
                    scrollDescription.setVisibility(View.GONE);
                    textTitle.setVisibility(View.VISIBLE);
                    break;
            }
        }
    }

}
