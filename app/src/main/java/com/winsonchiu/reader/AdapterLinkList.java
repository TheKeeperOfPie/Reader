package com.winsonchiu.reader;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class AdapterLinkList extends AdapterLink implements ControllerLinks.ListenerCallback {

    private static final String TAG = AdapterLinkList.class.getCanonicalName();

    private DividerItemDecoration itemDecoration;

    public AdapterLinkList(Activity activity, ControllerLinks controllerLinks, ControllerLinks.LinkClickListener listener) {
        setControllerLinks(controllerLinks, listener);
        setActivity(activity);
    }

    @Override
    public void setActivity(Activity activity) {
        super.setActivity(activity);
        this.layoutManager = new LinearLayoutManager(activity);
        this.itemDecoration = new DividerItemDecoration(activity, DividerItemDecoration.VERTICAL_LIST);
    }

    @Override
    public RecyclerView.ItemDecoration getItemDecoration() {
        return itemDecoration;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.row_link, viewGroup, false), this);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        if (controllerLinks.isLoading() && position > controllerLinks.size() - 10) {
            controllerLinks.loadMoreLinks();
        }

        final ViewHolder viewHolder = (ViewHolder) holder;
        viewHolder.onBind(position);
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {

        final ViewHolder viewHolder = (ViewHolder) holder;

        viewHolder.onRecycle();

        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return controllerLinks.size();
    }

    @Override
    public ControllerLinks.LinkClickListener getListener() {
        return listener;
    }

    @Override
    public ControllerLinks getController() {
        return controllerLinks;
    }

    @Override
    public int getColorPositive() {
        return colorPositive;
    }

    @Override
    public int getColorNegative() {
        return colorNegative;
    }

    @Override
    public Activity getActivity() {
        return activity;
    }

    @Override
    public float getItemWidth() {
        return itemWidth;
    }

    @Override
    public RecyclerView.LayoutManager getLayoutManager() {
        return layoutManager;
    }

    protected static class ViewHolder extends AdapterLink.ViewHolderBase {

        protected String currentThumbnailLink;

        public ViewHolder(View itemView, final ControllerLinks.ListenerCallback callback) {
            super(itemView, callback);

            this.imagePreview.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Link link = callback.getController().getLink(getAdapterPosition());

                    // TODO: Abstract to remove code duplication
                    if (link.isSelf()) {
                        if (textThreadSelf.isShown()) {
                            textThreadSelf.setVisibility(View.GONE);
                            return;
                        }
                        if (TextUtils.isEmpty(link.getSelfText())) {
                            callback.getListener().onClickComments(link, ViewHolder.this);
                        }
                        else {
                            if (textThreadSelf.isShown()) {
                                textThreadSelf.setVisibility(View.GONE);
                            }
                            else {
                                String html = link.getSelfTextHtml();
                                html = Html.fromHtml(html.trim())
                                        .toString();
                                textThreadSelf.setVisibility(View.VISIBLE);
                                textThreadSelf.setText(Reddit.formatHtml(html,
                                        new Reddit.UrlClickListener() {
                                            @Override
                                            public void onUrlClick(String url) {
                                                callback.getListener().loadUrl(url);
                                            }
                                        }));
                            }
                        }
                    }
                    else {
                        loadFull(link);
                    }
                }
            });
        }

        @Override
        public void onBind(int position) {
            super.onBind(position);

            final Link link = callback.getController()
                    .getLink(position);

            Drawable drawable = callback.getController().getDrawableForLink(link);
            if (drawable == null) {
                imageContainer = callback.getController()
                        .getReddit().getImageLoader().get(link.getThumbnail(),
                                new ImageLoader.ImageListener() {
                                    @Override
                                    public void onResponse(ImageLoader.ImageContainer response,
                                                           boolean isImmediate) {
                                        if (response.getBitmap() != null) {
                                            currentThumbnailLink = link.getThumbnail();
                                            if (isImmediate) {
                                                imagePreview.setImageBitmap(response.getBitmap());
                                            }
                                            else {
                                                imagePreview.setAlpha(0.0f);
                                                imagePreview.setImageBitmap(response.getBitmap());
                                                AnimationUtils.animateAlpha(imagePreview, 0.0f, 1.0f);
                                            }
                                        }
                                    }

                                    @Override
                                    public void onErrorResponse(VolleyError error) {

                                    }
                                });
            }
            else {
                imagePreview.setImageDrawable(drawable);
            }

            textThreadTitle.setText(link.getTitle());
            setTextInfo();
            toolbarActions.setVisibility(View.GONE);

        }
    }

}