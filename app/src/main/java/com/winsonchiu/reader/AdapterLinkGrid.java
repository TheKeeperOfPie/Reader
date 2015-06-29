package com.winsonchiu.reader;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
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
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.imgur.Album;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class AdapterLinkGrid extends AdapterLink {

    private static final String TAG = AdapterLinkGrid.class.getCanonicalName();

    private int thumbnailSize;

    public AdapterLinkGrid(Activity activity,
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

        Resources resources = activity.getResources();

        boolean isLandscape = resources.getDisplayMetrics().widthPixels > resources.getDisplayMetrics().heightPixels;
        int spanCount = isLandscape ? 3 : 2;
        layoutManager = new StaggeredGridLayoutManager(spanCount,
                StaggeredGridLayoutManager.VERTICAL);
        ((StaggeredGridLayoutManager) this.layoutManager).setGapStrategy(
                StaggeredGridLayoutManager.GAP_HANDLING_NONE);

        DisplayMetrics displayMetrics = resources.getDisplayMetrics();

        this.thumbnailSize = (int) (displayMetrics.widthPixels / 2 * (displayMetrics.widthPixels > displayMetrics.heightPixels ? 1f : 0.75f));
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        if (viewType == VIEW_LINK_HEADER) {
            return new ViewHolderHeader(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.header_link, viewGroup, false), eventListenerHeader);
        }

        return new ViewHolder(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.cell_link, viewGroup, false), eventListenerBase, disallowListener,
                recyclerCallback, thumbnailSize);
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

        private final int thumbnailSize;
        protected ImageView imageFull;

        public ViewHolder(View itemView,
                EventListener eventListener,
                DisallowListener disallowListener,
                RecyclerCallback recyclerCallback,
                int thumbnailSize) {
            super(itemView, eventListener, disallowListener, recyclerCallback);
            this.thumbnailSize = thumbnailSize;

        }

        @Override
        protected void initialize() {
            super.initialize();
            imageFull = (ImageView) itemView.findViewById(R.id.image_full);
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
                        imageFull.setVisibility(View.VISIBLE);
                        imagePlay.setVisibility(View.VISIBLE);
                    }
                    loadComments();
                }
            });
            this.imageFull.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    imageFull.setVisibility(View.GONE);
                    progressImage.setVisibility(View.GONE);
                    imagePlay.setVisibility(View.GONE);

                    loadFull();
                }
            });
        }

        @Override
        public void onBind(Link link, boolean showSubbreddit, String userName) {

            super.onBind(link, showSubbreddit, userName);

            int position = getAdapterPosition();

            setTextInfo(link);

            itemView.setBackgroundColor(itemView.getContext().getResources().getColor(R.color.darkThemeDialog));
            imagePlay.setVisibility(View.GONE);
            Drawable drawable = Reddit.getDrawableForLink(itemView.getContext(), link);
            if (drawable != null) {
                imageFull.setVisibility(View.GONE);
                imageThumbnail.setVisibility(View.VISIBLE);
                imageThumbnail.setImageDrawable(drawable);
            }
            else if (Reddit.showThumbnail(link)) {
                loadThumbnail(link, position);
                return;
            }
            else if (!TextUtils.isEmpty(link.getThumbnail())) {
                imageFull.setVisibility(View.GONE);
                imageThumbnail.setVisibility(View.VISIBLE);
                Picasso.with(itemView.getContext())
                        .load(link.getThumbnail())
                        .into(imageThumbnail);
            }
            else {
                imageFull.setVisibility(View.GONE);
                imageThumbnail.setVisibility(View.VISIBLE);
                imageThumbnail.setImageDrawable(drawableDefault);
            }

            ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).removeRule(
                    RelativeLayout.START_OF);
            ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).setMarginEnd(titleMargin);
        }

        @Override
        public void expandFull(boolean expand) {
            super.expandFull(expand);

            if (itemView.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams) {
                ((StaggeredGridLayoutManager.LayoutParams) itemView.getLayoutParams())
                        .setFullSpan(expand);
                if (expand) {
                    if (recyclerCallback.getLayoutManager() instanceof StaggeredGridLayoutManager) {
                        ((StaggeredGridLayoutManager) recyclerCallback.getLayoutManager())
                                .invalidateSpanAssignments();
                    }
                    recyclerCallback.scrollTo(getAdapterPosition());
                }
            }
        }

        private void loadThumbnail(final Link link, final int position) {

            // TODO: Improve thumbnail loading logic

            imageFull.setVisibility(View.VISIBLE);
            imageThumbnail.setVisibility(View.GONE);
            progressImage.setVisibility(View.VISIBLE);
            ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).addRule(
                    RelativeLayout.START_OF, buttonComments.getId());
            ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).setMarginEnd(0);

            Picasso.with(itemView.getContext()).load(android.R.color.transparent).into(imageFull);

            if (TextUtils.isEmpty(link.getThumbnail())) {
                if (Reddit.placeImageUrl(
                        link) && position == getAdapterPosition()) {
                    Picasso.with(itemView.getContext())
                            .load(link.getUrl())
                            .resize(thumbnailSize, thumbnailSize)
                            .centerCrop()
                            .into(imageFull, new Callback() {
                                @Override
                                public void onSuccess() {
                                    loadBackgroundColor();
                                    progressImage.setVisibility(
                                            View.GONE);
                                }

                                @Override
                                public void onError() {

                                }
                            });

                }
                else {
                    imageFull.setVisibility(View.GONE);
                    imageThumbnail.setVisibility(View.VISIBLE);
                    imageThumbnail.setImageDrawable(drawableDefault);
                    progressImage.setVisibility(View.GONE);

                    ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).removeRule(
                            RelativeLayout.START_OF);
                    ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).setMarginEnd(titleMargin);
                }
            }
            else {
                Picasso.with(itemView.getContext())
                        .load(link.getThumbnail())
                        .into(imageFull,
                                new Callback() {
                                    @Override
                                    public void onSuccess() {
                                        loadBackgroundColor();

                                        if (Reddit.placeImageUrl(
                                                link) && position == getAdapterPosition()) {
                                            Picasso.with(itemView.getContext())
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
                                        progressImage.setVisibility(View.GONE);
                                    }
                                });
            }

        }

        public void loadBackgroundColor() {
            final int position = getAdapterPosition();
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
                                                                    itemView.getContext().getResources().getColor(R.color.darkThemeDialog))));
                                        }
                                    }
                                });
            }
        }

        @Override
        public float getRatio() {
            if (itemView.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams) {
                int width = itemView.getContext().getResources().getDisplayMetrics().widthPixels;

                return ((StaggeredGridLayoutManager.LayoutParams) itemView.getLayoutParams()).isFullSpan() ?
                        1f :
                        width / itemView.getWidth();
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
                    link.isOver18() ?itemView.getContext().getResources()
                            .getColor(R.color.darkThemeTextColorAlert) : itemView.getContext()
                            .getResources()
                            .getColor(
                                    R.color.darkThemeTextColor));

            int scoreLength = String.valueOf(link.getScore())
                    .length();

            String subreddit;
            Spannable spannableInfo;

            if (showSubreddit) {
                subreddit = "/r/" + link.getSubreddit();
                spannableInfo = new SpannableString(
                        subreddit + "\n" + link.getScore() + " by " + link.getAuthor());
            }
            else {
                subreddit = "";
                spannableInfo = new SpannableString(" " + link.getScore() + " by " + link.getAuthor());
            }

            spannableInfo.setSpan(new ForegroundColorSpan(itemView.getContext().getResources()
                            .getColor(
                                    R.color.darkThemeTextColorMuted)), 0,
                    subreddit.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            spannableInfo.setSpan(
                    new ForegroundColorSpan(link.getScore() > 0 ? itemView.getContext().getResources()
                            .getColor(
                                    R.color.positiveScore) :
                            itemView.getContext().getResources()
                                    .getColor(
                                            R.color.negativeScore)),
                    subreddit.length() + 1,
                    subreddit.length() + 1 + scoreLength, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            spannableInfo.setSpan(new ForegroundColorSpan(itemView.getContext().getResources()
                            .getColor(
                                    R.color.darkThemeTextColorMuted)),
                    subreddit.length() + 1 + scoreLength, spannableInfo.length(),
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            textThreadInfo.setText(spannableInfo);

            textHidden.setText(DateUtils.getRelativeTimeSpanString(link.getCreatedUtc()) + "\n" + link.getNumComments() + " comments");
        }

        @Override
        public void setAlbum(Link link, Album album) {
            super.setAlbum(link, album);
            imageThumbnail.setVisibility(View.GONE);
            ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).addRule(
                    RelativeLayout.START_OF,
                    buttonComments.getId());
            ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).setMarginEnd(
                    0);
        }

        @Override
        public void onRecycle() {
            super.onRecycle();
            expandFull(false);
            ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).removeRule(
                    RelativeLayout.START_OF);
            ((RelativeLayout.LayoutParams) textThreadTitle.getLayoutParams()).setMarginEnd(titleMargin);
        }
    }

}