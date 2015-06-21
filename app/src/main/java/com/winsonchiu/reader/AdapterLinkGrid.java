package com.winsonchiu.reader;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class AdapterLinkGrid extends AdapterLink {

    private static final String TAG = AdapterLinkGrid.class.getCanonicalName();

    private DividerItemDecoration itemDecoration;
    private int defaultColor;
    private int thumbnailSize;
    private SharedPreferences preferences;

    public AdapterLinkGrid(Activity activity,
            ControllerLinksBase controllerLinks,
            ControllerLinks.LinkClickListener listener) {
        super();
        setControllerLinks(controllerLinks, listener);
        setActivity(activity);
    }

    @Override
    public void setActivity(Activity activity) {
        super.setActivity(activity);
        preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        Resources resources = activity.getResources();
        this.thumbnailSize = (int) (resources.getDisplayMetrics().widthPixels / 2f * 0.75f);
        boolean isLandscape = resources.getDisplayMetrics().widthPixels > resources.getDisplayMetrics().heightPixels;

        this.layoutManager = new StaggeredGridLayoutManager(isLandscape ? 3 : 2,
                StaggeredGridLayoutManager.VERTICAL);
        ((StaggeredGridLayoutManager) this.layoutManager).setGapStrategy(
                StaggeredGridLayoutManager.GAP_HANDLING_NONE);
        this.defaultColor = resources.getColor(R.color.darkThemeDialog);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        if (viewType == VIEW_LINK_HEADER) {
            return new ViewHolderHeader(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.header_link, viewGroup, false), this);
        }

        ViewHolder viewHolder = new ViewHolder(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.cell_link, viewGroup, false), this, defaultColor, thumbnailSize);
        viewHolders.add(viewHolder);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {

        super.onBindViewHolder(holder, position);

        switch (getItemViewType(position)) {
            case VIEW_LINK_HEADER:
                ViewHolderHeader viewHolderHeader = (ViewHolderHeader) holder;
                viewHolderHeader.onBind(controllerLinks.getSubreddit());
                break;
            case VIEW_LINK:
                ViewHolder viewHolder = (ViewHolder) holder;
                viewHolder.onBind(controllerLinks.getLink(position));
                break;
        }
    }

    public static boolean showThumbnail(Link link) {
        if (link.getThumbnail()
                .equals("nsfw")) {
            return false;
        }
        String domain = link.getDomain();
        return domain.contains("gfycat") || domain.contains("imgur") || Reddit.placeImageUrl(link);
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {

        if (holder instanceof ViewHolder) {

            final ViewHolder viewHolder = (ViewHolder) holder;

            viewHolder.itemView.setBackgroundColor(defaultColor);
            viewHolder.imagePlay.setVisibility(View.GONE);
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

    public static class ViewHolder extends AdapterLink.ViewHolderBase {

        private final int defaultColor;
        private final int thumbnailSize;
        protected ImageView imageFull;

        public ViewHolder(View itemView,
                ControllerLinks.ListenerCallback listenerCallback,
                int defaultColor,
                int thumbnailSize) {
            super(itemView, listenerCallback);
            this.defaultColor = defaultColor;
            this.thumbnailSize = thumbnailSize;

            this.imageFull = (ImageView) itemView.findViewById(R.id.image_full);
            this.imageFull.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    ViewHolder viewHolder = ViewHolder.this;

                    if (callback.getLayoutManager() instanceof StaggeredGridLayoutManager) {
                        ((StaggeredGridLayoutManager.LayoutParams) viewHolder.itemView.getLayoutParams()).setFullSpan(
                                true);
                        ((StaggeredGridLayoutManager) callback.getLayoutManager()).invalidateSpanAssignments();
                    }

                    imageFull.setVisibility(View.GONE);
                    progressImage.setVisibility(View.GONE);
                    imagePlay.setVisibility(View.GONE);

                    loadFull(callback.getController()
                            .getLink(getAdapterPosition()));
                }
            });

            this.videoFull.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
            buttonComments.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (videoFull.isPlaying()) {
                        videoFull.stopPlayback();
                        videoFull.setVisibility(View.GONE);
                        imageFull.setVisibility(View.VISIBLE);
                        imagePlay.setVisibility(View.VISIBLE);
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
        public void onBind(Link link) {

            super.onBind(link);

            if (itemView.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams) {
                ((StaggeredGridLayoutManager.LayoutParams) itemView.getLayoutParams()).setFullSpan(
                        false);
            }

            int position = getAdapterPosition();

            itemView.setBackgroundColor(defaultColor);
            imagePlay.setVisibility(View.GONE);
            Drawable drawable = callback.getController()
                    .getDrawableForLink(link);
            if (drawable != null) {
                imageFull.setVisibility(View.GONE);
                imageThumbnail.setVisibility(View.VISIBLE);
                imageThumbnail.setImageDrawable(drawable);
                ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).removeRule(
                        RelativeLayout.START_OF);
                ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).setMarginEnd(callback.getTitleMargin());
            }
            else if (showThumbnail(link)) {
                loadThumbnail(link, position);
            }
            else {
                imageFull.setVisibility(View.GONE);
                imageThumbnail.setVisibility(View.VISIBLE);
                ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).removeRule(
                        RelativeLayout.START_OF);
                ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).setMarginEnd(callback.getTitleMargin());
                Picasso.with(callback.getController()
                        .getActivity())
                        .load(link.getThumbnail())
                        .into(imageThumbnail);
            }

            setTextInfo(link);
        }

        private void loadThumbnail(final Link link, final int position) {

            imageFull.setVisibility(View.VISIBLE);
            imageThumbnail.setVisibility(View.GONE);
            progressImage.setVisibility(View.VISIBLE);
            ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).addRule(
                    RelativeLayout.START_OF, buttonComments.getId());
            ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).setMarginEnd(0);

            Picasso.with(callback.getController()
                    .getActivity())
                    .load(link.getThumbnail())
                    .into(imageFull,
                            new Callback() {
                                @Override
                                public void onSuccess() {
                                    Drawable drawable = imageFull.getDrawable();
                                    if (drawable instanceof BitmapDrawable) {
                                        Palette.from(((BitmapDrawable) drawable).getBitmap())
                                                .generate(
                                                        new Palette.PaletteAsyncListener() {
                                                            @Override
                                                            public void onGenerated(Palette palette) {
                                                                if (position == getAdapterPosition()) {
                                                                    AnimationUtils.animateBackgroundColor(
                                                                            itemView,
                                                                            ((ColorDrawable) itemView.getBackground()).getColor(),
                                                                            palette.getDarkVibrantColor(
                                                                                    palette.getMutedColor(
                                                                                            defaultColor)));
                                                                }
                                                            }
                                                        });
                                    }

                                    imageUrl = link.getThumbnail();
                                    if (Reddit.placeImageUrl(
                                            link) && position == getAdapterPosition()) {
                                        Picasso.with(callback.getController()
                                                .getActivity())
                                                .load(link.getUrl())
                                                .resize(thumbnailSize, thumbnailSize)
                                                .centerCrop()
                                                .into(imageFull, new Callback() {
                                                    @Override
                                                    public void onSuccess() {
                                                        progressImage.setVisibility(
                                                                View.GONE);
                                                    }

                                                    @Override
                                                    public void onError() {

                                                    }
                                                });

                                    }
                                    else {
                                        imagePlay.setVisibility(View.VISIBLE);
                                        progressImage.setVisibility(View.GONE);
                                    }
                                }

                                @Override
                                public void onError() {

                                }
                            });

        }

        // TODO: Fix expanding reply when cell is not full span
        @Override
        public void toggleReply() {
            if (itemView.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams) {
                ((StaggeredGridLayoutManager.LayoutParams) itemView.getLayoutParams()).setFullSpan(
                        true);
            }
            itemView.requestLayout();
            callback.getListener()
                    .onFullLoaded(getAdapterPosition());
            super.toggleReply();
        }

        @Override
        public float getRatio(int adapterPosition) {
            if (itemView.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams) {
                return ((StaggeredGridLayoutManager.LayoutParams) itemView.getLayoutParams()).isFullSpan() ?
                        1f :
                        1f / ((StaggeredGridLayoutManager) callback.getLayoutManager()).getSpanCount();
            }

            return 1f;
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
                                    R.color.darkThemeTextColorMuted)), 0,
                    subreddit.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            spannableInfo.setSpan(
                    new ForegroundColorSpan(link.getScore() > 0 ? callback.getController()
                            .getActivity()
                            .getResources()
                            .getColor(
                                    R.color.positiveScore) :
                            callback.getController()
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
                                    R.color.darkThemeTextColorMuted)),
                    subreddit.length() + 1 + scoreLength, spannableInfo.length(),
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            textThreadInfo.setText(spannableInfo);

            textHidden.setText(DateUtils.getRelativeTimeSpanString(link.getCreatedUtc()) + "\n" + link.getNumComments() + " comments");
        }
    }

}