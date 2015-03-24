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

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.winsonchiu.reader.data.AnimationUtils;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class AdapterLinkList extends AdapterLink {

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
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.row_link, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        final ViewHolder viewHolder = (ViewHolder) holder;
        viewHolder.imagePreview.setImageBitmap(null);

        if (!controllerLinks.isLoading() && position > controllerLinks.size() - 10) {
            controllerLinks.loadMoreLinks();
        }

        Link link = controllerLinks.getLink(position);

        Drawable drawable = controllerLinks.getDrawableForLink(link);
        if (drawable == null) {
            viewHolder.imagePreview.setTag(
                    controllerLinks.loadImage(link.getThumbnail(), new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer response,
                                               boolean isImmediate) {
                            if (response.getBitmap() != null) {
                                viewHolder.imagePreview.setAlpha(0.0f);
                                viewHolder.imagePreview.setImageBitmap(response.getBitmap());
                                AnimationUtils.animateAlpha(viewHolder.imagePreview, 0.0f, 1.0f);
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError error) {

                        }
                    }));
        }
        else {
            viewHolder.imagePreview.setImageDrawable(drawable);
        }

        viewHolder.textThreadTitle.setText(link.getTitle());
        viewHolder.setTextInfo();
        viewHolder.layoutContainerActions.setVisibility(View.GONE);
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {

        final ViewHolder viewHolder = (ViewHolder) holder;

        Object tag = viewHolder.imagePreview.getTag();

        if (tag != null) {
            if (tag instanceof ImageLoader.ImageContainer) {
                ((ImageLoader.ImageContainer) tag).cancelRequest();
            }
            else if (tag instanceof Request) {
                ((Request) tag).cancel();
            }
        }

        viewHolder.webFull.onPause();
        viewHolder.webFull.resetMaxHeight();
        viewHolder.webFull.loadUrl("about:blank");
        viewHolder.webFull.setVisibility(View.GONE);
        viewHolder.videoFull.stopPlayback();
        viewHolder.videoFull.setVisibility(View.GONE);
        viewHolder.viewPagerFull.setVisibility(View.GONE);
        viewHolder.imagePreview.setImageBitmap(null);
        viewHolder.imagePreview.setVisibility(View.VISIBLE);
        viewHolder.progressImage.setVisibility(View.GONE);

        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return controllerLinks.size();
    }

    protected class ViewHolder extends AdapterLink.ViewHolderBase {

        public ViewHolder(View itemView) {
            super(itemView);

            View.OnClickListener clickListenerLink = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setVoteColors();
                    AnimationUtils.animateExpandActions(layoutContainerActions, false);
                }
            };
            textThreadTitle.setOnClickListener(clickListenerLink);
            textThreadInfo.setOnClickListener(clickListenerLink);
            this.itemView.setOnClickListener(clickListenerLink);


            this.imagePreview.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Link link = controllerLinks.getLink(getPosition());
                    imagePreview.setVisibility(View.VISIBLE);

                    if (link.isSelf()) {
                        if (TextUtils.isEmpty(link.getSelfText())) {
                            listener.onClickComments(link);
                        }
                        else {
                            String html = link.getSelfTextHtml();
                            html = Html.fromHtml(html.trim())
                                    .toString();
                            textThreadTitle.setText(Reddit.formatHtml(html,
                                    new Reddit.UrlClickListener() {
                                        @Override
                                        public void onUrlClick(String url) {
                                            listener.loadUrl(url);
                                        }
                                    }));
                        }
                    }
                    else {
                        loadFull(link);
                    }
                }
            });
        }
    }

}