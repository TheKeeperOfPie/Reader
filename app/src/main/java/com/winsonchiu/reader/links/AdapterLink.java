/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.links;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;
import com.squareup.picasso.Picasso;
import com.winsonchiu.reader.ActivityMain;
import com.winsonchiu.reader.ApiKeys;
import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.CustomApplication;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.adapter.AdapterBase;
import com.winsonchiu.reader.adapter.AdapterCallback;
import com.winsonchiu.reader.adapter.AdapterDataListener;
import com.winsonchiu.reader.adapter.AdapterListener;
import com.winsonchiu.reader.comments.Source;
import com.winsonchiu.reader.data.imgur.Album;
import com.winsonchiu.reader.data.imgur.Image;
import com.winsonchiu.reader.data.reddit.Comment;
import com.winsonchiu.reader.data.reddit.Likes;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.Reddit;
import com.winsonchiu.reader.data.reddit.Replyable;
import com.winsonchiu.reader.data.reddit.Subreddit;
import com.winsonchiu.reader.data.reddit.Thing;
import com.winsonchiu.reader.data.reddit.User;
import com.winsonchiu.reader.glide.RequestListenerCompletion;
import com.winsonchiu.reader.history.Historian;
import com.winsonchiu.reader.rx.FinalizingSubscriber;
import com.winsonchiu.reader.theme.Themer;
import com.winsonchiu.reader.utils.BaseMediaPlayerControl;
import com.winsonchiu.reader.utils.BaseTextWatcher;
import com.winsonchiu.reader.utils.CallbackYouTubeDestruction;
import com.winsonchiu.reader.utils.CustomColorFilter;
import com.winsonchiu.reader.utils.OnTouchListenerDisallow;
import com.winsonchiu.reader.utils.SimplePlayerStateChangeListener;
import com.winsonchiu.reader.utils.Utils;
import com.winsonchiu.reader.utils.UtilsAnimation;
import com.winsonchiu.reader.utils.UtilsColor;
import com.winsonchiu.reader.utils.UtilsImage;
import com.winsonchiu.reader.utils.UtilsInput;
import com.winsonchiu.reader.utils.UtilsJson;
import com.winsonchiu.reader.utils.UtilsReddit;
import com.winsonchiu.reader.utils.UtilsRx;
import com.winsonchiu.reader.utils.UtilsTheme;
import com.winsonchiu.reader.utils.UtilsView;
import com.winsonchiu.reader.utils.ViewHolderBase;
import com.winsonchiu.reader.utils.YouTubeListener;
import com.winsonchiu.reader.views.ImageViewZoom;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import butterknife.BindColor;
import butterknife.BindDimen;
import butterknife.BindDrawable;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by TheKeeperOfPie on 3/14/2015.
 */
public abstract class AdapterLink extends AdapterBase<ViewHolderBase> implements CallbackYouTubeDestruction, AdapterDataListener<LinksModel> {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_LINK = 1;

    private static final String TAG = AdapterLink.class.getCanonicalName();
    private static final int TIMESTAMP_BITMASK = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR;
    public static final String TAG_PICASSO = "picassoAdapterLink";

    protected FragmentActivity activity;
    protected AdapterListener adapterListener;
    protected LayoutManager layoutManager;
    protected List<ViewHolderBase> viewHolders = new ArrayList<>();

    protected LinksModel data = new LinksModel();

    protected ViewHolderHeader.EventListener eventListenerHeader;
    protected ViewHolderLink.Listener listenerLink;

    @Inject SharedPreferences preferences;

    public AdapterLink(FragmentActivity activity,
            AdapterListener adapterListener,
            ViewHolderHeader.EventListener eventListenerHeader,
            ViewHolderLink.Listener listenerLink) {
        setAdapterLoadMoreListener(adapterListener);
        this.activity = activity;
        this.adapterListener = adapterListener;
        this.eventListenerHeader = eventListenerHeader;
        this.listenerLink = listenerLink;
        viewHolders = new ArrayList<>();

        ((ActivityMain) activity).getComponentActivity().inject(this);
    }

    @Override
    public void setData(LinksModel newData) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return data.getLinks().size() + 1;
            }

            @Override
            public int getNewListSize() {
                return newData.getLinks().size() + 1;
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                if (oldItemPosition == 0 && newItemPosition == 0) {
                    return true;
                }
                else if (oldItemPosition == 0 || newItemPosition == 0) {
                    return false;
                }

                return data.getLinks().get(oldItemPosition - 1).getName().equals(newData.getLinks().get(newItemPosition - 1).getName());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return areItemsTheSame(oldItemPosition, newItemPosition);
            }
        });

        this.data = newData;

        diffResult.dispatchUpdatesTo(this);
    }

    public LayoutManager getLayoutManager() {
        return layoutManager;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_HEADER : TYPE_LINK;
    }

    @Override
    public int getItemCount() {
        return data.getLinks().size() + 1;
    }

    @Override
    @CallSuper
    public void onBindViewHolder(ViewHolderBase holder, int position) {
        super.onBindViewHolder(holder, position);
        Reddit.incrementBind();
        viewHolders.add(holder);
    }

    @Override
    public void onViewRecycled(ViewHolderBase holder) {
        super.onViewRecycled(holder);
        holder.onRecycle();

        Reddit.incrementRecycled();
        viewHolders.remove(holder);
    }

    public void setVisibility(int visibility) {
        for (ViewHolderBase viewHolder : viewHolders) {
            viewHolder.itemView.setVisibility(visibility);
        }
    }

    public void setVisibility(int visibility, @NonNull Thing thing) {
        for (ViewHolderBase viewHolder : viewHolders) {
            if (viewHolder.getItemViewType() == TYPE_LINK && thing.getId().equals(((ViewHolderLink) viewHolder).link.getId())) {
                viewHolder.itemView.setVisibility(visibility);
            }
        }
    }

    public void pauseViewHolders() {
        for (ViewHolderBase viewHolder : viewHolders) {
            if (viewHolder.getItemViewType() == TYPE_LINK) {
                ViewHolderLink viewHolderLink = (ViewHolderLink) viewHolder;
                if (viewHolderLink.mediaPlayer != null) {
                    viewHolderLink.mediaPlayer.stop();
                }
            }
        }

        destroyYouTubePlayerFragments();
    }

    public boolean navigateBack() {
        boolean navigateBack = true;

        for (ViewHolderBase viewHolder : viewHolders) {
            if (viewHolder.getItemViewType() == TYPE_LINK) {
                ViewHolderLink viewHolderLink = (ViewHolderLink) viewHolder;

                if (viewHolderLink.youTubePlayer != null) {
                    if (viewHolderLink.isYouTubeFullscreen) {
                        viewHolderLink.youTubePlayer.setFullscreen(false);
                        navigateBack = false;
                        break;
                    }
                }
            }
        }

        return navigateBack;
    }

    public void destroyViewHolders() {
        for (ViewHolderBase viewHolder : viewHolders) {
            Reddit.incrementRecycled();
            viewHolder.onRecycle();
        }
    }

    @Override
    public void destroyYouTubePlayerFragments() {
        for (ViewHolderBase viewHolder : viewHolders) {
            if (viewHolder.getItemViewType() == TYPE_LINK) {
                ((ViewHolderLink) viewHolder).destroyYouTube();
            }
        }
    }

    public static class ViewHolderHeader extends ViewHolderBase implements View.OnClickListener {

        @BindView(R.id.text_name) TextView textName;
        @BindView(R.id.text_title) TextView textTitle;
        @BindView(R.id.text_description) TextView textDescription;
        @BindView(R.id.layout_buttons) LinearLayout layoutButtons;
        @BindView(R.id.button_submit_link) Button buttonSubmitLink;
        @BindView(R.id.button_submit_self) Button buttonSubmitSelf;
        @BindView(R.id.layout_container_expand) RelativeLayout layoutContainerExpand;
        @BindView(R.id.text_hidden) TextView textHidden;
        @BindView(R.id.button_show_sidebar) ImageButton buttonShowSidebar;

        @BindString(R.string.submit_link) String defaultTextSubmitLink;
        @BindString(R.string.submit_text) String defaultTextSubmitText;

        private EventListener eventListener;

        public ViewHolderHeader(View itemView,
                AdapterCallback adapterCallback,
                EventListener eventListener) {
            super(itemView, adapterCallback);
            this.eventListener = eventListener;
            ButterKnife.bind(this, itemView);

            textDescription.setMovementMethod(LinkMovementMethod.getInstance());

            if (itemView.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams) {
                ((StaggeredGridLayoutManager.LayoutParams) itemView.getLayoutParams()).setFullSpan(
                        true);
            }

            buttonShowSidebar.setColorFilter(UtilsTheme.getAttributeColor(itemView.getContext(), R.attr.colorIconFilter, 0), PorterDuff.Mode.MULTIPLY);
        }

        private void setVisibility(int visibility) {
            textName.setVisibility(visibility);
            textTitle.setVisibility(visibility);
            textDescription.setVisibility(visibility);
            textHidden.setVisibility(visibility);
            layoutButtons.setVisibility(visibility);
            buttonShowSidebar.setVisibility(visibility);
            buttonSubmitLink.setVisibility(visibility);
            buttonSubmitSelf.setVisibility(visibility);
        }

        public void onBind(Subreddit subreddit) {
            if (TextUtils.isEmpty(subreddit.getDisplayName())
                    || UtilsReddit.isAll(subreddit)
                    || UtilsReddit.isMultiple(subreddit)) {
                setVisibility(View.GONE);
                return;
            }
            setVisibility(View.VISIBLE);

            textName.setText(subreddit.getDisplayName());
            textTitle.setText(Html.fromHtml(subreddit.getTitle()));

            if (TextUtils.isEmpty(subreddit.getPublicDescription())) {
                textDescription.setText("");
                textDescription.setVisibility(View.GONE);
            }
            else {
                textDescription.setText(UtilsReddit.getFormattedHtml(subreddit.getPublicDescriptionHtml()));
                textDescription.setVisibility(View.VISIBLE);
            }

            textHidden.setText(itemView.getResources().getString(R.string.subreddit_info, subreddit.getSubscribers(), new Date(subreddit.getCreatedUtc())));

            buttonSubmitLink.setText(UtilsJson.isEmptyOrNullLiteral(subreddit.getSubmitLinkLabel())
                    ? defaultTextSubmitLink
                    : subreddit.getSubmitLinkLabel());

            buttonSubmitSelf.setText(UtilsJson.isEmptyOrNullLiteral(subreddit.getSubmitTextLabel())
                    ? defaultTextSubmitText
                    : subreddit.getSubmitTextLabel());

            if (Reddit.PostType.fromString(subreddit.getSubmissionType()) == Reddit.PostType.LINK) {
                buttonSubmitSelf.setVisibility(View.GONE);
            }
            else {
                buttonSubmitLink.setVisibility(View.GONE);
            }
        }

        @OnClick({
                R.id.button_show_sidebar,
                R.id.button_submit_link,
                R.id.button_submit_self,
                R.id.text_description,
                R.id.layout_root
        })
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.button_show_sidebar:
                    eventListener.showSidebar();
                    break;
                case R.id.button_submit_link:
                    eventListener.onClickSubmit(Reddit.PostType.LINK);
                    break;
                case R.id.button_submit_self:
                    eventListener.onClickSubmit(Reddit.PostType.SELF);
                    break;
                default:
                    UtilsAnimation.animateExpand(layoutContainerExpand, 1f, null);
                    break;
            }
        }

        public interface EventListener {
            void onClickSubmit(Reddit.PostType postType);
            void showSidebar();
        }
    }

    public static abstract class ViewHolderLink extends ViewHolderBase
            implements Toolbar.OnMenuItemClickListener,
            View.OnLongClickListener, SurfaceHolder.Callback {

        @BindView(R.id.layout_root) ViewGroup layoutRoot;
        @BindView(R.id.layout_inner) ViewGroup layoutInner;
        @BindView(R.id.layout_full) public ViewGroup layoutFull;
        @BindView(R.id.progress_image) public ProgressBar progressImage;
        @BindView(R.id.view_pager_full) public ViewPager viewPagerFull;
        @BindView(R.id.image_play) public ImageView imagePlay;
        @BindView(R.id.image_thumbnail) public ImageView imageThumbnail;
        @BindView(R.id.image_full) public ImageViewZoom imageFull;
        @BindView(R.id.view_margin) public View viewMargin;
        @BindView(R.id.button_comments) public ImageView buttonComments;
        @BindView(R.id.text_thread_flair) public TextView textThreadFlair;
        @BindView(R.id.text_thread_title) public TextView textThreadTitle;
        @BindView(R.id.text_thread_self) public TextView textThreadSelf;
        @BindView(R.id.text_thread_info) public TextView textThreadInfo;
        @BindView(R.id.text_hidden) public TextView textHidden;
        @BindView(R.id.layout_container_expand) public ViewGroup layoutContainerExpand;
        @BindView(R.id.toolbar_actions) public Toolbar toolbarActions;
        @BindView(R.id.layout_container_reply) public ViewGroup layoutContainerReply;
        @BindView(R.id.edit_text_reply) public EditText editTextReply;
        @BindView(R.id.text_username) public TextView textUsername;
        @BindView(R.id.button_send_reply) public Button buttonSendReply;
        @BindView(R.id.button_reply_editor) public ImageButton buttonReplyEditor;
        @BindView(R.id.view_overlay) public View viewOverlay;
        @BindView(R.id.layout_youtube) public ViewGroup layoutYouTube;

        @Nullable @BindView(R.id.view_mask_start) View viewMaskStart;
        @Nullable @BindView(R.id.view_mask_end) View viewMaskEnd;

        @BindDimen(R.dimen.touch_target_size) public int toolbarItemWidth;
        @BindDimen(R.dimen.activity_horizontal_margin) public int titleMargin;

        @BindDrawable(R.drawable.ic_web_white_48dp) public Drawable drawableDefault;

        @Inject protected Historian historian;
        @Inject protected Picasso picasso;
        @Inject protected Reddit reddit;
        @Inject protected SharedPreferences sharedPreferences;

        private final AdapterListener adapterListener;
        private final FragmentActivity activity;
        public Link link;
        public User user;
        public boolean showSubreddit;

        public Subscription subscription;
        public MediaController mediaController;
        public AdapterAlbum adapterAlbum;

        public YouTubePlayer youTubePlayer;
        public YouTubeListener youTubeListener;
        public YouTubePlayerSupportFragment youTubeFragment;
        public int youTubeViewId = View.generateViewId();

        public CallbackYouTubeDestruction callbackYouTubeDestruction;

        public MenuItem itemUpvote;
        public MenuItem itemDownvote;
        public MenuItem itemSave;
        public MenuItem itemReply;
        public MenuItem itemShare;
        public MenuItem itemDownloadImage;
        public MenuItem itemEdit;
        public MenuItem itemMarkNsfw;
        public MenuItem itemDelete;
        public MenuItem itemReport;
        public MenuItem itemViewSubreddit;
        public MenuItem itemCopyText;
        public CustomColorFilter colorFilterSave;
        public CustomColorFilter colorFilterPositive;
        public CustomColorFilter colorFilterNegative;
        public CustomColorFilter colorFilterIconDefault;
        public CustomColorFilter colorFilterIconLight;
        public CustomColorFilter colorFilterIconDark;
        public CustomColorFilter colorFilterMenuItem;

        @BindColor(R.color.positiveScore) int colorPositive;
        @BindColor(R.color.negativeScore) int colorNegative;
        public int colorAccent;
        public int colorTextPrimaryDefault;
        public int colorTextSecondaryDefault;
        public int colorTextAlertDefault;
        public int titleTextColor;
        public int titleTextColorAlert;
        public int colorTextSecondary;

        public boolean isYouTubeFullscreen;
        protected GestureDetectorCompat gestureDetectorDoubleTap = new GestureDetectorCompat(itemView.getContext(), new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (user != null) {
                    listener.onVote(link, ViewHolderLink.this, Likes.UPVOTE);
                }
                if (layoutContainerExpand.getVisibility() == View.VISIBLE) {
                    layoutContainerExpand.clearAnimation();
                    layoutContainerExpand.setVisibility(View.GONE);
                }
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                toggleToolbarActions();
                return super.onSingleTapUp(e);
            }

        });

        public SurfaceView surfaceVideo;
        public SurfaceHolder surfaceHolder;
        public MediaPlayer mediaPlayer;
        public Uri uriVideo;
        public int bufferPercentage;
        protected Source source;
        private boolean expanded;

        protected Listener listener;

        public ViewHolderLink(FragmentActivity activity,
                ViewGroup parent,
                int layoutResourceId,
                AdapterCallback adapterCallback,
                AdapterListener adapterListener,
                Listener listener,
                Source source,
                CallbackYouTubeDestruction callbackYouTubeDestruction) {
            super(LayoutInflater.from(parent.getContext()).inflate(layoutResourceId, parent, false), adapterCallback);
            this.adapterListener = adapterListener;
            this.activity = activity;
            this.listener = listener;
            this.source = source;
            this.callbackYouTubeDestruction = callbackYouTubeDestruction;

            ButterKnife.bind(this, itemView);
            CustomApplication.getComponentMain().inject(this);

            initialize();
            initializeToolbar();
            initializeListeners();
        }

        protected void toggleToolbarActions() {
            setToolbarValues();

            itemView.postOnAnimation(() -> {
                if (toolbarActions.isShown()) {
                    UtilsAnimation.animateCollapseHeight(layoutContainerExpand, 0, null);
                }
                else {
                    UtilsAnimation.animateExpandHeight(layoutContainerExpand, itemView.getWidth(), 0, null);
                }
            });
        }

        public void showToolbarActionsInstant() {
            setToolbarValues();

            layoutContainerExpand.setVisibility(View.VISIBLE);
            layoutContainerExpand.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            layoutContainerExpand.requestLayout();
        }

        public void hideToolbarActionsInstant()  {
            layoutContainerExpand.setVisibility(View.GONE);
        }

        private void setToolbarValues() {
            addToHistory();
            setVoteColors();

            ShareActionProvider shareActionProvider = (ShareActionProvider) MenuItemCompat
                    .getActionProvider(
                            itemShare);
            if (shareActionProvider != null) {
                shareActionProvider.setShareIntent(getShareIntent());
            }

            boolean downloadable = UtilsImage.checkIsImageUrl(link.getUrl()) || UtilsImage.placeImageUrl(link);

            itemDownloadImage.setVisible(downloadable);
            itemDownloadImage.setEnabled(downloadable);

            setToolbarMenuVisibility();
            clearOverlay();
        }

        private void sendComment() {
            // TODO: Move add to immediate on button click, check if failed afterwards

            listener.onSubmitComment(link, editTextReply.getText().toString());
            link.setReplyExpanded(false);
            layoutContainerReply.setVisibility(View.GONE);
        }

        @SuppressWarnings("ResourceType")
        protected void initialize() {
            Context context = itemView.getContext();

            Themer themer = new Themer(context);

            TypedArray typedArray = context.getTheme().obtainStyledAttributes(R.styleable.TextAttributes);

            colorTextPrimaryDefault = typedArray.getColor(R.styleable.TextAttributes_android_textColorPrimary, ContextCompat.getColor(context, R.color.darkThemeTextColor));
            colorTextSecondaryDefault = typedArray.getColor(R.styleable.TextAttributes_android_textColorSecondary, ContextCompat.getColor(context, R.color.darkThemeTextColorMuted));
            colorTextAlertDefault = themer.getColorAlert();
            colorAccent = themer.getColorAccent();

            typedArray.recycle();

            colorFilterIconDefault = themer.getColorFilterIcon();
            colorFilterIconLight = new CustomColorFilter(getColor(R.color.darkThemeIconFilter));
            colorFilterIconDark = new CustomColorFilter(getColor(R.color.lightThemeIconFilter));
            colorFilterPositive = new CustomColorFilter(colorPositive);
            colorFilterNegative = new CustomColorFilter(colorNegative);
            colorFilterSave = new CustomColorFilter(colorAccent);
            colorFilterMenuItem = colorFilterIconDefault;

            mediaController = new MediaController(context);
            adapterAlbum = new AdapterAlbum(
                    adapterListener,
                    (title, fileName, url) -> listener.onDownloadImage(link, title, fileName, url),
                    colorFilterMenuItem
            );

            viewPagerFull.setAdapter(adapterAlbum);
            viewPagerFull.setPageTransformer(false, (page, position) -> {
                if (page.getTag() instanceof AdapterAlbum.ViewHolder) {
                    AdapterAlbum.ViewHolder viewHolder = (AdapterAlbum.ViewHolder) page
                            .getTag();
                    if (Utils.inRangeInclusive(-1, position, 1)) {
                        float translationX = -position * page.getWidth();
                        viewHolder.textAlbumIndicator.setTranslationX(translationX);
                        viewHolder.layoutDownload.setTranslationX(translationX);
                    }
                }
            });

            titleTextColor = colorTextPrimaryDefault;
            titleTextColorAlert = colorTextAlertDefault;
            colorTextSecondary = colorTextSecondaryDefault;
            colorFilterMenuItem = colorFilterIconDefault;
        }

        protected void initializeListeners() {
            textThreadSelf.setMovementMethod(LinkMovementMethod.getInstance());

            imageFull.setOnTouchListener(new OnTouchListenerDisallow(adapterListener) {
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
                            if (event.getY() - startY < 0 && view.canScrollVertically(1)) {
                                disallowListener.requestDisallowInterceptTouchEventVertical(true);
                            }
                            else if (event.getY() - startY > 0 && view.canScrollVertically(-1)) {
                                disallowListener.requestDisallowInterceptTouchEventVertical(true);
                            }
                            break;
                    }
                    return false;
                }
            });
            imageFull.setListener(new ImageViewZoom.Listener() {
                @Override
                public void onTextureSizeExceeded() {
                    listener.onLoadUrl(link, true);
                }

                @Override
                public void onBeforeContentLoad(int width, int height) {
                    adapterListener.scrollAndCenter(getAdapterPosition(), height);
                }
            });

            mediaController.setMediaPlayer(new BaseMediaPlayerControl() {
                @Override
                protected MediaPlayer getMediaPlayer() {
                    return mediaPlayer;
                }

                @Override
                public int getBufferPercentage() {
                    return bufferPercentage;
                }
            });

            editTextReply.setOnTouchListener(new OnTouchListenerDisallow(adapterListener));
            editTextReply.addTextChangedListener(new BaseTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() > 0) {
                        link.setReplyText(s.toString());
                    }
                }
            });
        }

        @OnTouch({R.id.layout_root, R.id.toolbar_actions})
        protected boolean onTouchEvent(MotionEvent event) {
            return gestureDetectorDoubleTap.onTouchEvent(event);
        }

        @OnClick({
                R.id.button_comments,
                R.id.button_reply_editor,
                R.id.image_thumbnail,
                R.id.button_send_reply,
                R.id.text_thread_self
        })
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_comments:
                    onClickComments();
                    break;
                case R.id.image_thumbnail:
                    onClickThumbnail();
                    break;
                case R.id.button_send_reply:
                    if (!TextUtils.isEmpty(editTextReply.getText())) {
                        sendComment();
                    }
                    break;
                case R.id.button_reply_editor:
                    listener.onShowFullEditor(link);
                    break;
                case R.id.text_thread_self:
                    toggleToolbarActions();
                    break;
            }
        }

        @Override
        public boolean onLongClick(View v) {
            switch (v.getId()) {
                default:
                    listener.onVote(link, this, Likes.UPVOTE);
                    clearOverlay();
                    return true;
            }
        }

        protected void initializeToolbar() {
            toolbarActions.inflateMenu(R.menu.menu_link);
            toolbarActions.setOnMenuItemClickListener(this);

            Menu menu = toolbarActions.getMenu();
            itemUpvote = menu.findItem(R.id.item_upvote);
            itemDownvote = menu.findItem(R.id.item_downvote);
            itemSave = menu.findItem(R.id.item_save);
            itemReply = menu.findItem(R.id.item_reply);
            itemShare = menu.findItem(R.id.item_share);
            itemDownloadImage = menu.findItem(R.id.item_download_image);
            itemEdit = menu.findItem(R.id.item_edit);
            itemMarkNsfw = menu.findItem(R.id.item_mark_nsfw);
            itemDelete = menu.findItem(R.id.item_delete);
            itemReport = menu.findItem(R.id.item_report);
            itemViewSubreddit = menu.findItem(R.id.item_view_subreddit);
            itemCopyText = menu.findItem(R.id.item_copy_text);

            UtilsColor.tintMenu(menu, colorFilterMenuItem);

            buttonReplyEditor.setColorFilter(colorFilterMenuItem);
        }

        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.item_upvote:
                    listener.onVote(link, this, Likes.UPVOTE);
                    break;
                case R.id.item_downvote:
                    listener.onVote(link, this, Likes.DOWNVOTE);
                    break;
                case R.id.item_share:
                    break;
                case R.id.item_download_image:
                    listener.onDownloadImage(link);
                    break;
                case R.id.item_web:
                    listener.onLoadUrl(link, true);
                    break;
                case R.id.item_reply:
                    toggleReply();
                    break;
                case R.id.item_save:
                    saveLink();
                    break;
                case R.id.item_view_profile:
                    UtilsReddit.launchScreenProfile(itemView.getContext(), link);
                    break;
                case R.id.item_copy_text:
                    listener.onCopyText(link);
//                    eventListener.toast();
                    break;
                case R.id.item_edit:
                    listener.onEdit(link);
                    break;
                case R.id.item_mark_nsfw:
                    markNsfw();
                    break;
                case R.id.item_delete:
                    listener.onDelete(link);
                    break;
                case R.id.item_view_subreddit:
                    UtilsReddit.launchScreenSubreddit(itemView.getContext(), link);
                    break;
                // Reporting
                // TODO: Use report reasons from subreddit rules
                case R.id.item_report:
                    listener.onReport(link);
                    break;
            }
            return true;
        }

        protected Intent getShareIntent() {
            return UtilsReddit.getShareIntentLinkSource(link);
        }

        // TODO: Improve scrolling/centering logic
        protected void scrollToSelf() {
            adapterListener.scrollAndCenter(getAdapterPosition(), 0);
        }

        private void saveLink() {
            listener.onSave(link);
            syncSaveIcon();
        }

        public void toggleReply() {
            expandFull(true);
            link.setReplyExpanded(!link.isReplyExpanded());
            layoutContainerReply.setVisibility(link.isReplyExpanded() ? View.VISIBLE : View.GONE);

            if (link.isReplyExpanded()) {
                textUsername.setText(resources.getString(R.string.as_author, user.getName()));
                adapterListener.clearDecoration();
                editTextReply.setText(link.getReplyText());
                editTextReply.requestFocus();
                UtilsInput.showKeyboard(editTextReply);
            }
            else {
                UtilsInput.hideKeyboard(editTextReply);
            }
        }

        public void onClickComments() {
            clearOverlay();

            link.setYouTubeTime(youTubePlayer == null ? -1 : youTubePlayer.getCurrentTimeMillis());

            destroyYouTube();
            destroySurfaceView();

            itemView.post(() -> listener.onShowComments(link, ViewHolderLink.this, source));
        }

        public void onClickThumbnail() {
            clearOverlay();

            if (youTubeListener != null && !youTubeListener.hideYouTube()) {
                return;
            }
            else if (link.isSelf()) {
                if (TextUtils.isEmpty(link.getSelfText())) {
                    onClickComments();
                }
                else {
                    toggleSelfText();
                }
            }
            else {
                adapterListener.hideToolbar();

                String urlString = link.getUrl();
                if (!TextUtils.isEmpty(urlString)) {
                    if (!checkLinkUrl()) {
                        attemptLoadImage();
                    }
                }
            }
        }

        public void toggleSelfText() {
            if (textThreadSelf.isShown()) {
                UtilsAnimation.animateCollapseHeight(textThreadSelf, 0, null);
            }
            else {
                expandFull(true);
                UtilsAnimation.animateExpandHeight(textThreadSelf, UtilsView.getContentWidth(adapterCallback.getRecyclerView().getLayoutManager()), 0, null);
            }
        }

        public void addToHistory() {
            historian.add(link);
        }

        @CallSuper
        public void expandFull(boolean expand) {
            if (expanded == expand) {
                return;
            }

            this.expanded = expand;

            if (expand) {
                ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
                if ((layoutParams.width == ViewGroup.LayoutParams.WRAP_CONTENT || layoutParams.width == ViewGroup.LayoutParams.MATCH_PARENT)
                        && (!(layoutParams instanceof StaggeredGridLayoutManager.LayoutParams) || ((StaggeredGridLayoutManager.LayoutParams) layoutParams).isFullSpan())) {
                    setToolbarMenuVisibility();
                    return;
                }

                float startX = itemView.getX() - adapterCallback.getRecyclerView().getLayoutManager().getPaddingStart();

                layoutInner.getLayoutParams().width = itemView.getWidth();
                layoutInner.setTranslationX((int) startX);
                layoutInner.requestLayout();

                if (viewMaskStart != null) {
                    viewMaskStart.getLayoutParams().width = (int) startX;
                }

                if (viewMaskEnd != null) {
                    viewMaskEnd.getLayoutParams().width = (int) (adapterCallback.getRecyclerView().getWidth() - itemView.getWidth() - startX);
                }

                itemView.requestLayout();

                int targetWidth = UtilsView.getContentWidth(adapterCallback.getRecyclerView().getLayoutManager());
                itemView.postOnAnimation(() -> {
                    Log.d(TAG, "run() called with with: " + layoutInner.getLayoutParams().width);
                    UtilsAnimation.animateExpandRecyclerItemView(layoutInner, layoutRoot, viewMaskStart, viewMaskEnd, targetWidth, 0, null);

                    setToolbarMenuVisibility();
                });

                scrollToSelf();
            }
        }

        public void setVoteColors() {

            // TODO: Fix this
//            switch (link.getLikes()) {
//                case 1:
//                    itemUpvote.getIcon().mutate().setColorFilter(colorFilterPositive);
//                    itemDownvote.getIcon().mutate().setColorFilter(colorFilterMenuItem);
//                    break;
//                case -1:
//                    itemUpvote.getIcon().mutate().setColorFilter(colorFilterMenuItem);
//                    itemDownvote.getIcon().mutate().setColorFilter(colorFilterNegative);
//                    break;
//                case 0:
//                    itemUpvote.getIcon().mutate().setColorFilter(colorFilterMenuItem);
//                    itemDownvote.getIcon().mutate().setColorFilter(colorFilterMenuItem);
//                    break;
//            }

            setTextValues(link);

        }

        public void setTextValues(Link link) {
            if (TextUtils.isEmpty(link.getLinkFlairText())) {
                textThreadFlair.setVisibility(View.GONE);
            }
            else {
                textThreadFlair.setVisibility(View.VISIBLE);
                textThreadFlair.setText(link.getLinkFlairText());
            }

            textThreadTitle.setText(link.getTitle());
            syncTitleColor();

            textThreadSelf.setText(link.getSelfTextHtml());

            if (showSubreddit) {
                textThreadInfo.setText(TextUtils.concat(getSubredditString(), getSpannableScore(), link.getAuthor(), getFlairString()));
            }
            else {
                textThreadInfo.setText(TextUtils.concat(getSpannableScore(), link.getAuthor(), getFlairString()));
            }

            Linkify.addLinks(textThreadInfo, Linkify.WEB_URLS);

            textHidden.setText(resources.getString(R.string.hidden_description, getTimestamp(), link.getNumComments()));
        }

        public String getFlairString() {
            return TextUtils.isEmpty(link.getAuthorFlairText()) || "null".equals(link.getAuthorFlairText()) ? "" : " (" + Html
                    .fromHtml(link.getAuthorFlairText()) + ") ";
        }

        public String getSubredditString() {
            return showSubreddit ? "/r/" + link.getSubreddit() + "\n" : "";
        }

        public CharSequence getSpannableScore() {

            String voteIndicator = "";
            int voteColor = 0;

            switch (link.getLikes()) {
                case DOWNVOTE:
                    voteIndicator = " \u25BC";
                    voteColor = colorNegative;
                    break;
                case UPVOTE:
                    voteIndicator = " \u25B2";
                    voteColor = colorPositive;
                    break;
            }

            Spannable spannableVote = new SpannableString(voteIndicator);
            if (!TextUtils.isEmpty(spannableVote)) {
                spannableVote.setSpan(new ForegroundColorSpan(voteColor), 0, spannableVote.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            }

            Spannable spannableScore = new SpannableString(" " + link.getScore() + " ");
            spannableScore.setSpan(new ForegroundColorSpan(
                            link.getScore() > 0 ? colorPositive : colorNegative), 0,
                    spannableScore.length(),
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            return TextUtils.concat(spannableVote, spannableScore);
        }

        public CharSequence getTimestamp() {
            CharSequence timeCreated = sharedPreferences.getBoolean(AppSettings.PREF_FULL_TIMESTAMPS, false)
                    ? DateUtils.formatDateTime(itemView.getContext(), link.getCreatedUtc(), TIMESTAMP_BITMASK)
                    : DateUtils.getRelativeTimeSpanString(link.getCreatedUtc());

            if (link.getEdited() > 1) {
                CharSequence timeEdited = sharedPreferences.getBoolean(AppSettings.PREF_FULL_TIMESTAMPS, false)
                        ? DateUtils.formatDateTime(itemView.getContext(), link.getEdited(), TIMESTAMP_BITMASK)
                        : DateUtils.getRelativeTimeSpanString(link.getEdited());

                return resources.getString(R.string.link_timestamp_edited, timeEdited, timeCreated);
            }

            return timeCreated;
        }

        public boolean isInHistory() {
            return historian.contains(link.getName());
        }

        public void attemptLoadImage() {
            if (UtilsImage.placeImageUrl(link)) {
                imageFull.setVisibility(View.VISIBLE);
                progressImage.setVisibility(View.VISIBLE);
                expandFull(true);
                adapterCallback.getRecyclerView().getLayoutManager().requestLayout();
                itemView.invalidate();
                itemView.post(() -> Glide.with(itemView.getContext())
                        .load(link.getUrl())
                        .priority(Priority.IMMEDIATE)
                        .listener(new RequestListenerCompletion<String, GlideDrawable>() {
                            @Override
                            protected void onCompleted() {
                                progressImage.setVisibility(View.GONE);
                                UtilsAnimation.scrollToPositionWithCentering(getAdapterPosition(), adapterCallback.getRecyclerView(), 0, 0, 0, false);
                            }
                        })
                        .into(new GlideDrawableImageViewTarget(imageFull)));
            }
            else {
                listener.onLoadUrl(link, true);
            }
        }

        public Observer<Album> getObserverAlbum(Link link) {
            return new FinalizingSubscriber<Album>() {
                @Override
                public void start() {
                    super.start();
                    progressImage.setVisibility(View.VISIBLE);
                }

                @Override
                public void next(Album next) {
                    super.next(next);
                    setAlbum(link, next);
                }

                @Override
                public void finish() {
                    super.finish();
                    progressImage.setVisibility(View.GONE);
                }
            };
        }

        public void setAlbum(Link link, Album album) {
            link.setAlbum(album);
            ViewGroup.LayoutParams layoutParams = viewPagerFull.getLayoutParams();
            layoutParams.height = adapterCallback.getRecyclerView().getHeight();
            viewPagerFull.setLayoutParams(layoutParams);
            viewPagerFull.setVisibility(View.VISIBLE);
            viewPagerFull.requestLayout();
            adapterAlbum.setAlbum(album, colorFilterMenuItem);
            viewPagerFull.setCurrentItem(album.getPage(), false);
            scrollToSelf();
        }

        /**
         * @return true if Link loading has been handled, false otherwise
         */
        public boolean checkLinkUrl() {
            if (link.getAlbum() != null) {
                setAlbum(link, link.getAlbum());
                return true;
            }
            else if (link.getDomain()
                    .contains("imgur")) {
                expandFull(true);
                return loadImgur();
            }
            else if (link.getDomain()
                    .contains("gfycat")) {
                expandFull(true);
                return loadGfycat();
            }
            else if (link.getDomain()
                    .contains("youtu")) {
                expandFull(true);
                return loadYouTube();
            }

            return false;
        }

        private boolean loadImgur() {
            // TODO: Use regex or better parsing system
            if (link.getUrl().contains(Reddit.IMGUR_PREFIX_ALBUM)) {
                loadAlbum(Reddit.parseUrlId(link.getUrl(), Reddit.IMGUR_PREFIX_ALBUM, "/"),
                        link);
            }
            else if (link.getUrl().contains(Reddit.IMGUR_PREFIX_GALLERY)) {
                loadGallery(
                        Reddit.parseUrlId(link.getUrl(), Reddit.IMGUR_PREFIX_GALLERY, "/"),
                        link);
            }
            else if (link.getUrl().contains(UtilsImage.GIFV)) {
                loadGifv(Reddit.parseUrlId(link.getUrl(), Reddit.IMGUR_PREFIX, "."));
            }
            else {
                return false;
            }

            return true;
        }

        private boolean loadGfycat() {
            String gfycatId = Reddit.parseUrlId(link.getUrl(), Reddit.GFYCAT_PREFIX, ".");
            progressImage.setVisibility(View.VISIBLE);

            subscription = reddit.loadGfycat(gfycatId)
                    .observeOn(Schedulers.computation())
                    .flatMap(UtilsRx.flatMapWrapError(response -> new JSONObject(response).getJSONObject(Reddit.GFYCAT_ITEM)))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new FinalizingSubscriber<JSONObject>() {

                        @Override
                        public void next(JSONObject jsonObject) {
                            try {
                                loadVideo(jsonObject.getString(Reddit.GFYCAT_MP4),
                                        (float) jsonObject.getInt(
                                                "height") / jsonObject.getInt(
                                                "width"));
                            }
                            catch (JSONException e) {
                                error(e);
                            }
                        }

                        @Override
                        public void finish() {
                            progressImage.setVisibility(View.GONE);
                        }
                    });
            return true;
        }

        public boolean loadYouTube() {

            if (layoutYouTube.isShown()) {
                hideYouTube();
                return true;
            }

            if (youTubePlayer != null) {
                layoutYouTube.setVisibility(View.VISIBLE);
                return true;
            }

            Uri uri = Uri.parse(link.getUrl());
            String host = uri.getHost();

            if (host.equalsIgnoreCase("youtube.com")
                    || host.equalsIgnoreCase("youtu.be")) {
                String id = uri.getQueryParameter("v");
                if (TextUtils.isEmpty(id)) {
                    /*
                        Regex taken from Gubatron at
                        http://stackoverflow.com/questions/24048308/how-to-get-the-video-id-from-a-youtube-url-with-regex-in-java
                    */
                    Pattern pattern = Pattern.compile(".*(?:youtu.be/|v/|u/\\w/|embed/|watch\\?v=)([^#&\\?]*).*");
                    final Matcher matcher = pattern.matcher(link.getUrl());

                    if (matcher.matches()) {
                        id = matcher.group(1);
                    }
                }

                if (TextUtils.isEmpty(id)) {
                    return false;
                }

                int time = 0;
                String timeQuery = uri.getQueryParameter("t");
                if (!TextUtils.isEmpty(timeQuery)) {
                    try {
                        // YouTube query provides time in seconds, but we need milliseconds
                        time = Integer.parseInt(timeQuery) * 1000;
                    }
                    catch (NumberFormatException e) {
                        time = 0;
                    }
                }

                loadYouTubeVideo(id, time);
                return true;
            }

            return false;
        }

        private void loadGallery(String id, final Link link) {
            subscription = reddit.loadImgurGallery(id)
                    .observeOn(Schedulers.computation())
                    .flatMap(UtilsRx.flatMapWrapError(response -> Album.fromJson(new JSONObject(response).getJSONObject("data"))))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(getObserverAlbum(link));
        }

        private void loadAlbum(String id, final Link link) {
            subscription = reddit.loadImgurAlbum(id)
                    .observeOn(Schedulers.computation())
                    .flatMap(UtilsRx.flatMapWrapError(response -> Album.fromJson(new JSONObject(response).getJSONObject("data"))))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(getObserverAlbum(link));
        }

        private void loadGifv(String id) {
            reddit.loadImgurImage(id)
                    .observeOn(Schedulers.computation())
                    .flatMap(UtilsRx.flatMapWrapError(response -> Image.fromJson(new JSONObject(response).getJSONObject("data"))))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new FinalizingSubscriber<Image>() {
                        @Override
                        public void start() {
                            progressImage.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void next(Image image) {
                            if (!TextUtils.isEmpty(image.getMp4())) {
                                loadVideo(image.getMp4(), (float) image.getHeight() / image.getWidth());
                            }
                            else if (!TextUtils.isEmpty(image.getWebm())) {
                                loadVideo(image.getWebm(), (float) image.getHeight() / image.getWidth());
                            }
                        }

                        @Override
                        public void finish() {
                            progressImage.setVisibility(View.GONE);
                        }
                    });
        }

        private void loadVideo(final String url, float heightRatio) {
            uriVideo = Uri.parse(url);
            progressImage.setVisibility(View.GONE);

            if (surfaceVideo == null) {
                surfaceVideo = new SurfaceView(itemView.getContext());
                layoutFull.addView(surfaceVideo, layoutFull.getChildCount() - 1);

                surfaceVideo.setOnTouchListener((v, event) -> {
                    // TODO: Use custom MediaController
                    if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                        if (mediaController.isShowing()) {
                            mediaController.hide();
                        }
                        else {
                            mediaController.show();
                        }
                    }
                    return true;
                });
                mediaController.setAnchorView(surfaceVideo);
            }

            surfaceVideo.setVisibility(View.VISIBLE);

            surfaceVideo.getLayoutParams().height = (int) (itemView.getWidth() * heightRatio);
            surfaceVideo.requestLayout();
            surfaceVideo.setVisibility(View.GONE);
            surfaceVideo.setVisibility(View.VISIBLE);

            surfaceHolder = surfaceVideo.getHolder();
            surfaceHolder.addCallback(ViewHolderLink.this);
            surfaceHolder.setSizeFromLayout();
            scrollToSelf();
        }

        public void loadYouTubeVideo(final String id, final int timeInMillis) {
            if (youTubeListener != null) {
                youTubeListener.loadYouTubeVideo(link, id, timeInMillis);
                return;
            }

            link.setYouTubeId(id);
            callbackYouTubeDestruction.destroyYouTubePlayerFragments();
            layoutYouTube.postOnAnimation(() -> {
                youTubeFragment = new YouTubePlayerSupportFragment();
                layoutYouTube.setId(youTubeViewId);
                activity.getSupportFragmentManager()
                        .beginTransaction()
                        .add(youTubeViewId, youTubeFragment, String.valueOf(youTubeViewId))
                        .commit();
                youTubeFragment.initialize(ApiKeys.YOUTUBE_API_KEY,
                        new YouTubePlayer.OnInitializedListener() {
                            @Override
                            public void onInitializationSuccess(YouTubePlayer.Provider provider,
                                    final YouTubePlayer youTubePlayer1,
                                    boolean b) {
                                youTubePlayer = youTubePlayer1;
                                youTubePlayer.setFullscreenControlFlags(
                                        YouTubePlayer.FULLSCREEN_FLAG_CONTROL_SYSTEM_UI);
                                youTubePlayer.setManageAudioFocus(false);
                                youTubePlayer.setOnFullscreenListener(
                                        fullscreen -> {
                                            isYouTubeFullscreen = fullscreen;
                                            youTubePlayer.setFullscreen(fullscreen);
                                        });
                                youTubePlayer.setPlayerStateChangeListener(new SimplePlayerStateChangeListener() {
                                    @Override
                                    public void onVideoStarted() {
                                        youTubePlayer.seekToMillis(timeInMillis);
                                        youTubePlayer.setPlayerStateChangeListener(new SimplePlayerStateChangeListener());
                                    }
                                });
                                youTubePlayer.loadVideo(id);
                                layoutYouTube.setVisibility(View.VISIBLE);
                                scrollToSelf();
                            }

                            @Override
                            public void onInitializationFailure(YouTubePlayer.Provider provider,
                                    YouTubeInitializationResult youTubeInitializationResult) {
                                listener.onShowError(resources.getString(R.string.error_youtube));
                            }
                        });
            });
        }

        private void hideYouTube() {
            layoutYouTube.setVisibility(View.GONE);
        }

        public void setToolbarMenuVisibility() {
            boolean loggedIn = user != null;
            boolean isAuthor = loggedIn && link.getAuthor().equals(user.getName());

            itemEdit.setVisible(link.isSelf() && isAuthor);
            itemEdit.setEnabled(link.isSelf() && isAuthor);
            itemMarkNsfw.setVisible(isAuthor);
            itemMarkNsfw.setEnabled(isAuthor);
            itemDelete.setVisible(isAuthor);
            itemDelete.setEnabled(isAuthor);

            itemUpvote.setVisible(loggedIn);
            itemDownvote.setVisible(loggedIn);
            itemReply.setVisible(loggedIn);
            itemSave.setVisible(loggedIn);
            itemReport.setVisible(loggedIn);
            itemViewSubreddit.setVisible(showSubreddit);
            itemViewSubreddit.setEnabled(showSubreddit);
            itemCopyText.setVisible(link.isSelf());
            itemCopyText.setEnabled(link.isSelf());

            syncSaveIcon();

            toolbarActions.post(() -> calculateVisibleToolbarItems(itemView.getWidth()));
        }

        public void calculateVisibleToolbarItems(int width) {
            Menu menu = toolbarActions.getMenu();

            int maxNum = width / toolbarItemWidth;
            int numShown = 0;

            for (int index = 0; index < menu.size(); index++) {

                MenuItem menuItem = menu.getItem(index);

                if (!menuItem.isVisible()) {
                    continue;
                }

                if (numShown++ < maxNum - 1) {
                    menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                }
                else {
                    menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                }
            }
        }

        public void onBind(Link link, @Nullable User user, boolean showSubreddit) {
            this.link = link;
            this.user = user;
            this.showSubreddit = showSubreddit;

            if (link.isReplyExpanded() && !TextUtils.isEmpty(link.getReplyText())) {
                expandFull(true);
                editTextReply.setText(link.getReplyText());
                layoutContainerReply.setVisibility(View.VISIBLE);
            }
            else {
                link.setReplyExpanded(false);
                layoutContainerReply.setVisibility(View.GONE);
            }

            setVoteColors();

            if (sharedPreferences.getBoolean(AppSettings.PREF_DIM_POSTS, true)) {
                viewOverlay.setVisibility(isInHistory() && !link.isReplyExpanded() ? View.VISIBLE : View.GONE);
            }
        }

        public void onRecycle() {
            UtilsAnimation.clearAnimation(textThreadSelf, layoutInner);
            textThreadSelf.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            layoutInner.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
            layoutInner.setTranslationX(0);

            if (viewMaskStart != null) {
                viewMaskStart.getLayoutParams().width = 0;
            }

            if (viewMaskEnd != null) {
                viewMaskEnd.getLayoutParams().width = 0;
            }

            titleTextColor = colorTextPrimaryDefault;
            titleTextColorAlert = colorTextAlertDefault;
            colorTextSecondary = colorTextSecondaryDefault;
            colorFilterMenuItem = colorFilterIconDefault;
            isYouTubeFullscreen = false;
            layoutContainerExpand.setVisibility(View.GONE);
            itemView.setVisibility(View.VISIBLE);

            destroyWebViews();
            destroySurfaceView();
            destroyYouTube();

            UtilsRx.unsubscribe(subscription);
            subscription = null;

            picasso.cancelRequest(imageThumbnail);
            picasso.cancelRequest(imageFull);

            layoutYouTube.setVisibility(View.GONE);
            viewPagerFull.setVisibility(View.GONE);
            imagePlay.setVisibility(View.GONE);
            imageThumbnail.setVisibility(View.VISIBLE);
            progressImage.setVisibility(View.GONE);
            textThreadSelf.setVisibility(View.GONE);
            imageThumbnail.setImageDrawable(null);
            imageFull.setImageDrawable(null);
            imageFull.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            imageFull.setVisibility(View.GONE);
        }

        public void syncSaveIcon() {
            if (link.isSaved()) {
                itemSave.setTitle(resources.getString(R.string.unsave));
                itemSave.getIcon().mutate().setColorFilter(colorFilterSave);
            }
            else {
                itemSave.setTitle(resources.getString(R.string.save));
                itemSave.getIcon().setColorFilter(colorFilterMenuItem);
            }
        }

        public void markNsfw() {
            listener.onMarkNsfw(link);
            syncTitleColor();
        }

        public void syncTitleColor() {
            textThreadTitle.setTextColor(link.isOver18() ? titleTextColorAlert : titleTextColor);
            textThreadInfo.setTextColor(colorTextSecondary);
            itemMarkNsfw.setTitle(link.isOver18() ? R.string.unmark_nsfw : R.string.mark_nsfw);
        }

        public void destroyWebViews() {
            adapterAlbum.setAlbum(new Album(), null);
            layoutFull.requestLayout();
        }

        public void destroySurfaceView() {
            if (surfaceVideo != null) {
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
                surfaceVideo.setVisibility(View.GONE);
                mediaController.setAnchorView(layoutFull);
                mediaController.hide();
                layoutFull.removeView(surfaceVideo);
                surfaceVideo = null;
            }
        }

        public int getBackgroundColor() {
            return link.getBackgroundColor();
        }

        public void clearOverlay() {
            addToHistory();
            viewOverlay.setVisibility(View.GONE);
        }

        // TODO: Calculate which Views are visible
        public void setVisibility(int visibility) {
            layoutFull.setVisibility(visibility);
            progressImage.setVisibility(visibility);
            viewPagerFull.setVisibility(visibility);
            imagePlay.setVisibility(visibility);
            imageThumbnail.setVisibility(visibility);
            textThreadFlair.setVisibility(visibility);
            viewMargin.setVisibility(visibility);
            textThreadTitle.setVisibility(visibility);
            textThreadSelf.setVisibility(visibility);
            textThreadInfo.setVisibility(visibility);
            textHidden.setVisibility(visibility);
            layoutContainerExpand.setVisibility(visibility);
            toolbarActions.setVisibility(visibility);
            layoutContainerReply.setVisibility(visibility);
            editTextReply.setVisibility(visibility);
            buttonSendReply.setVisibility(visibility);
            layoutYouTube.setVisibility(visibility);
            itemView.setVisibility(visibility);
        }

        public void setYouTubeListener(YouTubeListener youTubeListener) {
            this.youTubeListener = youTubeListener;
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            surfaceHolder = holder;
            if (uriVideo == null) {
                return;
            }

            bufferPercentage = 0;
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setLooping(true);
            mediaPlayer.setOnBufferingUpdateListener((mp, percent) -> bufferPercentage = percent);
            mediaPlayer.setOnPreparedListener(mp -> {
                mediaController.hide();
                surfaceHolder.setFixedSize(mp.getVideoWidth(), mp.getVideoHeight());
                mp.start();
            });
            mediaPlayer.setDisplay(surfaceHolder);

            try {
                mediaPlayer.setDataSource(surfaceVideo.getContext(), uriVideo);
                mediaPlayer.prepareAsync();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (mediaPlayer != null) {
                mediaPlayer.reset();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            if (surfaceHolder != null) {
                surfaceHolder.removeCallback(this);
            }
        }

        public int[] getScreenAnchor() {
            int[] location = new int[2];
            if (link.isSelf()) {
                imageThumbnail.getLocationOnScreen(location);
            }
            else {
                itemView.getLocationOnScreen(location);
            }
            return location;
        }

        public Link getLink() {
            return link;
        }

        public void destroyYouTube() {
            if (youTubePlayer != null) {
                isYouTubeFullscreen = false;
                youTubePlayer.release();
                youTubePlayer = null;
            }

            if (youTubeFragment != null) {
                activity.getSupportFragmentManager()
                        .beginTransaction()
                        .remove(youTubeFragment)
                        .commit();
            }

            layoutYouTube.setVisibility(View.GONE);
        }

        public static class State implements Parcelable {
            private boolean isSelfExpanded;
            private boolean isToolbarActionsExpanded;

            @Override
            public int describeContents() {
                return 0;
            }

            @Override
            public void writeToParcel(Parcel dest, int flags) {
                dest.writeByte(isSelfExpanded ? (byte) 1 : (byte) 0);
                dest.writeByte(isToolbarActionsExpanded ? (byte) 1 : (byte) 0);
            }

            public State() {
            }

            protected State(Parcel in) {
                this.isSelfExpanded = in.readByte() != 0;
                this.isToolbarActionsExpanded = in.readByte() != 0;
            }

            public static final Creator<State> CREATOR = new Creator<State>() {
                @Override
                public State createFromParcel(Parcel source) {
                    return new State(source);
                }

                @Override
                public State[] newArray(int size) {
                    return new State[size];
                }
            };
        }

        public interface Listener {
            void onSubmitComment(Link link, String text);
            void onDownloadImage(Link link);
            void onDownloadImage(Link link, String title, String fileName, String url);
            void onLoadUrl(Link link, boolean forceExternal);
            void onShowFullEditor(Link link);
            void onVote(Link link, ViewHolderLink viewHolderLink, Likes vote);
            void onCopyText(Link link);
            void onEdit(Link link);
            void onDelete(Link link);
            void onReport(Link link);
            void onSave(Link link);
            void onShowComments(Link link, ViewHolderLink viewHolderLink, Source source);
            void onShowError(String error);
            void onMarkNsfw(Link link);
        }

        public interface EventListenerGeneral {

            void sendComment(String name, String text);
            void sendMessage(String name, String text);
            void save(Link link);
            void save(Comment comment);
            void toast(String text);
            void report(Thing thing, String reason, String otherReason);
            void hide(Link link);
            void markRead(Thing thing);
            User getUser();
            void copyText(CharSequence text);
        }

        public interface EventListener extends EventListenerGeneral {
            void onClickComments(Link link, ViewHolderLink viewHolderLink, Source source);
            void loadUrl(String url);
            void downloadImage(String title, String fileName, String url);
            void editLink(Link link);
            void showReplyEditor(Replyable replyable);
            void loadWebFragment(String url);
            void launchScreen(Intent intent);
        }

    }

}
