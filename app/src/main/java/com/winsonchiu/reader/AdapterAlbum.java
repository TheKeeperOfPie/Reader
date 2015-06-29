package com.winsonchiu.reader;

import android.support.v4.view.PagerAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
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
    private DisallowListener disallowListener;
//    private Stack<View> recycledViews;
    private Album album;

    public AdapterAlbum(Album album, DisallowListener disallowListener) {
        this.album = album;
        this.disallowListener = disallowListener;
//        recycledViews = new Stack<>();
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {


        Image image = album.getImages().get(position);
        View view;
        final WebView webView;
        final ScrollView scrollText;

        view = LayoutInflater.from(container.getContext())
                .inflate(R.layout.view_image, container, false);
        view.setTag(new ViewHolder(view));

        ViewHolder viewHolder = (ViewHolder) view.getTag();

        webView = WebViewFixed.newInstance(container.getContext().getApplicationContext());
        webView.setOnTouchListener(new OnTouchListenerDisallow(disallowListener));
        webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webView.setId(R.id.web);
        webView.setScrollbarFadingEnabled(false);
        webView.setScrollY(0);
        webView.loadData(Reddit.getImageHtml(image.getLink()), "text/html", "UTF-8");

        scrollText = viewHolder.scrollText;
        scrollText.setOnTouchListener(new OnTouchListenerDisallow(disallowListener));
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

        RelativeLayout relativeLayout = (RelativeLayout) view;
        relativeLayout.addView(webView, relativeLayout.getChildCount() - 2, layoutParams);

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

    public static class ViewHolder {

        protected ScrollView scrollText;
        protected TextView textAlbumIndicator;

        public ViewHolder(View view) {
            scrollText = (ScrollView) view.findViewById(R.id.scroll_text);
            textAlbumIndicator = (TextView) view.findViewById(R.id.text_album_indicator);
        }
    }

}
