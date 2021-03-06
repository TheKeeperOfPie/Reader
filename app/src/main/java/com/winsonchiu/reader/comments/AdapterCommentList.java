/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.comments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.winsonchiu.reader.ActivityMain;
import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.ControllerUser;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.adapter.AdapterBase;
import com.winsonchiu.reader.adapter.AdapterCallback;
import com.winsonchiu.reader.adapter.AdapterDataListener;
import com.winsonchiu.reader.adapter.AdapterListener;
import com.winsonchiu.reader.data.reddit.Comment;
import com.winsonchiu.reader.data.reddit.Likes;
import com.winsonchiu.reader.data.reddit.Link;
import com.winsonchiu.reader.data.reddit.User;
import com.winsonchiu.reader.links.AdapterLink;
import com.winsonchiu.reader.links.AdapterLinkGrid;
import com.winsonchiu.reader.links.AdapterLinkList;
import com.winsonchiu.reader.profile.ControllerProfile;
import com.winsonchiu.reader.theme.Themer;
import com.winsonchiu.reader.utils.CallbackYouTubeDestruction;
import com.winsonchiu.reader.utils.OnTouchListenerDisallow;
import com.winsonchiu.reader.utils.UtilsAnimation;
import com.winsonchiu.reader.utils.UtilsInput;
import com.winsonchiu.reader.utils.UtilsReddit;
import com.winsonchiu.reader.utils.UtilsTheme;
import com.winsonchiu.reader.utils.UtilsView;
import com.winsonchiu.reader.utils.ViewHolderBase;
import com.winsonchiu.reader.utils.YouTubeListener;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;

/**
 * Created by TheKeeperOfPie on 3/12/2015.
 */

public class AdapterCommentList extends AdapterBase<RecyclerView.ViewHolder> implements CallbackYouTubeDestruction, AdapterDataListener<CommentsModel> {

    private static final String TAG = AdapterCommentList.class.getCanonicalName();

    private static final int VIEW_LINK = 0;
    private static final int VIEW_COMMENT = 1;

    private static final int ALPHA_LEVELS = 14;
    private static final int MAX_ALPHA = 180;

    private static final Interpolator INTERPOLATOR_LEVEL = new AccelerateInterpolator();

    // TODO: Find a way to animate with the toolbar actions expanded without lagging
    private boolean actionsExpanded;

    private FragmentActivity activity;
    private AdapterLink.ViewHolderLink.Listener listenerLink;
    private YouTubeListener youTubeListener;
    private CallbackYouTubeDestruction callbackYouTubeDestruction;
    protected List<RecyclerView.ViewHolder> viewHolders = new ArrayList<>();

    private AdapterLink.ViewHolderLink viewHolderLink;
    private boolean isGrid;
    private String firstLinkName;
    private int colorLink;
    private boolean animationFinished;

    private AdapterListener adapterListener;
    private ViewHolderComment.Listener listenerComment;

    private CommentsModel data = new CommentsModel();

    @Inject ControllerUser controllerUser;

    public AdapterCommentList(FragmentActivity activity,
            AdapterListener adapterListener,
            ViewHolderComment.Listener listenerCommnent,
            AdapterLink.ViewHolderLink.Listener listenerLink,
            YouTubeListener youTubeListener,
            CallbackYouTubeDestruction callbackYouTubeDestruction,
            boolean isGrid,
            String firstLinkName,
            int colorLink,
            boolean actionsExpanded) {
        ((ActivityMain) activity).getComponentActivity().inject(this);
        this.activity = activity;
        this.listenerComment = listenerCommnent;
        this.adapterListener = adapterListener;
        this.listenerLink = listenerLink;
        this.youTubeListener = youTubeListener;
        this.callbackYouTubeDestruction = callbackYouTubeDestruction;
        this.isGrid = isGrid;
        this.firstLinkName = firstLinkName;
        this.colorLink = colorLink;
        this.actionsExpanded = actionsExpanded;
        setAdapterLoadMoreListener(adapterListener);
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_LINK : VIEW_COMMENT;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        if (viewType == VIEW_LINK) {
            if (isGrid) {
                viewHolderLink = new AdapterLinkGrid.ViewHolder(
                        activity,
                        parent,
                        adapterCallback,
                        adapterListener,
                        listenerLink,
                        Source.NONE,
                        callbackYouTubeDestruction) {

                    @Override
                    protected Intent getShareIntent() {
                        return UtilsReddit.getShareIntentLinkComments(link);
                    }

                    @Override
                    public boolean isInHistory() {
                        return false;
                    }
                    @Override
                    public void loadBackgroundColor() {
                        if (colorLink != 0 && (link.getName().equals(firstLinkName))) {
                            link.setBackgroundColor(colorLink);
                            itemView.setBackgroundColor(colorLink);
                            setTextColors(colorLink, link.getTextTitleColor(), link.getTextBodyColor());
                        }
                        else {
                            super.loadBackgroundColor();
                        }
                        colorLink = 0;
                    }

                    @Override
                    public void onBind(Link link, @Nullable User user, boolean showSubreddit) {
                        super.onBind(link, user, showSubreddit);
                        if (actionsExpanded) {
                            setToolbarMenuVisibility();
                            showToolbarActionsInstant();
                        }
                        if (animationFinished) {
                            if (!TextUtils.isEmpty(link.getSelfText()) && textThreadSelf.getVisibility() != View.VISIBLE) {
                                UtilsAnimation.animateExpandHeight(textThreadSelf, UtilsView.getContentWidth(adapterCallback.getRecyclerView().getLayoutManager()), 0, null);
                            }
                        }
                    }

                    @Override
                    public void onRecycle() {
                        boolean selfShown = textThreadSelf.getVisibility() == View.VISIBLE;
                        super.onRecycle();
                        if (selfShown) {
                            textThreadSelf.setVisibility(View.VISIBLE);
                        }

                        actionsExpanded = false;
                    }

                    @Override
                    public void onClickComments() {
                        listener.onShowComments(link, this, source);
                    }

                    @Override
                    public void addToHistory() {
                        // Override to prevent adding to history
                    }
                };
            }
            else {
                viewHolderLink = new AdapterLinkList.ViewHolder(
                        activity,
                        parent,
                        adapterCallback,
                        adapterListener,
                        listenerLink,
                        Source.NONE,
                        callbackYouTubeDestruction) {

                    @Override
                    protected Intent getShareIntent() {
                        return UtilsReddit.getShareIntentLinkComments(link);
                    }

                    @Override
                    public boolean isInHistory() {
                        return false;
                    }

                    @Override
                    public void onBind(Link link, User user, boolean showSubreddit) {
                        super.onBind(link, user, showSubreddit);
                        if (actionsExpanded) {
                            setToolbarMenuVisibility();
                            showToolbarActionsInstant();
                        }
                        if (animationFinished) {
                            if (!TextUtils.isEmpty(link.getSelfText()) && textThreadSelf.getVisibility() != View.VISIBLE) {
                                UtilsAnimation.animateExpandHeight(textThreadSelf, UtilsView.getContentWidth(adapterCallback.getRecyclerView().getLayoutManager()), 0, null);
                            }
                        }
                    }

                    @Override
                    public void onRecycle() {
                        boolean selfShown = textThreadSelf.getVisibility() == View.VISIBLE;
                        super.onRecycle();
                        if (selfShown) {
                            textThreadSelf.setVisibility(View.VISIBLE);
                        }

                        actionsExpanded = false;
                    }

                    @Override
                    public void onClickComments() {
                        listener.onShowComments(link, this, source);
                    }

                    @Override
                    public void addToHistory() {
                        // Override to prevent adding to history
                    }
                };
            }

            viewHolderLink.setYouTubeListener(youTubeListener);

            return viewHolderLink;
        }

        return new ViewHolderComment(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_comment, parent, false),
                adapterCallback,
                adapterListener,
                listenerComment);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        if (holder.getItemViewType() == VIEW_LINK) {
            AdapterLink.ViewHolderLink viewHolderLink = (AdapterLink.ViewHolderLink) holder;

            viewHolderLink.onBind(data.getLink(), controllerUser.getUser(), data.isShowSubreddit());

            viewHolderLink.itemView.invalidate();

            this.viewHolderLink = viewHolderLink;
        }
        else {
            ViewHolderComment viewHolderComment = (ViewHolderComment) holder;
            viewHolderComment.onBind(data.getComments().get(position - 1), data.getUser());
        }
        viewHolders.add(holder);

    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.getItemViewType() == VIEW_LINK) {
            ((AdapterLink.ViewHolderLink) holder).onRecycle();
        }
        viewHolders.remove(holder);
    }

    @Override
    public int getItemCount() {
        int count = TextUtils.isEmpty(data.getLink().getId()) ? 0 : data.getComments().size() + 1;

        if (count > 0 && !animationFinished) {
            setAdapterLoadMoreListener(null);
            return 1;
        }

        setAdapterLoadMoreListener(adapterListener);
        return count;
    }

    public void collapseViewHolderLink(boolean expandActions) {
        // TODO: Support collapsing enlarged thumbnail and self text
        if (viewHolderLink == null) {
            return;
        }

        if (data.getLink().isSelf()) {
            viewHolderLink.textThreadSelf.setVisibility(View.GONE);
        }
        else {
            viewHolderLink.destroyWebViews();
            viewHolderLink.onRecycle();
            viewHolderLink.onBind(data.getLink(), controllerUser.getUser(), data.isShowSubreddit());
        }

        if (expandActions) {
            actionsExpanded = true;
            viewHolderLink.showToolbarActionsInstant();
        } else {
            viewHolderLink.hideToolbarActionsInstant();
        }
    }

    public void destroyViewHolderLink() {
        if (viewHolderLink != null) {
            viewHolderLink.destroyWebViews();
            destroyYouTubePlayerFragments();
        }
    }

    public void setAnimationFinished(boolean animationFinished) {
        if (this.animationFinished != animationFinished) {
            this.animationFinished = animationFinished;
            notifyDataSetChanged();
        }
    }

    public boolean isAnimationFinished() {
        return animationFinished;
    }

    public void fadeComments(Resources resources, Runnable runnable) {

        float width = resources.getDisplayMetrics().widthPixels;
        float height = resources.getDisplayMetrics().heightPixels;
        boolean listenerSet = false;
//        for (RecyclerView.ViewHolder viewHolder : viewHolders) {
//            if (getItemViewType(viewHolder.getAdapterPosition()) == VIEW_COMMENT) {
//                if (viewHolder.itemView.getWidth() < width && viewHolder.itemView.getHeight() < height) {
//                    if (listenerSet) {
//                        ViewCompat.animate(viewHolder.itemView).alpha(0).setListener(null).withLayer();
//                    }
//                    else {
//                        listenerSet = true;
//                        ViewCompat.animate(viewHolder.itemView).alpha(0).setListener(null).withLayer().withEndAction(runnable);
//                    }
//                }
//            }
//        }
        if (!listenerSet) {
            runnable.run();
        }
    }

    public AdapterLink.ViewHolderLink getViewHolderLink() {
        return viewHolderLink;
    }

    @Override
    public void destroyYouTubePlayerFragments() {
        if (viewHolderLink != null) {
            viewHolderLink.destroyYouTube();
        }
    }

    @Override
    public void setData(CommentsModel data) {
        this.data = data;

        notifyDataSetChanged();
    }

    public static class ViewHolderComment extends ViewHolderBase
            implements Toolbar.OnMenuItemClickListener {

        private final AdapterListener adapterListener;
        protected Comment comment;
        protected User user;

        protected View viewIndent;
        protected View viewIndicator;
        protected View viewIndentCollapsed;
        protected TextView textComment;
        protected TextView textInfo;
        protected TextView textHidden;
        protected ViewGroup layoutContainerReply;
        protected EditText editTextReply;
        protected TextView textUsername;
        protected Button buttonSendReply;
        protected ImageButton buttonReplyEditor;
        protected RelativeLayout layoutContainerExpand;
        protected Toolbar toolbarActions;
        protected MenuItem itemViewLink;
        protected MenuItem itemJumpParent;
        protected MenuItem itemUpvote;
        protected MenuItem itemDownvote;
        protected MenuItem itemReply;
        protected MenuItem itemSave;
        protected MenuItem itemEdit;
        protected MenuItem itemDelete;
        protected MenuItem itemReport;
        protected Drawable drawableUpvote;
        protected Drawable drawableDownvote;
        protected PorterDuffColorFilter colorFilterSave;
        protected PorterDuffColorFilter colorFilterPositive;
        protected PorterDuffColorFilter colorFilterNegative;
        protected PorterDuffColorFilter colorFilterMenuItem;
        protected View viewIndicatorContainer;
        protected View viewDivider;
        protected ViewGroup layoutContainerCollapsed;
        protected View viewIndicatorCollapsed;
        protected TextView textCollapsed;

        protected int indentWidth;
        protected int toolbarItemWidth;
        protected SharedPreferences preferences;
        protected Resources resources;
        protected int colorPrimary;
        protected int colorTextSecondary;
        protected int colorTextPrimary;
        protected int colorAccent;
        protected int colorIconFilter;
        protected int colorGold;

        protected Listener listener;

        public ViewHolderComment(View itemView,
                AdapterCallback adapterCallback,
                AdapterListener adapterListener,
                Listener listener,
                final ControllerProfile.Listener listenerProfile) {
            this(itemView, adapterCallback, adapterListener, listener);

            itemViewLink.setVisible(true);
            itemViewLink.setEnabled(true);
            itemViewLink.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            itemViewLink.setOnMenuItemClickListener(
                    item -> {
                        listenerProfile.loadLink(comment);
                        return true;
                    });
        }

        public ViewHolderComment(final View itemView,
                AdapterCallback adapterCallback,
                AdapterListener adapterListener,
                Listener listener) {
            super(itemView, adapterCallback);

            this.listener = listener;
            this.adapterListener = adapterListener;

            initialize();
            initializeToolbar();
            initializeListeners();
        }

        @SuppressWarnings("ResourceType")
        private void initialize() {
            Context context = itemView.getContext();

            resources = itemView.getResources();
            preferences = PreferenceManager.getDefaultSharedPreferences(context);

            Themer themer = new Themer(context);

            colorPrimary = themer.getColorPrimary();
            colorAccent = themer.getColorAccent();
            colorIconFilter = themer.getColorIconFilter();

            colorTextPrimary = UtilsTheme.getAttributeColor(context, android.R.attr.textColorPrimary, ContextCompat.getColor(context, R.color.darkThemeTextColor));
            colorTextSecondary = UtilsTheme.getAttributeColor(context, android.R.attr.textColorSecondary, ContextCompat.getColor(context, R.color.darkThemeTextColorMuted));
            colorGold = UtilsTheme.getAttributeColor(context, R.attr.colorGold, ContextCompat.getColor(context, R.color.darkThemeGold));

            this.drawableUpvote = resources.getDrawable(R.drawable.ic_keyboard_arrow_up_white_24dp);
            this.drawableDownvote = resources.getDrawable(
                    R.drawable.ic_keyboard_arrow_down_white_24dp);

            indentWidth = (int) TypedValue
                    .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, resources.getDisplayMetrics());
            toolbarItemWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                    resources.getDisplayMetrics());

            viewIndent = itemView.findViewById(R.id.view_indent);
            viewIndicator = itemView.findViewById(R.id.view_indicator);
            viewIndentCollapsed = itemView.findViewById(R.id.view_indent_collapsed);
            viewIndicatorContainer = itemView.findViewById(R.id.view_indicator_container);
            textComment = (TextView) itemView.findViewById(R.id.text_comment);
            textInfo = (TextView) itemView.findViewById(R.id.text_info);
            textHidden = (TextView) itemView.findViewById(R.id.text_hidden);
            layoutContainerExpand = (RelativeLayout) itemView.findViewById(
                    R.id.layout_container_expand);
            layoutContainerReply = (ViewGroup) itemView.findViewById(
                    R.id.layout_container_reply);
            editTextReply = (EditText) itemView.findViewById(R.id.edit_text_reply);
            textUsername = (TextView) itemView.findViewById(R.id.text_username);
            buttonSendReply = (Button) itemView.findViewById(R.id.button_send_reply);
            buttonReplyEditor = (ImageButton) itemView.findViewById(R.id.button_reply_editor);

            viewDivider = itemView.findViewById(R.id.view_divider);
            viewIndicatorCollapsed = itemView.findViewById(R.id.view_indicator_collapsed);
            layoutContainerCollapsed = (ViewGroup) itemView.findViewById(R.id.layout_container_collapsed);
            textCollapsed = (TextView) itemView.findViewById(R.id.text_collapsed);
        }

        private void initializeListeners() {

            textComment.setMovementMethod(LinkMovementMethod.getInstance());
            textComment.setOnTouchListener(new OnTouchListenerDisallow(adapterListener));
            editTextReply.setOnTouchListener(new OnTouchListenerDisallow(adapterListener));
            buttonSendReply.setOnClickListener(v -> {
                if (!TextUtils.isEmpty(editTextReply.getText())) {
                    sendReply();
                    UtilsInput.hideKeyboard(itemView);
                }
            });

            final GestureDetectorCompat gestureDetectorCompat = new GestureDetectorCompat(itemView.getContext(), new GestureDetector.SimpleOnGestureListener() {

                @Override
                public void onLongPress(MotionEvent e) {
                    listener.onToggleComment(comment);
                }

                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    if (!comment.isMore() && !TextUtils.isEmpty(user.getName())) {
                        listener.onVoteComment(comment, ViewHolderComment.this, Likes.UPVOTE);
                    }
                    if (layoutContainerExpand.getVisibility() == View.VISIBLE) {
                        layoutContainerExpand.clearAnimation();
                        layoutContainerExpand.setVisibility(View.GONE);
                    }
                    return true;
                }

                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    expandToolbarActions();
                    return super.onSingleTapUp(e);
                }

            });

            View.OnTouchListener onTouchListener = (v, event) -> gestureDetectorCompat.onTouchEvent(event);

            textComment.setClickable(true);
            textInfo.setClickable(true);
            itemView.setClickable(true);

            textComment.setOnTouchListener(onTouchListener);
            textInfo.setOnTouchListener(onTouchListener);
            itemView.setOnTouchListener(onTouchListener);

            editTextReply.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    comment.setReplyText(s.toString());
                }
            });
            buttonReplyEditor.setOnClickListener(v -> listener.onShowReplyEditor(comment));

        }

        private void sendReply() {
            if (comment.isEditMode()) {
                listener.onEditComment(comment, editTextReply.getText().toString());
                comment.setEdited(System.currentTimeMillis());
            }
            else {
                listener.onSendComment(comment, editTextReply.getText().toString());
            }
            comment.setReplyExpanded(false);
            layoutContainerReply.setVisibility(View.GONE);
        }

        private void initializeToolbar() {

            toolbarActions = (Toolbar) itemView.findViewById(R.id.toolbar_actions);
            toolbarActions.inflateMenu(R.menu.menu_comment);
            toolbarActions.setOnMenuItemClickListener(this);

            Menu menu = toolbarActions.getMenu();
            itemViewLink = menu.findItem(R.id.item_view_link);
            itemJumpParent = menu.findItem(R.id.item_jump_parent);
            itemUpvote = menu.findItem(R.id.item_upvote);
            itemDownvote = menu.findItem(R.id.item_downvote);
            itemReply = menu.findItem(R.id.item_reply);
            itemSave = menu.findItem(R.id.item_save);
            itemEdit = menu.findItem(R.id.item_edit);
            itemDelete = menu.findItem(R.id.item_delete);
            itemReport = menu.findItem(R.id.item_report);

            colorFilterMenuItem = new PorterDuffColorFilter(colorIconFilter,
                    PorterDuff.Mode.MULTIPLY);

            colorFilterPositive = new PorterDuffColorFilter(resources.getColor(
                    R.color.positiveScore),
                    PorterDuff.Mode.MULTIPLY);
            colorFilterNegative = new PorterDuffColorFilter(resources.getColor(
                    R.color.negativeScore),
                    PorterDuff.Mode.MULTIPLY);
            colorFilterSave = new PorterDuffColorFilter(colorAccent, PorterDuff.Mode.MULTIPLY);

            for (int index = 0; index < menu.size(); index++) {
                menu.getItem(index).getIcon().mutate().setColorFilter(colorFilterMenuItem);
            }

            buttonReplyEditor.setColorFilter(colorFilterMenuItem);
        }

        public void expandToolbarActions() {

            if (comment.getIsNew()) {
                listener.onMarkRead(comment);
                comment.setIsNew(false);
                textInfo.setTextColor(comment.getIsNew() ? itemView.getResources()
                        .getColor(R.color.textColorAlert) : colorTextSecondary);
                return;
            }

            if (comment.isMore()) {
                listener.onLoadNestedComments(comment);
//                if (!eventListener.loadNestedComments(comment, eventListenerComment.getSubredditName(), eventListenerComment.getLinkId())) {
//                    eventListenerComment.loadNestedComments(comment);
//                }
                return;
            }

            if (!toolbarActions.isShown()) {

                setVoteColors();
                setToolbarMenuVisibility();
            }

            UtilsAnimation.animateExpand(layoutContainerExpand, 1f, null);
        }

        private void setToolbarMenuVisibility() {
            // TODO: Move instances to shared class to prevent code duplication

            Menu menu = toolbarActions.getMenu();

            int maxNum = (itemView.getWidth() - comment.getLevel() * indentWidth) / toolbarItemWidth;
            int numShown = 0;

            boolean loggedIn = !TextUtils.isEmpty(user.getName());
            boolean isAuthor = comment.getAuthor().equals(user.getName());

            itemEdit.setEnabled(isAuthor);
            itemEdit.setVisible(isAuthor);
            itemDelete.setEnabled(isAuthor);
            itemDelete.setVisible(isAuthor);
            itemJumpParent.setEnabled(comment.getLevel() > 0);
            itemJumpParent.setVisible(comment.getLevel() > 0);

            itemUpvote.setVisible(loggedIn);
            itemDownvote.setVisible(loggedIn);
            itemReply.setVisible(loggedIn);
            itemSave.setVisible(loggedIn);
            itemReport.setVisible(loggedIn);

            for (int index = 0; index < menu.size(); index++) {
                if (!loggedIn) {
                    switch (menu.getItem(index)
                            .getItemId()) {
                        case R.id.item_upvote:
                        case R.id.item_downvote:
                        case R.id.item_reply:
                            continue;
                    }
                }

                if (menu.getItem(index).getItemId() == R.id.item_view_link) {
                    continue;
                }

                if (numShown++ < maxNum - 1) {
                    menu.getItem(index)
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                }
                else {
                    menu.getItem(index)
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                }
            }

            syncSaveIcon();
        }

        public void setVoteColors() {
            switch (comment.getLikes()) {
                case UPVOTE:
                    drawableUpvote.mutate().setColorFilter(colorFilterPositive);
                    itemUpvote.setIcon(drawableUpvote);
                    drawableDownvote.setColorFilter(colorFilterMenuItem);
                    itemDownvote.setIcon(drawableDownvote);
                    break;
                case DOWNVOTE:
                    drawableDownvote.mutate().setColorFilter(colorFilterNegative);
                    itemDownvote.setIcon(drawableDownvote);
                    drawableUpvote.setColorFilter(colorFilterMenuItem);
                    itemUpvote.setIcon(drawableUpvote);
                    break;
                case NONE:
                    drawableUpvote.setColorFilter(colorFilterMenuItem);
                    itemUpvote.setIcon(drawableUpvote);
                    drawableDownvote.setColorFilter(colorFilterMenuItem);
                    itemDownvote.setIcon(drawableDownvote);
                    break;
            }
            setTextInfo();
        }

        private int getIndentWidth() {
            return indentWidth * comment.getLevel();
        }

        public void onBind(Comment comment, User user) {
            this.comment = comment;
            this.user = user;

            if (itemView.getBackground() != null) {
                itemView.getBackground().setState(new int[0]);
            }

            layoutContainerReply
                    .setVisibility(comment.isReplyExpanded() ? View.VISIBLE : View.GONE);
            layoutContainerExpand
                    .setVisibility(comment.isReplyExpanded() ? View.VISIBLE : View.GONE);

            if (comment.isReplyExpanded()) {
                editTextReply.setText(comment.getReplyText());
            }

            float alphaLevel = comment.getLevel() > 0
                    ? comment.getLevel() + 6
                    : 0;

            if (alphaLevel > ALPHA_LEVELS) {
                alphaLevel = ALPHA_LEVELS;
            }

            int interpolatedValue = (int) (MAX_ALPHA * INTERPOLATOR_LEVEL.getInterpolation(alphaLevel / ALPHA_LEVELS));

            int overlayColor = ColorUtils.setAlphaComponent(0xFF000000, interpolatedValue);
            int indicatorColor = ColorUtils.compositeColors(overlayColor, colorPrimary);

            viewIndicator.setBackgroundColor(indicatorColor);
            viewIndicatorContainer.setBackgroundColor(indicatorColor);

            if (comment.getCollapsed() > 0) {
                viewIndicatorCollapsed.setBackgroundColor(indicatorColor);
                textCollapsed.setText(resources.getString(R.string.comments_collapsed, comment.getCollapsed()));
                layoutContainerCollapsed.setVisibility(View.VISIBLE);
                viewDivider.setVisibility(View.VISIBLE);
            }
            else {
                layoutContainerCollapsed.setVisibility(View.GONE);
                viewDivider.setVisibility(View.INVISIBLE);
            }

            ViewGroup.LayoutParams layoutParamsIndent = viewIndent.getLayoutParams();
            layoutParamsIndent.width = comment.getLevel() * indentWidth;
            viewIndent.setLayoutParams(layoutParamsIndent);

            ViewGroup.LayoutParams layoutParamsIndentCollapsed = viewIndentCollapsed.getLayoutParams();
            layoutParamsIndentCollapsed.width = (comment.getLevel() + 1) * indentWidth;
            viewIndentCollapsed.setLayoutParams(layoutParamsIndentCollapsed);

            if (comment.isMore()) {
                if (comment.getCount() == 0) {
                    textComment.setText(R.string.continue_thread);
                }
                else {
                    textComment.setText(R.string.load_more_comments);
                }
                textInfo.setText("");
            }
            else {
                textComment.setText(comment.getBodyHtml());

                setTextInfo();

                if (comment.getEdited() > 1) {
                    textHidden.setText(
                            "Edited " + DateUtils.getRelativeTimeSpanString(comment.getEdited()));
                    textHidden.setVisibility(View.VISIBLE);
                }
                else {
                    textHidden.setVisibility(View.GONE);
                }

            }

            textComment.setTextColor(
                    comment.getGilded() > 0 ? colorGold :
                            colorTextPrimary);
        }

        public void setTextInfo() {

            int colorPositive = resources.getColor(R.color.positiveScore);
            int colorNegative = resources.getColor(R.color.negativeScore);


            String voteIndicator = "";
            int voteColor = 0;

            switch (comment.getLikes()) {
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

            Spannable spannableScore;

            if (comment.isScoreHidden()) {
                spannableScore = new SpannableString(String.valueOf(0));
            }
            else {
                spannableScore = new SpannableString(String.valueOf(comment.getScore()));
                spannableScore.setSpan(new ForegroundColorSpan(
                                comment.getScore() > 0 ? colorPositive : colorNegative), 0,
                        spannableScore.length(),
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            }

            String suffixAuthor = "";
            int color = colorTextSecondary;

            if (comment.getLinkAuthor().equals(comment.getAuthor())) {
                color = colorAccent;
            }
            else if (comment.getAuthor().equals(user.getName())) {
                color = colorPrimary;
            }
            else {
                switch (comment.getDistinguished()) {
                    case MODERATOR:
                        color = resources.getColor(R.color.moderator);
                        suffixAuthor = resources.getString(R.string.prefix_moderator);
                        break;
                    case ADMIN:
                        color = resources.getColor(R.color.admin);
                        suffixAuthor = resources.getString(R.string.prefix_admin);
                        break;
                    case SPECIAL:
                        color = resources.getColor(R.color.special);
                        suffixAuthor = resources.getString(R.string.prefix_special);
                        break;
                }
            }

            Spannable spannableAuthor = new SpannableString(comment.getAuthor() + suffixAuthor);
            spannableAuthor.setSpan(new ForegroundColorSpan(color), 0, spannableAuthor.length(),
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

            String flair = TextUtils.isEmpty(comment.getAuthorFlairText()) || "null"
                    .equals(comment.getAuthorFlairText()) ? " " : " (" + Html
                    .fromHtml(comment.getAuthorFlairText()) + ") ";

            CharSequence timestamp =
                    preferences.getBoolean(AppSettings.PREF_FULL_TIMESTAMPS, false) ? DateUtils
                            .formatDateTime(itemView.getContext(), comment.getCreatedUtc(),
                                    DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR) :
                            DateUtils.getRelativeTimeSpanString(comment.getCreatedUtc());

            textInfo.setTextColor(comment.getIsNew() ? resources.getColor(
                    R.color.textColorAlert) : colorTextSecondary);

            textInfo.setText(TextUtils
                    .concat(spannableVote, spannableScore, " by ", spannableAuthor, flair,
                            timestamp, comment.getEdited() > 0 ? "*" : ""));

            Linkify.addLinks(textInfo, Linkify.WEB_URLS);
        }

        public void syncSaveIcon() {
            if (comment.isSaved()) {
                itemSave.getIcon().mutate().setColorFilter(colorFilterSave);
            }
            else {
                itemSave.getIcon().setColorFilter(colorFilterMenuItem);
            }
        }

        public void toggleReply() {
            comment.setReplyExpanded(!comment.isReplyExpanded());
            buttonSendReply.setText(
                    comment.isEditMode() ? itemView.getContext().getString(R.string.send_edit) :
                            itemView.getContext().getString(R.string.send_reply));
            layoutContainerReply.setVisibility(
                    comment.isReplyExpanded() ? View.VISIBLE : View.GONE);
            if (comment.isReplyExpanded()) {
                textUsername.setText("- " + user.getName());
                adapterListener.clearDecoration();
                editTextReply.setText(comment.getReplyText());
                editTextReply.clearFocus();
                editTextReply.requestFocus();
                UtilsInput.showKeyboard(editTextReply);
            }
            else {
                UtilsInput.hideKeyboard(editTextReply);
            }
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.item_jump_parent:
                    listener.onJumpToParent(comment);
                    break;
                case R.id.item_upvote:
                    listener.onVoteComment(comment, ViewHolderComment.this, Likes.UPVOTE);
                    break;
                case R.id.item_downvote:
                    listener.onVoteComment(comment, ViewHolderComment.this, Likes.DOWNVOTE);
                    break;
                case R.id.item_reply:
                    comment.setEditMode(false);
                    comment.setReplyText(comment.getReplyText());
                    toggleReply();
                    break;
                case R.id.item_save:
                    saveComment(comment);
                    break;
                case R.id.item_view_profile:
                    Intent intent = new Intent(itemView.getContext(), ActivityMain.class);
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.putExtra(ActivityMain.REDDIT_PAGE,
                            "https://reddit.com/user/" + comment.getAuthor());
                    listener.onViewProfile(comment);
                    break;
                case R.id.item_copy_text:
                    ClipboardManager clipboard = (ClipboardManager) itemView.getContext()
                            .getSystemService(
                                    Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText(
                            resources.getString(R.string.comment),
                            comment.getBody());
                    clipboard.setPrimaryClip(clip);
                    listener.onCopyText(comment);
                    break;
                case R.id.item_edit:
                    comment.setEditMode(true);
                    comment.setReplyText(comment.getBody());
                    toggleReply();
                    break;
                case R.id.item_delete:
                    // TODO: Test if truncate needed
                    listener.onDeleteComment(comment);

                    new AlertDialog.Builder(itemView.getContext())
                            .setTitle("Delete comment?")
                            .setMessage(comment.getBodyHtml())
                            .setPositiveButton("Yes",
                                    (dialog, which) -> {
//                                        eventListenerComment.deleteComment(comment)
//                                                .subscribe(new FinalizingSubscriber<String>() {
//                                                    @Override
//                                                    public void error(Throwable e) {
//                                                        Toast.makeText(itemView.getContext(), R.string.error_deleting_comment, Toast.LENGTH_LONG).show();
//                                                    }
//                                                });
                                    })
                            .setNegativeButton("No", null)
                            .show();
                    break;
                case R.id.item_report:
                    listener.onReport(comment);
                    break;
            }
            return true;
        }

        private void saveComment(final Comment comment) {
            listener.onSave(comment);
            syncSaveIcon();
        }

        public void setVisibility(int visibility) {
            viewIndent.setVisibility(visibility);
            viewIndicator.setVisibility(visibility);
            viewIndentCollapsed.setVisibility(visibility);
            viewIndicatorContainer.setVisibility(visibility);
            textComment.setVisibility(visibility);
            textInfo.setVisibility(visibility);
            textHidden.setVisibility(visibility);
            layoutContainerReply.setVisibility(visibility);
            editTextReply.setVisibility(visibility);
            buttonSendReply.setVisibility(visibility);
            toolbarActions.setVisibility(visibility);
            layoutContainerExpand.setVisibility(visibility);
            itemView.setVisibility(visibility);
        }

        public interface Listener {
            void onToggleComment(Comment comment);
            void onShowReplyEditor(Comment comment);
            void onEditComment(Comment comment, String text);
            void onSendComment(Comment comment, String text);
            void onMarkRead(Comment comment);
            void onLoadNestedComments(Comment comment);
            void onJumpToParent(Comment comment);
            void onViewProfile(Comment comment);
            void onCopyText(Comment comment);
            void onDeleteComment(Comment comment);
            void onReport(Comment comment);
            void onVoteComment(Comment comment, ViewHolderComment viewHolderComment, Likes vote);
            void onSave(Comment comment);
        }

        public interface EventListenerComment {
            boolean toggleComment(Comment comment);
            Observable<String> deleteComment(Comment comment);
            void editComment(String name, int level, String text);
            Observable<String> voteComment(ViewHolderComment viewHolderComment, Comment comment, Likes vote);
            void jumpToParent(Comment comment);
            String getLinkId();
            String getSubredditName();
            void loadNestedComments(Comment comment);
        }

        public interface EventListener {
            boolean loadNestedComments(Comment comment, String subreddit, String linkId);
        }
    }
}
