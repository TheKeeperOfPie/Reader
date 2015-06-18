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
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.picasso.Picasso;
import com.winsonchiu.reader.data.Link;

import java.util.Date;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class AdapterLinkList extends AdapterLink {

    private static final String TAG = AdapterLinkList.class.getCanonicalName();

    private DividerItemDecoration itemDecoration;

    public AdapterLinkList(Activity activity,
            ControllerLinksBase controllerLinks,
            ControllerLinks.LinkClickListener listener) {
        super();
        setControllerLinks(controllerLinks, listener);
        setActivity(activity);
    }

    @Override
    public void setActivity(Activity activity) {
        super.setActivity(activity);
        this.layoutManager = new LinearLayoutManager(activity);
        this.itemDecoration = new DividerItemDecoration(activity,
                DividerItemDecoration.VERTICAL_LIST);
    }

    @Override
    public RecyclerView.ItemDecoration getItemDecoration() {
        return itemDecoration;
    }

    @Override
    public RecyclerView.ViewHolder  onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        if (viewType == VIEW_LINK_HEADER) {
            return new ViewHolderHeader(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.header_link, viewGroup, false), this);
        }

        ViewHolder viewHolder = new ViewHolder(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.row_link, viewGroup, false), this);
        viewHolders.add(viewHolder);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        super.onBindViewHolder(holder, position);

        switch (getItemViewType(position)) {
            case VIEW_LINK_HEADER:
                ViewHolderHeader viewHolderHeader = (ViewHolderHeader) holder;
                viewHolderHeader.onBind(controllerLinks.getSubreddit());
                break;
            case VIEW_LINK:
                ViewHolder viewHolder = (ViewHolder) holder;
                viewHolder.onBind(position);
                break;
        }
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {

        if (holder instanceof ViewHolder) {
            final ViewHolder viewHolder = (ViewHolder) holder;

            viewHolder.onRecycle();
        }

        super.onViewRecycled(holder);
    }

    @Override
    public ControllerLinks.LinkClickListener getListener() {
        return listener;
    }

    @Override
    public ControllerCommentsBase getControllerComments() {
        return listener.getControllerComments();
    }

    @Override
    public void pauseViewHolders() {
        for (ViewHolderBase viewHolder : viewHolders) {
            viewHolder.videoFull.stopPlayback();
        }
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
                    callback.pauseViewHolders();
                    callback.getListener()
                            .onClickComments(
                                    callback.getController()
                                            .getLink(getAdapterPosition()), ViewHolder.this);
                }
            });
        }

        @Override
        public float getRatio(int adapterPosition) {
            return 1f;
        }

        @Override
        public void onBind(int position) {
            super.onBind(position);

            imageThumbnail.setVisibility(View.VISIBLE);

            final Link link = callback.getController()
                    .getLink(position);

            Drawable drawable = callback.getController()
                    .getDrawableForLink(link);
            if (drawable == null) {
                Picasso.with(callback.getController()
                        .getActivity())
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
                    link.isOver18() ? callback.getController()
                            .getActivity()
                            .getResources()
                            .getColor(R.color.darkThemeTextColorAlert) : callback.getController()
                            .getActivity()
                            .getResources()
                            .getColor(
                                    R.color.darkThemeTextColor));

            int scoreLength = String.valueOf(link.getScore())
                    .length();

            String subreddit;
            Spannable spannableInfo;

            if (callback.getController().showSubreddit()) {
                subreddit = "/r/" + link.getSubreddit();
                spannableInfo = new SpannableString(
                        subreddit + "\n" + link.getScore() + " by " + link.getAuthor());
            }
            else {
                subreddit = "";
                spannableInfo = new SpannableString(" " + link.getScore() + " by " + link.getAuthor());
            }

            spannableInfo.setSpan(new ForegroundColorSpan(callback.getController()
                    .getActivity()
                    .getResources()
                    .getColor(
                            R.color.darkThemeTextColorMuted)), 0, subreddit.length(),
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            spannableInfo.setSpan(
                    new ForegroundColorSpan(link.getScore() > 0 ? callback.getController()
                            .getActivity()
                            .getResources()
                            .getColor(
                                    R.color.positiveScore) : callback.getController()
                            .getActivity()
                            .getResources()
                            .getColor(
                                    R.color.negativeScore)),
                    subreddit.length() + 1,
                    subreddit.length() + 1 + scoreLength, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            spannableInfo.setSpan(new ForegroundColorSpan(callback.getController()
                    .getActivity()
                    .getResources()
                    .getColor(
                            R.color.darkThemeTextColorMuted)), subreddit.length() + 1 + scoreLength,
                    spannableInfo.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            textThreadInfo.setText(spannableInfo);

            textHidden.setText(DateUtils.getRelativeTimeSpanString(link.getCreatedUtc()) + " at " + getFormatttedDate(link.getCreatedUtc()) + ", " + link.getNumComments() + " comments");
        }
    }

}