package com.winsonchiu.reader;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.winsonchiu.reader.data.Message;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.winsonchiu.reader.ControllerInbox.ItemClickListener;
import static com.winsonchiu.reader.ControllerInbox.ListenerCallback;
import static com.winsonchiu.reader.ControllerInbox.VIEW_TYPE_COMMENT;
import static com.winsonchiu.reader.ControllerInbox.VIEW_TYPE_MESSAGE;

/**
 * Created by TheKeeperOfPie on 5/15/2015.
 */
public class AdapterInbox extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements ListenerCallback {

    private static final String TAG = AdapterInbox.class.getCanonicalName();
    private static final int MESSAGE_MENU_SIZE = 1;

    protected Activity activity;
    private ControllerInbox controllerInbox;
    private SharedPreferences preferences;
    private float itemWidth;
    private ItemClickListener listener;
    private ControllerComments.ListenerCallback commentsCallback;

    public AdapterInbox(final Activity activity,
            final ControllerInbox controllerInbox,
            ItemClickListener listener) {
        this.activity = activity;
        this.preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        Resources resources = activity.getResources();
        this.itemWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                resources.getDisplayMetrics());
        this.controllerInbox = controllerInbox;
        this.listener = listener;
        setCallbacks();
    }

    private void setCallbacks() {
        this.commentsCallback = new ControllerComments.ListenerCallback() {
            @Override
            public ControllerComments.CommentClickListener getCommentClickListener() {
                return new ControllerComments.CommentClickListener() {
                    @Override
                    public void loadUrl(String url) {
                        listener.loadUrl(url);
                    }

                    @Override
                    public void setRefreshing(boolean refreshing) {

                    }

                    @Override
                    public AdapterCommentList getAdapter() {
                        return null;
                    }

                    @Override
                    public int getRecyclerHeight() {
                        return listener.getRecyclerHeight();
                    }

                    @Override
                    public int getRecyclerWidth() {
                        return listener.getRecyclerWidth();
                    }

                    @Override
                    public void setToolbarTitle(String title) {
                        // Not implemented
                    }

                    @Override
                    public void requestDisallowInterceptTouchEvent(boolean disallow) {

                    }
                };
            }

            @Override
            public ControllerCommentsBase getControllerComments() {
                return controllerInbox;
            }

            @Override
            public SharedPreferences getPreferences() {
                return preferences;
            }

            @Override
            public User getUser() {
                return controllerInbox.getUser();
            }

            @Override
            public float getItemWidth() {
                return itemWidth;
            }
        };
    }

    @Override
    public int getItemViewType(int position) {
        return controllerInbox.getViewType(position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        switch (viewType) {

            case VIEW_TYPE_MESSAGE:
                return new ViewHolderMessage(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.row_message, parent, false), this);
            case VIEW_TYPE_COMMENT:
                return new AdapterCommentList.ViewHolderComment(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.row_comment, parent, false), commentsCallback);

        }

        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        switch (getItemViewType(position)) {
            case VIEW_TYPE_MESSAGE:
                ViewHolderMessage viewHolderMessage = (ViewHolderMessage) holder;
                viewHolderMessage.onBind(position);
                break;
            case VIEW_TYPE_COMMENT:
                AdapterCommentList.ViewHolderComment viewHolderComment = (AdapterCommentList.ViewHolderComment) holder;
                viewHolderComment.onBind(position);
                break;
        }

    }

    @Override
    public int getItemCount() {
        return controllerInbox.getItemCount();
    }

    @Override
    public ItemClickListener getListener() {
        return listener;
    }

    @Override
    public ControllerInbox getController() {
        return controllerInbox;
    }

    @Override
    public float getItemWidth() {
        return itemWidth;
    }

    @Override
    public SharedPreferences getPreferences() {
        return preferences;
    }

    private static class ViewHolderMessage extends RecyclerView.ViewHolder {

        protected TextView textMessage;
        protected TextView textInfo;
        protected RelativeLayout layoutContainerReply;
        protected EditText editTextReply;
        protected Button buttonSendReply;
        protected Toolbar toolbarActions;
        private View.OnClickListener clickListenerLink;
        private ListenerCallback callback;

        public ViewHolderMessage(View itemView, ListenerCallback listenerCallback) {
            super(itemView);
            this.callback = listenerCallback;

            Resources resources = callback.getController()
                    .getActivity()
                    .getResources();
            textMessage = (TextView) itemView.findViewById(R.id.text_message);
            textMessage.setMovementMethod(LinkMovementMethod.getInstance());
            textInfo = (TextView) itemView.findViewById(R.id.text_info);
            layoutContainerReply = (RelativeLayout) itemView.findViewById(
                    R.id.layout_container_reply);
            editTextReply = (EditText) itemView.findViewById(R.id.edit_text_reply);
            buttonSendReply = (Button) itemView.findViewById(R.id.button_send_reply);
            buttonSendReply.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (!TextUtils.isEmpty(editTextReply.getText())) {
                        final int messageIndex = getAdapterPosition();
                        Message message = callback.getController()
                                .getMessage(messageIndex);
                        Map<String, String> params = new HashMap<>();
                        params.put("api_type", "json");
                        params.put("text", editTextReply.getText()
                                .toString());
                        params.put("thing_id", message.getName());

                        // TODO: Move add to immediate on button click, check if failed afterwards
                        callback.getController()
                                .getReddit()
                                .loadPost(Reddit.OAUTH_URL + "/api/comment",
                                        new Response.Listener<String>() {
                                            @Override
                                            public void onResponse(String response) {
                                                try {
                                                    JSONObject jsonObject = new JSONObject(
                                                            response);
                                                    Message newMessage = Message.fromJson(
                                                            jsonObject.getJSONObject("json")
                                                                    .getJSONObject("data")
                                                                    .getJSONArray("things")
                                                                    .getJSONObject(0));
                                                    callback.getController()
                                                            .insertMessage(newMessage);
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
                        message.setReplyExpanded(!message.isReplyExpanded());

                        layoutContainerReply.setVisibility(View.GONE);

//                        AnimationUtils.animateExpand(editTextReply, 1f,
//                                new AnimationUtils.OnAnimationEndListener() {
//                                    @Override
//                                    public void onAnimationEnd() {
//                                        AnimationUtils.animateExpand(buttonSendReply, 1f, null);
//                                    }
//                                });
                    }
                }
            });
            toolbarActions = (Toolbar) itemView.findViewById(R.id.toolbar_actions);
            toolbarActions.inflateMenu(R.menu.menu_message);
            toolbarActions.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    final int messageIndex = getAdapterPosition();
                    switch (menuItem.getItemId()) {
                        case R.id.item_reply:
                            if (TextUtils.isEmpty(callback.getPreferences()
                                    .getString(AppSettings.REFRESH_TOKEN, ""))) {
                                Toast.makeText(callback.getController()
                                        .getActivity(), callback.getController()
                                        .getActivity()
                                        .getString(R.string.must_be_logged_in_to_reply),
                                        Toast.LENGTH_SHORT)
                                        .show();
                                return false;
                            }
                            Message message = callback.getController()
                                    .getMessage(messageIndex);
                            if (!message.isReplyExpanded()) {
                                editTextReply.requestFocus();
                                editTextReply.setText(null);
                            }
                            message.setReplyExpanded(!message.isReplyExpanded());
                            layoutContainerReply.setVisibility(
                                    message.isReplyExpanded() ? View.VISIBLE : View.GONE);

//                            AnimationUtils.animateExpand(editTextReply, 1f,
//                                    new AnimationUtils.OnAnimationEndListener() {
//                                        @Override
//                                        public void onAnimationEnd() {
//                                            AnimationUtils.animateExpand(buttonSendReply, 1f, null);
//                                        }
//                                    });
                            break;
                    }
                    return true;
                }
            });
            clickListenerLink = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AnimationUtils.animateExpand(toolbarActions, 1f, null);
                }
            };
            textMessage.setOnClickListener(clickListenerLink);
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

            for (int index = 0; index < MESSAGE_MENU_SIZE; index++) {
                if (index < maxNum) {
                    toolbarActions.getMenu()
                            .getItem(index)
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                }
                else {
                    toolbarActions.getMenu()
                            .getItem(index)
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                }
            }
        }

        public void onBind(int position) {

            toolbarActions.setVisibility(View.GONE);

            Message message = callback.getController()
                    .getMessage(position);

            if (message.isReplyExpanded()) {
                layoutContainerReply.setVisibility(View.VISIBLE);
            }
            else {
                layoutContainerReply.setVisibility(View.GONE);
            }

            textMessage.setText(Reddit.getTrimmedHtml(message.getBodyHtml()));

            Spannable spannableInfo = new SpannableString(
                    "by " + message.getAuthor() + " on " + new Date(
                            message.getCreatedUtc()).toString());

            textInfo.setText(spannableInfo);
            textInfo.setTextColor(callback.getController()
                    .getActivity()
                    .getResources()
                    .getColor(R.color.darkThemeTextColorMuted));
        }
    }

}
