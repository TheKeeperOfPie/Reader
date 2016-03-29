/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.links;

import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.PagerAdapter;
import android.text.Html;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;
import com.bumptech.glide.request.target.Target;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.data.imgur.Album;
import com.winsonchiu.reader.data.imgur.Image;
import com.winsonchiu.reader.glide.RequestListenerCompletion;
import com.winsonchiu.reader.utils.CustomColorFilter;
import com.winsonchiu.reader.utils.DisallowListener;
import com.winsonchiu.reader.utils.OnTouchListenerDisallow;
import com.winsonchiu.reader.views.CustomScrollView;
import com.winsonchiu.reader.views.ImageViewZoom;

import java.util.Stack;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by TheKeeperOfPie on 3/19/2015.
 */
public class AdapterAlbum extends PagerAdapter {

    public static final String TAG = AdapterAlbum.class.getCanonicalName();

    private Album album = new Album();
    private Stack<View> recycledViews = new Stack<>();

    private RequestManager requestManager;
    private DisallowListener disallowListener;
    private EventListener eventListener;
    private CustomColorFilter colorFilterIcon;

    public AdapterAlbum(RequestManager requestManager,
            DisallowListener disallowListener,
            EventListener eventListener,
            CustomColorFilter colorFilterIcon) {
        this.requestManager = requestManager;
        this.disallowListener = disallowListener;
        this.eventListener = eventListener;
        this.colorFilterIcon = colorFilterIcon;
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
            view.setTag(new ViewHolder(view, eventListener, requestManager, disallowListener, colorFilterIcon));
        }
        else {
            view = recycledViews.pop();
        }

        final ViewHolder viewHolder = (ViewHolder) view.getTag();
        viewHolder.bindData(image, position, album.getImagesCount());

        container.addView(view);
        container.requestLayout();

        return view;
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        super.setPrimaryItem(container, position, object);
        album.setPage(position);
        ((ViewHolder) ((View) object).getTag()).refreshImage();
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        View view = (View) object;
        ((ViewHolder) view.getTag()).recycle();
        container.removeView(view);
        recycledViews.add(view);
    }

    @Override
    public int getCount() {
        return album.getImagesCount();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    public void setAlbum(Album album, CustomColorFilter colorFilterIcon) {
        this.album = album;
        this.colorFilterIcon = colorFilterIcon;
        notifyDataSetChanged();
    }

    public interface EventListener {
        void downloadImage(String title, String fileName, String url);
    }

    public static class ViewHolder {

        private Image image;
        private EventListener eventListener;
        private RequestManager requestManager;

        private View view;
        @Bind(R.id.scroll_image) CustomScrollView scrollImage;
        @Bind(R.id.image_full) ImageViewZoom imageFull;
        @Bind(R.id.progress_image) ProgressBar progressImage;
        @Bind(R.id.text_error) TextView textError;
        @Bind(R.id.text_title) TextView textTitle;
        @Bind(R.id.text_description) TextView textDescription;
        @Bind(R.id.layout_download) ViewGroup layoutDownload;
        @Bind(R.id.button_download) ImageButton buttonDownload;
        @Bind(R.id.text_album_indicator) TextView textAlbumIndicator;

        public ViewHolder(View view,
                EventListener listener,
                RequestManager requestManager,
                DisallowListener disallowListener,
                CustomColorFilter colorFilterIcon) {
            this.view = view;
            this.eventListener = listener;
            this.requestManager = requestManager;
            ButterKnife.bind(this, view);

            textError.setTextColor(colorFilterIcon.getColor());
            textAlbumIndicator.setTextColor(colorFilterIcon.getColor());
            buttonDownload.setColorFilter(colorFilterIcon);

            imageFull.setListener(new ImageViewZoom.Listener() {
                @Override
                public void onTextureSizeExceeded() {
                    showError();
                }

                @Override
                public void onBeforeContentLoad(int width, int height) {

                }
            });
            imageFull.setOnTouchListener(new OnTouchListenerDisallow(disallowListener) {
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    if (event.getPointerCount() > 1) {
                        disallowListener.requestDisallowInterceptTouchEventHorizontal(true);
                        disallowListener.requestDisallowInterceptTouchEventVertical(true);
                        return false;
                    }

                    switch (MotionEventCompat.getActionMasked(event)) {
                        case MotionEvent.ACTION_DOWN:
                            startY = event.getY();

                            if ((view.canScrollVertically(1) && view.canScrollVertically(-1))) {
                                disallowListener.requestDisallowInterceptTouchEventVertical(true);
                            }
                            else {
                                disallowListener.requestDisallowInterceptTouchEventVertical(false);
                            }
                            disallowListener.requestDisallowInterceptTouchEventHorizontal(true);
                            break;
                        case MotionEvent.ACTION_CANCEL:
                            disallowListener.requestDisallowInterceptTouchEventHorizontal(false);
                            break;
                        case MotionEvent.ACTION_UP:
                            disallowListener.requestDisallowInterceptTouchEventVertical(false);
                            disallowListener.requestDisallowInterceptTouchEventHorizontal(false);
                            break;
                        case MotionEvent.ACTION_MOVE:
                            disallowListener.requestDisallowInterceptTouchEventHorizontal(true);
                            if (event.getY() - startY < 0) {
                                if (view.canScrollVertically(1)) {
                                    disallowListener.requestDisallowInterceptTouchEventVertical(true);
                                } else if (scrollImage.canScrollVertically(1)) {
                                    disallowListener.requestDisallowInterceptTouchEventVertical(true);
                                }
                            }
                            else if (event.getY() - startY > 0) {
                                if (view.canScrollVertically(-1)) {
                                    disallowListener.requestDisallowInterceptTouchEventVertical(true);
                                } else if (scrollImage.canScrollVertically(-1)) {
                                    disallowListener.requestDisallowInterceptTouchEventVertical(true);
                                }
                            }
                            break;
                    }
                    return false;
                }
            });

            scrollImage.setOnTouchListener(new OnTouchListenerDisallow(disallowListener));
        }

        private void showError() {
            textError.setText(view.getResources().getString(R.string.error_album_image, image.getLink()));
            textError.setVisibility(View.VISIBLE);

            Linkify.addLinks(textError, Linkify.WEB_URLS);
        }

        public void bindData(Image image, int position, int maxImages) {
            this.image = image;

            textAlbumIndicator.setText(textAlbumIndicator.getResources().getString(R.string.album_indicator, position + 1, maxImages));

            CharSequence title =
                    !TextUtils.isEmpty(image.getTitle()) && !"null".equals(image.getTitle())
                            ? Html.fromHtml(image.getTitle())
                            : null;

            CharSequence description =
                    !TextUtils.isEmpty(image.getDescription()) && !"null".equals(image.getDescription())
                            ? Html.fromHtml(image.getDescription())
                            : null;

            textTitle.setText(title);
            textTitle.setVisibility(TextUtils.isEmpty(title) ? View.GONE : View.VISIBLE);

            textDescription.setText(description);
            textDescription.setVisibility(TextUtils.isEmpty(description) ? View.GONE : View.VISIBLE);

            progressImage.setVisibility(View.VISIBLE);

            refreshImage();
        }

        @OnClick(R.id.button_download)
        public void downloadImage() {
            eventListener.downloadImage(image.getTitle(), "Imgur" + image.getId(), image.getLink());
        }

        public void recycle() {
            textError.setVisibility(View.GONE);
            imageFull.setImageDrawable(null);
            imageFull.getLayoutParams().height = 0;
        }

        public void refreshImage() {
            requestManager.load(image.getLink())
                    .listener(new RequestListenerCompletion<String, GlideDrawable>() {
                        @Override
                        public boolean onException(Exception e, String s, Target<GlideDrawable> target, boolean isFirstResource) {
                            showError();
                            return super.onException(e, s, target, isFirstResource);
                        }

                        @Override
                        protected void onCompleted() {
                            progressImage.setVisibility(View.GONE);
                        }
                    })
                    .into(new GlideDrawableImageViewTarget(imageFull));
        }
    }
}
