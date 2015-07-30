/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.inbox;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.MainActivity;
import com.winsonchiu.reader.utils.AnimationUtils;
import com.winsonchiu.reader.ControllerUser;
import com.winsonchiu.reader.utils.DisallowListener;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.comments.AdapterCommentList;
import com.winsonchiu.reader.data.reddit.Message;
import com.winsonchiu.reader.data.reddit.Thing;
import com.winsonchiu.reader.links.AdapterLink;
import com.winsonchiu.reader.profile.ControllerProfile;
import com.winsonchiu.reader.utils.RecyclerCallback;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 5/15/2015.
 */
public class AdapterInbox extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = AdapterInbox.class.getCanonicalName();

    private ControllerInbox controllerInbox;
    private ControllerUser controllerUser;
    private AdapterLink.ViewHolderBase.EventListener eventListenerBase;
    private AdapterCommentList.ViewHolderComment.EventListener eventListenerComment;
    private DisallowListener disallowListener;
    private RecyclerCallback recyclerCallback;
    private ControllerProfile.Listener listener;
    private List<RecyclerView.ViewHolder> viewHolders;

    public AdapterInbox(ControllerInbox controllerInbox,
            ControllerUser controllerUser,
            AdapterLink.ViewHolderBase.EventListener eventListenerBase,
            AdapterCommentList.ViewHolderComment.EventListener eventListenerComment,
            DisallowListener disallowListener,
            RecyclerCallback recyclerCallback,
            ControllerProfile.Listener listener) {
        this.controllerInbox = controllerInbox;
        this.controllerUser = controllerUser;
        this.eventListenerBase = eventListenerBase;
        this.eventListenerComment = eventListenerComment;
        this.disallowListener = disallowListener;
        this.recyclerCallback = recyclerCallback;
        this.listener = listener;
        this.viewHolders = new ArrayList<>();
    }

    @Override
    public int getItemViewType(int position) {
        return controllerInbox.getViewType(position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        switch (viewType) {

            case ControllerInbox.VIEW_TYPE_MESSAGE:
                return new ViewHolderMessage(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.row_message, parent, false), eventListenerBase, recyclerCallback);
            case ControllerInbox.VIEW_TYPE_COMMENT:
                // TODO: Move to different ViewHolderComment constructor
                return new AdapterCommentList.ViewHolderComment(LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.row_comment, parent, false), eventListenerBase, eventListenerComment, disallowListener, recyclerCallback, listener);

        }

        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        if (!controllerInbox.isLoading() && position > controllerInbox.getItemCount() - 5) {
            controllerInbox.loadMore();
        }

        switch (getItemViewType(position)) {
            case ControllerInbox.VIEW_TYPE_MESSAGE:
                ViewHolderMessage viewHolderMessage = (ViewHolderMessage) holder;
                viewHolderMessage.onBind(controllerInbox.getMessage(position), controllerUser.getUser().getName());
                break;
            case ControllerInbox.VIEW_TYPE_COMMENT:
                AdapterCommentList.ViewHolderComment viewHolderComment = (AdapterCommentList.ViewHolderComment) holder;
                viewHolderComment.onBind(controllerInbox.getComment(position), controllerUser.getUser().getName());
                break;
        }
        viewHolders.add(holder);

    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        viewHolders.remove(holder);
    }

    @Override
    public int getItemCount() {
        return controllerInbox.getItemCount();
    }

    public void setVisibility(int visibility) {
        for (RecyclerView.ViewHolder viewHolder : viewHolders) {
            viewHolder.itemView.setVisibility(visibility);
        }
    }

    protected static class ViewHolderMessage extends RecyclerView.ViewHolder
            implements Toolbar.OnMenuItemClickListener {

        protected Message message;

        protected TextView textSubject;
        protected TextView textMessage;
        protected TextView textInfo;
        protected RelativeLayout layoutContainerReply;
        protected EditText editTextReply;
        protected Button buttonSendReply;
        protected ImageButton buttonReplyEditor;
        protected Toolbar toolbarActions;
        protected AdapterLink.ViewHolderBase.EventListener eventListener;
        protected RecyclerCallback recyclerCallback;
        private View.OnClickListener clickListenerLink;

        protected SharedPreferences preferences;
        protected int toolbarItemWidth;
        protected String userName;
        private Resources resources;

        public ViewHolderMessage(View itemView, final AdapterLink.ViewHolderBase.EventListener listener, RecyclerCallback recyclerCallback) {
            super(itemView);
            this.eventListener = listener;
            this.recyclerCallback = recyclerCallback;

            resources = itemView.getResources();
            toolbarItemWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                    itemView.getResources().getDisplayMetrics());

            textSubject = (TextView) itemView.findViewById(R.id.text_subject);
            textMessage = (TextView) itemView.findViewById(R.id.text_message);
            textMessage.setMovementMethod(LinkMovementMethod.getInstance());
            textInfo = (TextView) itemView.findViewById(R.id.text_info);
            layoutContainerReply = (RelativeLayout) itemView.findViewById(
                    R.id.layout_container_reply);
            editTextReply = (EditText) itemView.findViewById(R.id.edit_text_reply);
            editTextReply.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    message.setReplyText(s.toString());
                }
            });
            buttonSendReply = (Button) itemView.findViewById(R.id.button_send_reply);
            buttonSendReply.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (!TextUtils.isEmpty(editTextReply.getText())) {
                        eventListener.sendMessage(message.getName(), editTextReply.getText().toString());
                        message.setReplyExpanded(!message.isReplyExpanded());
                        layoutContainerReply.setVisibility(View.GONE);
                    }
                }
            });
            buttonReplyEditor = (ImageButton) itemView.findViewById(R.id.button_reply_editor);
            buttonReplyEditor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    eventListener.showReplyEditor(message);
                }
            });
            toolbarActions = (Toolbar) itemView.findViewById(R.id.toolbar_actions);
            toolbarActions.inflateMenu(R.menu.menu_message);
            toolbarActions.setOnMenuItemClickListener(this);

            Menu menu = toolbarActions.getMenu();
            TypedArray typedArray = itemView.getContext().getTheme().obtainStyledAttributes(new int[] {R.attr.colorIconFilter});
            int colorIconFilter = typedArray.getColor(0, 0xFFFFFFFF);
            typedArray.recycle();

            PorterDuffColorFilter colorFilter = new PorterDuffColorFilter(colorIconFilter,
                    PorterDuff.Mode.MULTIPLY);

            for (int index = 0; index < menu.size(); index++) {
                menu.getItem(index).getIcon().setColorFilter(colorFilter);
            }
            buttonReplyEditor.setColorFilter(colorFilter);
            preferences = PreferenceManager.getDefaultSharedPreferences(itemView.getContext());

            clickListenerLink = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (message.isNew()) {
                        eventListener.markRead(message);
                        message.setIsNew(false);
                        onBind(message, userName);
                    }

                    AnimationUtils.animateExpand(toolbarActions, 1f, null);
                }
            };
            textSubject.setOnClickListener(clickListenerLink);
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
            int maxNum = itemView.getWidth() / toolbarItemWidth;

            Menu menu = toolbarActions.getMenu();

            for (int index = 0; index < menu.size(); index++) {
                if (index < maxNum) {
                    menu.getItem(index)
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                }
                else {
                    menu.getItem(index)
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                }
            }
        }

        public void onBind(Message message, String userName) {

            this.message = message;
            this.userName = userName;

            toolbarActions.setVisibility(View.GONE);

            layoutContainerReply.setVisibility(message.isReplyExpanded() ? View.VISIBLE : View.GONE);

            if (message.isReplyExpanded()) {
                editTextReply.setText(message.getReplyText());
            }
            textSubject.setText(Html.fromHtml(message.getSubject()));
            textMessage.setText(message.getBodyHtml());

            String prefix;

            if (message.getAuthor().equals(userName)) {
                prefix = "to " + message.getDest().replaceAll("#", "/r/");
            }
            else {
                prefix = "by " + message.getAuthor();
            }


            CharSequence timestamp =
                    preferences.getBoolean(AppSettings.PREF_FULL_TIMESTAMPS, false) ? DateUtils
                            .formatDateTime(itemView.getContext(), message.getCreatedUtc(),
                                    DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR) :
                            DateUtils.getRelativeTimeSpanString(message.getCreatedUtc());

            Spannable spannableInfo = new SpannableString(
                    prefix + " " + timestamp);

            textInfo.setText(spannableInfo);

            TypedArray typedArray = itemView.getContext().getTheme().obtainStyledAttributes(new int[] {android.R.attr.textColorSecondary});
            textInfo.setTextColor(message.isNew() ? itemView.getResources()
                    .getColor(R.color.textColorAlert) : typedArray.getColor(0, itemView.getResources().getColor(R.color.darkThemeTextColorMuted)));
            typedArray.recycle();
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {

            switch (item.getItemId()) {
                case R.id.item_reply:
                    message.setReplyText(message.getReplyText());
                    toggleReply();
                    break;
                case R.id.item_view_profile:
                    Intent intent = new Intent(itemView.getContext(), MainActivity.class);
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.putExtra(MainActivity.REDDIT_PAGE,
                            "https://reddit.com/user/" + message.getAuthor());
                    eventListener.startActivity(intent);
                    break;
                case R.id.item_copy_text:
                    ClipboardManager clipboard = (ClipboardManager) itemView.getContext()
                            .getSystemService(
                                    Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText(
                            resources.getString(R.string.comment),
                            message.getBody());
                    clipboard.setPrimaryClip(clip);
                    eventListener.toast(resources.getString(
                            R.string.copied));
                    break;
            }
            return true;
        }

        private void toggleReply() {

            message.setReplyExpanded(!message.isReplyExpanded());
            layoutContainerReply.setVisibility(
                    message.isReplyExpanded() ? View.VISIBLE : View.GONE);
            if (message.isReplyExpanded()) {
                recyclerCallback.onReplyShown();
                editTextReply.setText(message.getReplyText());
                editTextReply.clearFocus();
                InputMethodManager inputManager = (InputMethodManager) itemView.getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                editTextReply.requestFocus();
            }
        }
    }

}
