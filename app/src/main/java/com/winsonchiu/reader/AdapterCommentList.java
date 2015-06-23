package com.winsonchiu.reader;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.winsonchiu.reader.data.Comment;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.Subreddit;
import com.winsonchiu.reader.data.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by TheKeeperOfPie on 3/12/2015.
 */

public class AdapterCommentList extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements ControllerLinks.ListenerCallback, ControllerComments.ListenerCallback {

    private static final String TAG = AdapterCommentList.class.getCanonicalName();

    private static final int VIEW_LINK = 0;
    private static final int VIEW_COMMENT = 1;
    private static final int MAX_ALPHA = 180;
    private static final int ALPHA_LEVELS = 8;
    private final ControllerComments.CommentClickListener listener;
    private final float itemWidth;
    private final int titleMargin;
    private final int colorLink;
    private User user;

    private Activity activity;
    private SharedPreferences preferences;
    private ControllerComments controllerComments;
    private ControllerLinks.LinkClickListener linkClickListener;
    private AdapterLink.ViewHolderBase viewHolderLink;
    private int thumbnailWidth;
    private boolean isGrid;
    private boolean isInitialized;

    public AdapterCommentList(Activity activity,
            final ControllerComments controllerComments,
            final ControllerComments.CommentClickListener listener,
            boolean isGrid, int colorLink) {
        this.isGrid = isGrid;
        this.colorLink = colorLink;
        this.activity = activity;
        this.controllerComments = controllerComments;
        this.listener = listener;
        this.preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        Resources resources = activity.getResources();
        this.thumbnailWidth = resources.getDisplayMetrics().widthPixels / 2;
        this.itemWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                resources.getDisplayMetrics());
        this.titleMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16,
                resources.getDisplayMetrics());
        this.linkClickListener = new ControllerLinks.LinkClickListener() {
            @Override
            public void onClickComments(Link link, RecyclerView.ViewHolder viewHolder) {
                // Not required
            }

            @Override
            public void loadUrl(String url) {
                listener.loadUrl(url);
            }

            @Override
            public void onFullLoaded(int position) {
                // Not required
            }

            @Override
            public void setRefreshing(boolean refreshing) {
                // Not required
            }

            @Override
            public void setToolbarTitle(String title) {
                // Not required
            }

            @Override
            public AdapterLink getAdapter() {
                return null;
            }

            @Override
            public int getRecyclerHeight() {
                return listener.getRecyclerHeight();
            }

            @Override
            public void loadSideBar(Subreddit listingSubreddits) {

            }

            @Override
            public void setEmptyView(boolean visible) {
                // Not required
            }

            @Override
            public int getRecyclerWidth() {
                return listener.getRecyclerWidth();
            }

            @Override
            public void onClickSubmit(String postType) {

            }

            @Override
            public ControllerCommentsBase getControllerComments() {
                return controllerComments;
            }

            @Override
            public void setSort(Sort sort) {

            }

            @Override
            public void requestDisallowInterceptTouchEventVertical(boolean disallow) {
                listener.requestDisallowInterceptTouchEventVertical(disallow);
            }

            @Override
            public void requestDisallowInterceptTouchEventHorizontal(boolean disallow) {
                listener.requestDisallowInterceptTouchEventHorizontal(disallow);
            }
        };
        // TODO: Move current user to global instance
        this.user = new User();

        if (!TextUtils.isEmpty(preferences.getString(AppSettings.ACCOUNT_JSON, ""))) {
            try {
                this.user = User.fromJson(
                        new JSONObject(preferences.getString(AppSettings.ACCOUNT_JSON, "")));
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        if (position == 0) {
            return 0;
        }

        return Long.parseLong(controllerComments.getComment(position)
                .getId(), 36);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VIEW_LINK;
        }

        return VIEW_COMMENT;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        if (viewType == VIEW_LINK) {
            if (isGrid) {
                viewHolderLink = new AdapterLinkGrid.ViewHolder(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.cell_link, parent, false), this,
                        activity.getResources()
                                .getColor(R.color.darkThemeDialog),
                        thumbnailWidth) {

                    @Override
                    public void loadBackgroundColor(Drawable drawable, int position) {
                        itemView.setBackgroundColor(colorLink);
                    }

                    @Override
                    public void onBind(Link link) {
                        super.onBind(link);
                        if (link.isSelf() && !TextUtils.isEmpty(link.getSelfTextHtml())) {
                            textThreadSelf.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void loadSelfText(Link link) {
                        super.loadSelfText(link);
                    }

                    @Override
                    public void loadYouTubeVideo(Link link, String id) {
                        listener.loadYouTube(link, id, this);
                    }

                    @Override
                    public void onClickThumbnail(Link link) {
                        if (listener.hideYouTube()) {
                            super.onClickThumbnail(link);
                        }
                    }
                };
                // TODO: Fix margin when expanding comment thread from grid UI
//                int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4,
//                        activity.getResources()
//                                .getDisplayMetrics());
//                ((RecyclerView.LayoutParams) viewHolderLink.itemView.findViewById(R.id.layout_link).getLayoutParams()).setMarginStart(
//                        margin);
//                ((RecyclerView.LayoutParams) viewHolderLink.itemView.findViewById(R.id.layout_link).getLayoutParams()).setMarginEnd(
//                        margin);
            }
            else {
                viewHolderLink = new AdapterLinkList.ViewHolder(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.row_link, parent, false), this) {

                    @Override
                    public void onBind(Link link) {
                        super.onBind(link);
                        if (link.isSelf() && !TextUtils.isEmpty(link.getSelfTextHtml())) {
                            textThreadSelf.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void loadSelfText(Link link) {
                        super.loadSelfText(link);
                    }

                    @Override
                    public void loadYouTubeVideo(Link link, String id) {
                        listener.loadYouTube(link, id, this);
                    }

                    @Override
                    public void onClickThumbnail(Link link) {
                        if (listener.hideYouTube()) {
                            super.onClickThumbnail(link);
                        }
                    }
                };
            }

            return viewHolderLink;
        }

        return new ViewHolderComment(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_comment, parent, false), this);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        if (!controllerComments.isLoading() && position > controllerComments.sizeLinks() - 10) {
            controllerComments.loadMoreComments();
        }

        if (getItemViewType(position) == VIEW_LINK) {
            AdapterLink.ViewHolderBase viewHolderBase = (AdapterLink.ViewHolderBase) holder;

            viewHolderBase.onBind(controllerComments.getMainLink());
            if (!isInitialized) {
                if (controllerComments.getMainLink().isSelf()) {
                    viewHolderBase.onClickThumbnail(controllerComments.getMainLink());
                }
                isInitialized = true;
            }
        }
        else {
            ViewHolderComment viewHolderComment = (ViewHolderComment) holder;
            viewHolderComment.onBind(controllerComments.getComment(position));
        }

    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        viewHolderLink.destroy();
    }

    @Override
    public int getItemCount() {
        return controllerComments.getItemCount();
    }

    @Override
    public ControllerLinks.LinkClickListener getListener() {
        return linkClickListener;
    }

    @Override
    public ControllerLinksBase getControllerLinks() {
        return controllerComments;
    }

    @Override
    public float getItemWidth() {
        return itemWidth;
    }

    @Override
    public int getTitleMargin() {
        return titleMargin;
    }

    @Override
    public RecyclerView.LayoutManager getLayoutManager() {
        return null;
    }

    @Override
    public ControllerComments.CommentClickListener getCommentClickListener() {
        return listener;
    }

    @Override
    public ControllerCommentsBase getControllerComments() {
        return controllerComments;
    }

    @Override
    public void pauseViewHolders() {
        viewHolderLink.videoFull.stopPlayback();
    }

    @Override
    public SharedPreferences getPreferences() {
        return preferences;
    }

    @Override
    public User getUser() {
        return user;
    }

    public static class ViewHolderComment extends RecyclerView.ViewHolder
            implements Toolbar.OnMenuItemClickListener {

        protected Comment comment;

        protected View viewIndent;
        protected View viewIndicator;
        protected View viewIndicatorContainer;
        protected View viewIndicatorContainerReply;
        protected TextView textComment;
        protected TextView textInfo;
        protected TextView textHidden;
        protected RelativeLayout layoutContainerReply;
        protected EditText editTextReply;
        protected Button buttonSendReply;
        protected Toolbar toolbarActions;
        protected MenuItem itemViewLink;
        protected MenuItem itemCollapse;
        protected MenuItem itemUpvote;
        protected MenuItem itemDownvote;
        protected MenuItem itemReply;
        protected MenuItem itemSave;
        protected MenuItem itemDelete;
        protected PorterDuffColorFilter colorFilterSave;
        protected Drawable drawableUpvote;
        protected Drawable drawableDownvote;
        protected RelativeLayout layoutContainerExpand;
        protected ControllerComments.ListenerCallback callback;


        public ViewHolderComment(final View itemView,
                ControllerComments.ListenerCallback listenerCallback,
                final ControllerProfile.ItemClickListener listener) {
            this(itemView, listenerCallback);
            itemViewLink.setVisible(true);
            itemViewLink.setEnabled(true);
            itemViewLink.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            itemViewLink.setOnMenuItemClickListener(
                    new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            listener.loadLink(comment);
                            return true;
                        }
                    });
        }

        public ViewHolderComment(final View itemView,
                ControllerComments.ListenerCallback listenerCallback) {
            super(itemView);
            this.callback = listenerCallback;

            intiialize();
            initializeToolbar();
            initializeListeners();
        }

        private void intiialize() {

            Resources resources = callback.getControllerComments()
                    .getActivity()
                    .getResources();
            this.drawableUpvote = resources.getDrawable(R.drawable.ic_keyboard_arrow_up_white_24dp);
            this.drawableDownvote = resources.getDrawable(
                    R.drawable.ic_keyboard_arrow_down_white_24dp);

            viewIndent = itemView.findViewById(R.id.view_indent);
            viewIndicator = itemView.findViewById(R.id.view_indicator);
            viewIndicatorContainer = itemView.findViewById(R.id.view_indicator_container);
            viewIndicatorContainerReply = itemView.findViewById(
                    R.id.view_indicator_container_reply);
            textComment = (TextView) itemView.findViewById(R.id.text_comment);
            textInfo = (TextView) itemView.findViewById(R.id.text_info);
            textHidden = (TextView) itemView.findViewById(R.id.text_hidden);
            layoutContainerExpand = (RelativeLayout) itemView.findViewById(
                    R.id.layout_container_expand);
            layoutContainerReply = (RelativeLayout) itemView.findViewById(
                    R.id.layout_container_reply);
            editTextReply = (EditText) itemView.findViewById(R.id.edit_text_reply);
            buttonSendReply = (Button) itemView.findViewById(R.id.button_send_reply);
        }

        private void initializeListeners() {

            textComment.setMovementMethod(LinkMovementMethod.getInstance());
            textComment.setOnTouchListener(
                    new OnTouchListenerDisallow(callback.getCommentClickListener()));
            editTextReply.setOnTouchListener(
                    new OnTouchListenerDisallow(callback.getCommentClickListener()));
            buttonSendReply.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!TextUtils.isEmpty(editTextReply.getText())) {
                        sendReply();
                    }
                }
            });

            View.OnClickListener clickListenerLink = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    expandToolbarActions();
                }
            };
            textComment.setOnClickListener(clickListenerLink);
            textInfo.setOnClickListener(clickListenerLink);
            this.itemView.setOnClickListener(clickListenerLink);
        }

        private void sendReply() {

            final int parentLevel = comment.getLevel();
            Map<String, String> params = new HashMap<>();
            params.put("api_type", "json");
            params.put("text", editTextReply.getText()
                    .toString());
            params.put("thing_id", comment.getName());

            // TODO: Move add to immediate on button click, check if failed afterwards
            callback.getControllerComments()
                    .getReddit()
                    .loadPost(Reddit.OAUTH_URL + "/api/comment",
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    try {
                                        JSONObject jsonObject = new JSONObject(
                                                response);
                                        Comment newComment = Comment.fromJson(
                                                jsonObject.getJSONObject("json")
                                                        .getJSONObject("data")
                                                        .getJSONArray("things")
                                                        .getJSONObject(0),
                                                parentLevel + 1);
                                        callback.getControllerComments()
                                                .insertComment(newComment);
                                    }
                                    catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {

                                }
                            }, params, 0);
            comment.setReplyExpanded(!comment.isReplyExpanded());
            layoutContainerReply.setVisibility(View.GONE);
        }

        private void initializeToolbar() {

            toolbarActions = (Toolbar) itemView.findViewById(R.id.toolbar_actions);
            toolbarActions.inflateMenu(R.menu.menu_comment);
            toolbarActions.setOnMenuItemClickListener(this);

            Menu menu = toolbarActions.getMenu();
            itemViewLink = menu.findItem(R.id.item_view_link);
            itemCollapse = menu.findItem(R.id.item_collapse);
            itemUpvote = menu.findItem(R.id.item_upvote);
            itemDownvote = menu.findItem(R.id.item_downvote);
            itemReply = menu.findItem(R.id.item_reply);
            itemSave = menu.findItem(R.id.item_save);
            itemDelete = menu.findItem(R.id.item_delete);

            colorFilterSave = new PorterDuffColorFilter(
                    callback.getControllerComments().getActivity().getResources()
                            .getColor(R.color.colorAccent),
                    PorterDuff.Mode.MULTIPLY);

            toolbarActions.post(new Runnable() {
                @Override
                public void run() {
                    setToolbarMenuVisibility();
                }
            });
        }

        private void expandToolbarActions() {

            if (comment.isMore()) {
                callback.getControllerComments()
                        .loadNestedComments(comment);
                return;
            }

            if (!toolbarActions.isShown()) {

                boolean isAuthor = comment.getAuthor()
                        .equals(callback.getUser()
                                .getName());

                if (callback.getControllerComments()
                        .isCommentExpanded(getAdapterPosition())) {
                    itemCollapse.setIcon(R.drawable.ic_arrow_drop_up_white_24dp);
                }
                else {
                    itemCollapse.setIcon(R.drawable.ic_arrow_drop_down_white_24dp);
                }

                itemDelete.setEnabled(isAuthor);
                itemDelete.setVisible(isAuthor);

                setVoteColors();

                setToolbarMenuVisibility();
            }

            AnimationUtils.animateExpand(layoutContainerExpand, 1f, null);
        }

        private void setToolbarMenuVisibility() {
            // TODO: Move instances to shared class to prevent code duplication

            Menu menu = toolbarActions.getMenu();

            int maxNum = (int) (itemView.getWidth() / callback.getItemWidth());
            int numShown = 0;

            boolean loggedIn = !TextUtils.isEmpty(callback.getPreferences()
                    .getString(AppSettings.REFRESH_TOKEN, ""));

            itemUpvote.setVisible(loggedIn);
            itemDownvote.setVisible(loggedIn);
            itemReply.setVisible(loggedIn);
            itemSave.setVisible(loggedIn);
            itemCollapse.setVisible(callback.getControllerComments()
                    .hasChildren(comment));

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
        }

        public void setVoteColors() {
            switch (comment.isLikes()) {
                case 1:
                    drawableUpvote.setColorFilter(callback.getControllerComments()
                            .getActivity()
                            .getResources()
                            .getColor(
                                    R.color.positiveScore), PorterDuff.Mode.MULTIPLY);
                    itemUpvote.setIcon(drawableUpvote);
                    drawableDownvote.clearColorFilter();
                    itemDownvote.setIcon(drawableDownvote);
                    break;
                case -1:
                    drawableDownvote.setColorFilter(callback.getControllerComments()
                            .getActivity()
                            .getResources()
                            .getColor(
                                    R.color.negativeScore), PorterDuff.Mode.MULTIPLY);
                    itemDownvote.setIcon(drawableDownvote);
                    drawableUpvote.clearColorFilter();
                    itemUpvote.setIcon(drawableUpvote);
                    break;
                case 0:
                    drawableUpvote.clearColorFilter();
                    itemUpvote.setIcon(drawableUpvote);
                    drawableDownvote.clearColorFilter();
                    itemDownvote.setIcon(drawableDownvote);
                    break;
            }
        }

        public void onBind(Comment comment) {

            this.comment = comment;

            layoutContainerReply
                    .setVisibility(comment.isReplyExpanded() ? View.VISIBLE : View.GONE);
            layoutContainerExpand
                    .setVisibility(comment.isReplyExpanded() ? View.VISIBLE : View.GONE);

            int alphaLevel = comment.getLevel() * MAX_ALPHA / ALPHA_LEVELS;

            int overlayColor = ColorUtils.setAlphaComponent(0xFF000000,
                    alphaLevel <= MAX_ALPHA ? alphaLevel : MAX_ALPHA);
            int indicatorColor = ColorUtils.compositeColors(overlayColor,
                    callback.getControllerComments()
                            .getActivity()
                            .getResources()
                            .getColor(
                                    R.color.colorPrimary));

            viewIndicator.setBackgroundColor(indicatorColor);
            viewIndicatorContainer.setBackgroundColor(indicatorColor);
            viewIndicatorContainerReply.setBackgroundColor(indicatorColor);

            ViewGroup.LayoutParams layoutParams = viewIndent.getLayoutParams();
            layoutParams.width = callback.getControllerComments()
                    .getIndentWidth(comment);
            viewIndent.setLayoutParams(layoutParams);

            if (comment.isMore()) {
                textComment.setText(R.string.load_more_comments);
                textInfo.setText("");
            }
            else {
                textComment.setText(Reddit.getTrimmedHtml(comment.getBodyHtml()));

                Resources resources = callback.getControllerComments().getActivity().getResources();

                int colorPositive = resources.getColor(R.color.positiveScore);
                int colorNegative = resources.getColor(R.color.negativeScore);

                Spannable spannableScore = new SpannableString(String.valueOf(comment.getScore()));
                spannableScore.setSpan(new ForegroundColorSpan(
                                comment.getScore() > 0 ? colorPositive : colorNegative), 0,
                        spannableScore.length(),
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE);


                Spannable spannableAuthor = new SpannableString(comment.getAuthor());
                if (callback.getControllerComments().getMainLink().getAuthor()
                        .equals(comment.getAuthor())) {
                    spannableAuthor.setSpan(new ForegroundColorSpan(resources.getColor(R.color.colorAccent)), 0,
                            spannableAuthor.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                }

                textInfo.setText(TextUtils.concat(spannableScore, " by ", spannableAuthor, " ",
                        DateUtils.getRelativeTimeSpanString(
                                comment.getCreatedUtc()) + (comment.getEdited() > 0 ? "*" : "")));

                if (comment.getEdited() > 1) {
                    textHidden.setText(
                            "Edited " + DateUtils.getRelativeTimeSpanString(comment.getEdited()));
                }

            }

            if (comment.getGilded() > 0) {
                textComment.setTextColor(callback.getControllerComments()
                        .getActivity()
                        .getResources()
                        .getColor(R.color.gildedComment));
            }
            else {
                textComment.setTextColor(callback.getControllerComments()
                        .getActivity()
                        .getResources()
                        .getColor(R.color.darkThemeTextColor));
            }


        }

        public void syncSaveIcon() {
            if (comment.isSaved()) {
                itemSave.getIcon().setColorFilter(colorFilterSave);
            }
            else {
                itemSave.getIcon().clearColorFilter();
            }
        }

        public void toggleReply() {
            if (TextUtils.isEmpty(callback.getPreferences()
                    .getString(AppSettings.REFRESH_TOKEN, ""))) {
                Toast.makeText(callback.getControllerComments()
                                .getActivity(), callback.getControllerComments()
                                .getActivity()
                                .getString(R.string.must_be_logged_in_to_reply),
                        Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            if (!comment.isReplyExpanded()) {
                editTextReply.requestFocus();
                editTextReply.setText(null);
            }
            comment.setReplyExpanded(!comment.isReplyExpanded());
            layoutContainerReply.setVisibility(
                    comment.isReplyExpanded() ? View.VISIBLE : View.GONE);
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.item_collapse:
                    item.setIcon(
                            callback.getControllerComments().toggleComment(getAdapterPosition()) ?
                                    R.drawable.ic_arrow_drop_up_white_24dp :
                                    R.drawable.ic_arrow_drop_down_white_24dp);
                    break;
                case R.id.item_upvote:
                    callback.getControllerComments()
                            .voteComment(ViewHolderComment.this, comment, 1);
                    break;
                case R.id.item_downvote:
                    callback.getControllerComments()
                            .voteComment(ViewHolderComment.this, comment, -1);
                    break;
                case R.id.item_reply:
                    toggleReply();
                    break;
                case R.id.item_save:
                    saveComment(comment);
                    break;
                case R.id.item_view_profile:
                    Intent intent = new Intent(callback.getControllerComments()
                            .getActivity(), MainActivity.class);
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.putExtra(MainActivity.REDDIT_PAGE,
                            "https://reddit.com/user/" + comment.getAuthor());
                    callback.getControllerComments()
                            .getActivity()
                            .startActivity(intent);
                    break;
                case R.id.item_delete:
                    new AlertDialog.Builder(callback.getControllerComments()
                            .getActivity())
                            .setTitle("Delete comment?")
                            .setMessage(Reddit.getTrimmedHtml(comment.getBodyHtml()))
                            .setPositiveButton("Yes",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(
                                                DialogInterface dialog,
                                                int which) {

                                            callback.getControllerComments()
                                                    .deleteComment(
                                                            comment);
                                        }
                                    })
                            .setNegativeButton("No", null)
                            .show();
                    break;
            }
            return true;
        }

        private void saveComment(final Comment comment) {
            if (comment.isSaved()) {
                callback.getControllerComments().getReddit().unsave(comment);
            }
            else {
                callback.getControllerComments().getReddit().save(comment, "",
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                comment.setSaved(false);
                                syncSaveIcon();
                            }
                        });
            }
            comment.setSaved(!comment.isSaved());
            syncSaveIcon();
        }
    }
}
