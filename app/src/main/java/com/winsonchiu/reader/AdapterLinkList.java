package com.winsonchiu.reader;

import android.app.Activity;
import android.content.res.Resources;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.picasso.Picasso;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class AdapterLinkList extends AdapterLink {

    private static final String TAG = AdapterLinkList.class.getCanonicalName();

    public AdapterLinkList(Activity activity,
            ControllerLinksBase controllerLinks,
            ControllerUser controllerUser,
            ViewHolderHeader.EventListener eventListenerHeader,
            ViewHolderBase.EventListener eventListenerBase,
            DisallowListener disallowListener,
            RecyclerCallback recyclerCallback) {
        super(eventListenerHeader, eventListenerBase, disallowListener, recyclerCallback);
        setControllers(controllerLinks, controllerUser);
        setActivity(activity);
    }

    @Override
    public void setActivity(Activity activity) {
        super.setActivity(activity);
        layoutManager = new LinearLayoutManager(activity);
    }

    @Override
    public RecyclerView.ViewHolder  onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        if (viewType == VIEW_LINK_HEADER) {
            return new ViewHolderHeader(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.header_link, viewGroup, false), eventListenerHeader);
        }

        return new ViewHolder(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.row_link, viewGroup, false), eventListenerBase, disallowListener,
                recyclerCallback);
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
                viewHolder.onBind(controllerLinks.getLink(position), controllerLinks.showSubreddit(), controllerUser.getUser().getName());
                break;
        }
    }

    public static class ViewHolder extends AdapterLink.ViewHolderBase {

        public ViewHolder(View itemView,
                EventListener eventListener,
                DisallowListener disallowListener,
                RecyclerCallback recyclerCallback) {
            super(itemView, eventListener, disallowListener, recyclerCallback);
        }

        @Override
        protected void initializeListeners() {
            super.initializeListeners();
            buttonComments.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (videoFull.isPlaying()) {
                        videoFull.stopPlayback();
                        videoFull.setVisibility(View.GONE);
                        imageThumbnail.setVisibility(View.VISIBLE);
                    }
                    loadComments();
                }
            });
        }

        @Override
        public float getRatio() {
            return 1f;
        }

        @Override
        public void onBind(Link link, boolean showSubreddit, String userName) {
            super.onBind(link, showSubreddit, userName);

            imageThumbnail.setVisibility(View.VISIBLE);

            Drawable drawable = Reddit.getDrawableForLink(itemView.getContext(), link);
            if (drawable == null) {
                if (TextUtils.isEmpty(link.getThumbnail()) || !preferences.getBoolean(AppSettings.PREF_SHOW_THUMBNAILS, true) || (link.isOver18() && !preferences.getBoolean(AppSettings.PREF_NSFW_THUMBNAILS, true))) {
                    imageThumbnail.setImageDrawable(drawableDefault);
                }
                else {
                    Picasso.with(itemView.getContext())
                            .load(link.getThumbnail())
                            .into(imageThumbnail);
                }
            }
            else {
                imageThumbnail.setImageDrawable(drawable);
            }

        }

        @Override
        public void setTextInfo(Link link) {
            super.setTextInfo(link);

            if (!TextUtils.isEmpty(link.getLinkFlairText())) {
                textThreadFlair.setVisibility(View.VISIBLE);
                textThreadFlair.setText(Reddit.getTrimmedHtml(link.getLinkFlairText()));
            }
            else {
                textThreadFlair.setVisibility(View.GONE);
            }

            textThreadTitle.setText(Html.fromHtml(link.getTitle())
                    .toString());
            textThreadTitle.setTextColor(
                    link.isOver18() ? itemView.getResources()
                            .getColor(R.color.darkThemeTextColorAlert) : itemView.getResources()
                            .getColor(
                                    R.color.darkThemeTextColor));

            Resources resources = itemView.getResources();

            int colorPositive = resources.getColor(R.color.positiveScore);
            int colorNegative = resources.getColor(R.color.negativeScore);

            String subreddit = showSubreddit ? "/r/" + link.getSubreddit() : "";

            Spannable spannableScore = new SpannableString(" " + link.getScore() + " ");
            spannableScore.setSpan(new ForegroundColorSpan(
                            link.getScore() > 0 ? colorPositive : colorNegative), 0,
                    spannableScore.length(),
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

            String flair = TextUtils.isEmpty(link.getAuthorFlairText()) || "null".equals(link.getAuthorFlairText()) ? "" : " (" + Html
                    .fromHtml(link.getAuthorFlairText()) + ") ";

            textThreadInfo.setText(TextUtils.concat(subreddit, spannableScore, "by " + link.getAuthor(), flair));

            // TODO: Add link edited indicator

            CharSequence timestamp = preferences.getBoolean(AppSettings.PREF_FULL_TIMESTAMPS, false) ? DateUtils.formatDateTime(itemView.getContext(), link.getCreatedUtc(), DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR) : DateUtils.getRelativeTimeSpanString(link.getCreatedUtc());

            textHidden.setText(timestamp + ", " + link.getNumComments() + " comments");
        }
    }

}