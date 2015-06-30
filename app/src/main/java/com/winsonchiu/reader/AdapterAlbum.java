package com.winsonchiu.reader;

import android.support.v4.view.PagerAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.imgur.Album;
import com.winsonchiu.reader.data.imgur.Image;

import java.util.Stack;

/**
 * Created by TheKeeperOfPie on 3/19/2015.
 */
public class AdapterAlbum extends PagerAdapter {

    private static final String TAG = AdapterAlbum.class.getCanonicalName();
    private final EventListener eventListener;
    private DisallowListener disallowListener;
    private Album album;
    private Stack<View> recycledViews;

    public AdapterAlbum(Album album, EventListener eventListener, DisallowListener disallowListener) {
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
    public Object instantiateItem(ViewGroup container, int position) {

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

        ViewHolder viewHolder = (ViewHolder) view.getTag();
        viewHolder.instantiate(image, position, album.getImagesCount());

        WebViewFixed webView = WebViewFixed.newInstance(
                container.getContext().getApplicationContext());
        webView.setVisibility(View.GONE);
        webView.setOnTouchListener(new OnTouchListenerDisallow(disallowListener));
        webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webView.setId(R.id.web);
        webView.setScrollbarFadingEnabled(false);
        webView.setScrollY(0);

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.BELOW, R.id.scroll_title);

        ((RelativeLayout) view).addView(webView, 0, layoutParams);

        webView.loadData(Reddit.getImageHtml(image.getLink()), "text/html", "UTF-8");
        webView.setVisibility(View.VISIBLE);

        container.addView(view);

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

    public static class ViewHolder {

        private Image image;
        private EventListener eventListener;
        private DisallowListener disallowListener;

        protected RelativeLayout layoutRelative;
        protected TextView textTitle;
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
            textDescription = (TextView) view.findViewById(R.id.text_image_description);
            layoutInfo = (RelativeLayout) view.findViewById(R.id.layout_info);
            layoutDownloadImage = (RelativeLayout) view.findViewById(R.id.layout_download_image);
            buttonInfo = (ImageButton) view.findViewById(R.id.button_info);
            buttonDownload = (ImageButton) view.findViewById(R.id.button_download_image);

            buttonInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    textDescription.setVisibility(textDescription.isShown() ? View.GONE : View.VISIBLE);
                }
            });

            buttonInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    textDescription.setVisibility(textDescription.isShown() ? View.GONE : View.VISIBLE);
                }
            });

            buttonDownload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    eventListener.downloadImage("Imgur" + image.getId(), image.getLink());
                }
            });
        }

        public void instantiate(Image image, int position, int maxImages) {
            this.image = image;
            textDescription.setVisibility(View.GONE);

            textAlbumIndicator.setText((position + 1) + " / " + maxImages);

            if (!TextUtils.isEmpty(image.getTitle()) && !"null".equals(image.getTitle())) {
                textTitle.setText(image.getTitle());
            }

            if (!TextUtils.isEmpty(image.getDescription()) && !"null".equals(image.getDescription())) {
                textDescription.setText(image.getDescription());
                layoutInfo.setVisibility(View.VISIBLE);
            }

        }

    }

}
