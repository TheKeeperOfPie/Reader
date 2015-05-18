package com.winsonchiu.reader;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.picasso.Picasso;
import com.winsonchiu.reader.data.Link;

import java.util.Date;

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

        if (!controllerLinks.isLoading() && position > controllerLinks.size() - 10) {
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
    public int getColorMuted() {
        return colorMuted;
    }

    @Override
    public int getColorText() {
        return colorText;
    }

    @Override
    public int getColorTextAlert() {
        return colorTextAlert;
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

        public ViewHolder(View itemView, final ControllerLinks.ListenerCallback callback) {
            super(itemView, callback);
            buttonComments.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (videoFull.isPlaying()) {
                        videoFull.stopPlayback();
                        videoFull.setVisibility(View.GONE);
                        imageThumbnail.setVisibility(View.VISIBLE);
                    }
                    callback.getListener()
                            .onClickComments(
                                    callback.getController()
                                            .getLink(getAdapterPosition()), ViewHolder.this);
                }
            });
        }

        @Override
        public void onBind(int position) {
            super.onBind(position);

            textHidden.setVisibility(View.GONE);
            toolbarActions.setVisibility(View.GONE);
            imageThumbnail.setVisibility(View.VISIBLE);

            final Link link = callback.getController()
                    .getLink(position);

            Drawable drawable = callback.getController()
                    .getDrawableForLink(link);
            if (drawable == null) {
                Picasso.with(callback.getActivity())
                        .load(link.getThumbnail())
                        .into(imageThumbnail);
            }
            else {
                imageThumbnail.setImageDrawable(drawable);
            }

            setTextInfo(link);

        }

        @Override
        public void setTextInfo(Link link) {
            super.setTextInfo(link);

            if (!TextUtils.isEmpty(link.getLinkFlairText())) {
                textThreadFlair.setVisibility(View.VISIBLE);
                textThreadFlair.setText(link.getLinkFlairText());
            }
            else {
                textThreadFlair.setVisibility(View.GONE);
            }

            textThreadTitle.setText(Html.fromHtml(link.getTitle())
                    .toString());
            textThreadTitle.setTextColor(
                    link.isOver18() ? callback.getColorTextAlert() : callback.getColorText());

            String subreddit = "/r/" + link.getSubreddit();
            int scoreLength = String.valueOf(link.getScore())
                    .length();

            Spannable spannableInfo = new SpannableString(subreddit + " " + link.getScore() + " by " + link.getAuthor());
            spannableInfo.setSpan(new ForegroundColorSpan(callback.getColorMuted()), 0, subreddit.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            spannableInfo.setSpan(
                    new ForegroundColorSpan(link.getScore() > 0 ? callback.getColorPositive() : callback.getColorNegative()),
                    subreddit.length() + 1,
                    subreddit.length() + 1 + scoreLength, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            spannableInfo.setSpan(new ForegroundColorSpan(callback.getColorMuted()), subreddit.length() + 1 + scoreLength, spannableInfo.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            textThreadInfo.setText(spannableInfo);

            textHidden.setText(new Date(
                    link.getCreatedUtc()).toString() + "\n" + link.getNumComments() + " comments");
        }
    }

}