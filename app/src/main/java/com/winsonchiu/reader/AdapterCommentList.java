package com.winsonchiu.reader;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.winsonchiu.reader.data.Comment;
import com.winsonchiu.reader.data.Link;
import com.winsonchiu.reader.data.Listing;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by TheKeeperOfPie on 3/12/2015.
 */

public class AdapterCommentList extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements ControllerLinks.ListenerCallback, ControllerComments.ListenerCallback {

    private static final String TAG = AdapterCommentList.class.getCanonicalName();

    private static final int VIEW_LINK = 0;
    private static final int VIEW_COMMENT = 1;
    private static final int LINK_MENU_SIZE = 4;
    private static final int COMMENT_MENU_SIZE = 5;
    private static final int MAX_ALPHA = 180;
    private static final int ALPHA_LEVELS = 8;
    private final ControllerComments.CommentClickListener listener;
    private final float itemWidth;
    private User user;

    private Activity activity;
    private ControllerComments controllerComments;
    private ControllerLinks.LinkClickListener linkClickListener;
    private int colorMuted;
    private int colorAccent;
    private int colorPrimary;
    private int colorPositive;
    private int colorNegative;
    private int colorDefault;
    private int colorText;
    private int colorTextAlert;
    private int thumbnailWidth;
    private SharedPreferences preferences;
    private boolean isGrid;
    private boolean isInitialized;

    public AdapterCommentList(Activity activity, ControllerComments controllerComments, final ControllerComments.CommentClickListener listener, boolean isGrid) {
        this.isGrid = isGrid;
        this.activity = activity;
        this.controllerComments = controllerComments;
        this.listener = listener;
        this.preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        Resources resources = activity.getResources();
        this.colorMuted = resources.getColor(R.color.darkThemeTextColorMuted);
        this.colorAccent = resources.getColor(R.color.colorAccent);
        this.colorPrimary = resources.getColor(R.color.colorPrimary);
        this.colorPositive = resources.getColor(R.color.positiveScore);
        this.colorNegative = resources.getColor(R.color.negativeScore);
        this.colorDefault = resources.getColor(R.color.darkThemeDialog);
        this.colorText = resources.getColor(R.color.darkThemeTextColor);
        this.colorTextAlert = resources.getColor(R.color.darkThemeTextColorAlert);
        this.thumbnailWidth = resources.getDisplayMetrics().widthPixels / 2;
        this.itemWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
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
            public void loadSideBar(Listing listingSubreddits) {

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
            public void requestDisallowInterceptTouchEvent(boolean disallow) {
                listener.requestDisallowInterceptTouchEvent(disallow);
            }
        };
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
                return new AdapterLinkGrid.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.cell_link, parent, false), this, colorDefault, thumbnailWidth);
            }
            return new AdapterLinkList.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_link, parent, false), this);
        }

        return new ViewHolderComment(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_comment, parent, false), this);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        if (holder instanceof AdapterLinkList.ViewHolder) {
            ((AdapterLinkList.ViewHolder) holder).onBind(position);
            if (!isInitialized) {
                if (controllerComments.getMainLink().isSelf()) {
                    ((AdapterLinkList.ViewHolder) holder).imageThumbnail.callOnClick();
                }
                isInitialized = true;
            }
        }
        else if (holder instanceof AdapterLinkGrid.ViewHolder) {
            ((AdapterLinkGrid.ViewHolder) holder).onBind(position);
            if (!isInitialized) {
                if (controllerComments.getMainLink().isSelf()) {
                    ((AdapterLinkGrid.ViewHolder) holder).imageThumbnail.callOnClick();
                }
                isInitialized = true;
            }
        }
        else {

            ViewHolderComment viewHolderComment = (ViewHolderComment) holder;
            viewHolderComment.onBind(position);
        }

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
    public ControllerLinksBase getController() {
        return controllerComments;
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
    public int getColorAccent() {
        return colorAccent;
    }

    @Override
    public int getColorPrimary() {
        return colorPrimary;
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
    public int getColorDefault() {
        return colorDefault;
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
        return null;
    }

    public void setIsGrid(boolean isGrid) {
        this.isGrid = isGrid;
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
    public SharedPreferences getPreferences() {
        return preferences;
    }

    @Override
    public User getUser() {
        return user;
    }

    protected static class ViewHolderComment extends RecyclerView.ViewHolder {

        protected View viewIndent;
        protected View viewIndicator;
        protected View viewIndicatorContainer;
        protected View viewIndicatorContainerReply;
        protected TextView textComment;
        protected TextView textInfo;
        protected RelativeLayout layoutContainerReply;
        protected EditText editTextReply;
        protected Button buttonSendReply;
        protected Toolbar toolbarActions;
        protected MenuItem itemCollapse;
        protected MenuItem itemUpvote;
        protected MenuItem itemDownvote;
        protected MenuItem itemShare;
        protected Drawable drawableUpvote;
        protected Drawable drawableDownvote;
        protected LinearLayout layoutContainerActions;
        protected ControllerComments.ListenerCallback callback;
        private View.OnClickListener clickListenerLink;

        public ViewHolderComment(final View itemView, ControllerComments.ListenerCallback listenerCallback) {
            super(itemView);
            this.callback = listenerCallback;

            Resources resources = callback.getActivity().getResources();
            this.drawableUpvote = resources.getDrawable(R.drawable.ic_keyboard_arrow_up_white_24dp);
            this.drawableDownvote = resources.getDrawable(R.drawable.ic_keyboard_arrow_down_white_24dp);
            viewIndent = itemView.findViewById(R.id.view_indent);
            viewIndicator = itemView.findViewById(R.id.view_indicator);
            viewIndicatorContainer = itemView.findViewById(R.id.view_indicator_container);
            viewIndicatorContainerReply = itemView.findViewById(R.id.view_indicator_container_reply);
            textComment = (TextView) itemView.findViewById(R.id.text_comment);
            textComment.setMovementMethod(LinkMovementMethod.getInstance());
            textInfo = (TextView) itemView.findViewById(R.id.text_info);
            layoutContainerReply = (RelativeLayout) itemView.findViewById(R.id.layout_container_reply);
            editTextReply = (EditText) itemView.findViewById(R.id.edit_text_reply);
            layoutContainerActions = (LinearLayout) itemView.findViewById(R.id.layout_container_actions);
            buttonSendReply = (Button) itemView.findViewById(R.id.button_send_reply);
            buttonSendReply.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (TextUtils.isEmpty(callback.getPreferences().getString(AppSettings.REFRESH_TOKEN, ""))) {
                        Toast.makeText(callback.getActivity(), "Must be logged in to reply", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!TextUtils.isEmpty(editTextReply.getText())) {
                        Comment comment = callback.getControllerComments().getComment(
                                getAdapterPosition());
                        final int commentIndex = getAdapterPosition();
                        final int parentLevel = comment.getLevel();
                        Map<String, String> params = new HashMap<>();
                        params.put("api_type", "json");
                        params.put("text", editTextReply.getText()
                                .toString());
                        params.put("thing_id", comment.getName());

                        // TODO: Move add to immediate on button click, check if failed afterwards
                        callback.getControllerComments().getReddit()
                                .loadPost(Reddit.OAUTH_URL + "/api/comment",
                                        new Response.Listener<String>() {
                                            @Override
                                            public void onResponse(String response) {
                                                try {
                                                    JSONObject jsonObject = new JSONObject(response);
                                                    Comment newComment = Comment.fromJson(
                                                            jsonObject.getJSONObject("json")
                                                                    .getJSONObject("data")
                                                                    .getJSONArray("things")
                                                                    .getJSONObject(0), parentLevel + 1);
                                                    callback.getControllerComments().insertComment(newComment);
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
                        int width = callback.getCommentClickListener().getRecyclerWidth();
                        float ratio = (width - callback.getControllerComments().getIndentWidth(comment)) / width;

                        AnimationUtils.animateExpand(editTextReply, ratio);
                        AnimationUtils.animateExpand(buttonSendReply, ratio);
                    }
                }
            });
            toolbarActions = (Toolbar) itemView.findViewById(R.id.toolbar_actions);
            toolbarActions.inflateMenu(R.menu.menu_comment);
            toolbarActions.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    final Comment comment;
                    final int commentIndex = getAdapterPosition();
                    switch (menuItem.getItemId()) {
                        case R.id.item_collapse:
                            if (callback.getControllerComments().toggleComment(commentIndex)) {
                                menuItem.setIcon(R.drawable.ic_arrow_drop_up_white_24dp);
                            }
                            else {
                                menuItem.setIcon(R.drawable.ic_arrow_drop_down_white_24dp);
                            }
                            break;
                        case R.id.item_upvote:
                            callback.getControllerComments().voteComment(ViewHolderComment.this, 1);
                            break;
                        case R.id.item_downvote:
                            callback.getControllerComments().voteComment(ViewHolderComment.this, -1);
                            break;
                        case R.id.item_reply:
                            comment = callback.getControllerComments().getComment(commentIndex);
                            if (!comment.isReplyExpanded()) {
                                editTextReply.requestFocus();
                                editTextReply.setText(null);
                            }
                            comment.setReplyExpanded(!comment.isReplyExpanded());
                            int width = callback.getCommentClickListener().getRecyclerWidth();
                            float ratio = (width - callback.getControllerComments().getIndentWidth(comment)) / width;
                            AnimationUtils.animateExpand(editTextReply, ratio);
                            AnimationUtils.animateExpand(buttonSendReply, ratio);
                            break;
                        case R.id.item_delete:
                            comment = callback.getControllerComments().getComment(commentIndex);
                            String html = comment.getBodyHtml();
                            html = Html.fromHtml(html.trim())
                                    .toString();

                            CharSequence sequence = Html.fromHtml(html);

                            // Trims leading and trailing whitespace
                            int start = 0;
                            int end = sequence.length();
                            while (start < end && Character.isWhitespace(sequence.charAt(start))) {
                                start++;
                            }
                            while (end > start && Character.isWhitespace(sequence.charAt(end - 1))) {
                                end--;
                            }
                            sequence = sequence.subSequence(start, end);

                            new AlertDialog.Builder(callback.getActivity())
                                    .setTitle("Delete comment?")
                                    .setMessage(sequence)
                                    .setPositiveButton("Yes",
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(
                                                        DialogInterface dialog,
                                                        int which) {

                                                    callback.getControllerComments().deleteComment(
                                                            comment);

                                                    Map<String, String> params = new HashMap<>();
                                                    params.put("id", comment.getName());

                                                    callback.getControllerComments().getReddit()
                                                            .loadPost(Reddit.OAUTH_URL + "/api/del",
                                                                    new Response.Listener<String>() {
                                                                        @Override
                                                                        public void onResponse(String response) {
                                                                        }
                                                                    }, new Response.ErrorListener() {
                                                                        @Override
                                                                        public void onErrorResponse(VolleyError error) {

                                                                        }
                                                                    }, params, 0);
                                                }
                                            })
                                    .setNegativeButton("No", null)
                                    .show();
                            break;
                    }
                    return true;
                }
            });
            itemUpvote = toolbarActions.getMenu().findItem(R.id.item_upvote);
            itemDownvote = toolbarActions.getMenu().findItem(R.id.item_downvote);

            clickListenerLink = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Comment comment = callback.getControllerComments().getComment(
                            getAdapterPosition());
                    if (comment.isMore()) {
                        callback.getControllerComments().loadMoreComments(comment);
                        return;
                    }

                    boolean isAuthor = comment.getAuthor().equals(callback.getUser().getName());

                    if (callback.getControllerComments().isCommentExpanded(getAdapterPosition())) {
                        toolbarActions.getMenu().findItem(R.id.item_collapse).setIcon(R.drawable.ic_arrow_drop_up_white_24dp);
                    }
                    else {
                        toolbarActions.getMenu().findItem(R.id.item_collapse).setIcon(R.drawable.ic_arrow_drop_down_white_24dp);
                    }

                    toolbarActions.getMenu().findItem(R.id.item_delete).setEnabled(isAuthor);
                    toolbarActions.getMenu().findItem(R.id.item_delete).setVisible(isAuthor);

                    setVoteColors();

                    AnimationUtils.animateExpandActions(layoutContainerActions, true);
                    AnimationUtils.animateExpandActions(toolbarActions, false);
                }
            };
            textComment.setOnClickListener(clickListenerLink);
            textInfo.setOnClickListener(clickListenerLink);
            this.itemView.setOnClickListener(clickListenerLink);

            toolbarActions.post(new Runnable() {
                @Override
                public void run() {
                    setToolbarMenuVisibility();
                }
            });
        }

        private void setToolbarMenuVisibility() {
            int maxNum = (int) (itemView.getWidth() / callback.getItemWidth());

            for (int index = 0; index < COMMENT_MENU_SIZE; index++) {
                if (index < maxNum) {
                    toolbarActions.getMenu().getItem(index).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                }
                else {
                    toolbarActions.getMenu().getItem(index).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                }
            }
        }

        public void setVoteColors() {

            Comment comment = callback.getControllerComments().getComment(
                    getAdapterPosition());
            switch (comment.isLikes()) {
                case 1:
                    drawableUpvote.setColorFilter(callback.getColorPositive(), PorterDuff.Mode.MULTIPLY);
                    itemUpvote.setIcon(drawableUpvote);
                    drawableDownvote.clearColorFilter();
                    itemDownvote.setIcon(drawableDownvote);
                    break;
                case -1:
                    drawableDownvote.setColorFilter(callback.getColorNegative(), PorterDuff.Mode.MULTIPLY);
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

        public void onBind(int position) {

            toolbarActions.setVisibility(View.GONE);

            Comment comment = callback.getControllerComments().getComment(position);

            if (comment.isReplyExpanded()) {
                editTextReply.setVisibility(View.VISIBLE);
                buttonSendReply.setVisibility(View.VISIBLE);
                layoutContainerActions.setVisibility(View.VISIBLE);
            }
            else {
                editTextReply.setVisibility(View.GONE);
                buttonSendReply.setVisibility(View.GONE);
                layoutContainerActions.setVisibility(View.GONE);
            }

            int alphaLevel = comment.getLevel() * MAX_ALPHA / ALPHA_LEVELS;

            int overlayColor = ColorUtils.setAlphaComponent(0xFF000000, alphaLevel <= MAX_ALPHA ? alphaLevel : MAX_ALPHA);
            int indicatorColor = ColorUtils.compositeColors(overlayColor,
                    callback.getColorPrimary());

            viewIndicator.setBackgroundColor(indicatorColor);
            viewIndicatorContainer.setBackgroundColor(indicatorColor);
            viewIndicatorContainerReply.setBackgroundColor(indicatorColor);

            ViewGroup.LayoutParams layoutParams = viewIndent.getLayoutParams();
            layoutParams.width = callback.getControllerComments().getIndentWidth(comment);
            viewIndent.setLayoutParams(layoutParams);

            if (comment.isMore()) {
                textComment.setText(R.string.load_more_comments);
                textInfo.setText("");
            }
            else {
                String html = comment.getBodyHtml();
                html = Html.fromHtml(html.trim())
                        .toString();

                CharSequence sequence = Html.fromHtml(html);

                // Trims leading and trailing whitespace
                int start = 0;
                int end = sequence.length();
                while (start < end && Character.isWhitespace(sequence.charAt(start))) {
                    start++;
                }
                while (end > start && Character.isWhitespace(sequence.charAt(end - 1))) {
                    end--;
                }
                sequence = sequence.subSequence(start, end);

                textComment.setText(sequence);
//                Linkify.addLinks(viewHolderComment.textComment, Linkify.ALL);

                Spannable spannableInfo = new SpannableString(
                        comment.getScore() + " by " + comment.getAuthor() + " on " + new Date(comment.getCreatedUtc()).toString());
                spannableInfo.setSpan(new ForegroundColorSpan(
                                comment.getScore() > 0 ? callback.getColorPositive() : callback.getColorNegative()), 0,
                        String.valueOf(comment.getScore())
                                .length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

                int colorAuthor = callback.getControllerComments().getMainLink().getAuthor().equals(comment.getAuthor()) ? callback.getColorAccent() : callback.getColorMuted();

                int indexScore = String.valueOf(comment.getScore()).length();
                spannableInfo.setSpan(new ForegroundColorSpan(callback.getColorMuted()), indexScore, indexScore + 4, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                spannableInfo.setSpan(new ForegroundColorSpan(colorAuthor), indexScore + 4, indexScore + 4 + comment.getAuthor().length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                spannableInfo.setSpan(new ForegroundColorSpan(callback.getColorMuted()), indexScore + 4 + comment.getAuthor().length(), spannableInfo.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

                textInfo.setText(spannableInfo);
            }
        }
    }
}
