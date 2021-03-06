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
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.winsonchiu.reader.ActivityMain;
import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.ControllerUser;
import com.winsonchiu.reader.R;
import com.winsonchiu.reader.adapter.AdapterBase;
import com.winsonchiu.reader.adapter.AdapterCallback;
import com.winsonchiu.reader.adapter.AdapterListener;
import com.winsonchiu.reader.comments.AdapterCommentList;
import com.winsonchiu.reader.data.reddit.Message;
import com.winsonchiu.reader.links.AdapterLink;
import com.winsonchiu.reader.theme.Themer;
import com.winsonchiu.reader.utils.UtilsAnimation;
import com.winsonchiu.reader.utils.UtilsInput;
import com.winsonchiu.reader.utils.UtilsTheme;
import com.winsonchiu.reader.utils.ViewHolderBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 5/15/2015.
 */
public class AdapterInbox extends AdapterBase<RecyclerView.ViewHolder> {

    private static final String TAG = AdapterInbox.class.getCanonicalName();

    private ControllerInbox controllerInbox;
    private ControllerUser controllerUser;
    private AdapterListener adapterListener;
    private AdapterCommentList.ViewHolderComment.Listener listenerComments;
    private AdapterLink.ViewHolderLink.EventListener eventListenerBase;

    private List<RecyclerView.ViewHolder> viewHolders = new ArrayList<>();

    public AdapterInbox(ControllerInbox controllerInbox,
            ControllerUser controllerUser,
            AdapterListener adapterListener,
            AdapterCommentList.ViewHolderComment.Listener listenerComments,
            AdapterLink.ViewHolderLink.EventListener eventListenerBase) {
        this.controllerInbox = controllerInbox;
        this.controllerUser = controllerUser;
        this.adapterListener = adapterListener;
        this.listenerComments = listenerComments;
        this.eventListenerBase = eventListenerBase;
        setAdapterLoadMoreListener(adapterListener);
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
                                .inflate(R.layout.row_message, parent, false),
                        adapterCallback,
                        adapterListener,
                        eventListenerBase);
            case ControllerInbox.VIEW_TYPE_COMMENT:
                // TODO: Move to different ViewHolderComment constructor
                return new AdapterCommentList.ViewHolderComment(LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.row_comment, parent, false),
                        adapterCallback,
                        adapterListener,
                        listenerComments);

        }

        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        switch (holder.getItemViewType()) {
            case ControllerInbox.VIEW_TYPE_MESSAGE:
                ViewHolderMessage viewHolderMessage = (ViewHolderMessage) holder;
                viewHolderMessage.onBind(controllerInbox.getMessage(position));
                break;
            case ControllerInbox.VIEW_TYPE_COMMENT:
                AdapterCommentList.ViewHolderComment viewHolderComment = (AdapterCommentList.ViewHolderComment) holder;
                viewHolderComment.onBind(controllerInbox.getComment(position), controllerUser.getUser());
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

    protected static class ViewHolderMessage extends ViewHolderBase
            implements Toolbar.OnMenuItemClickListener {

        protected AdapterListener adapterListener;

        protected Message message;

        protected TextView textSubject;
        protected TextView textMessage;
        protected TextView textInfo;
        protected ViewGroup layoutContainerReply;
        protected EditText editTextReply;
        protected TextView textUsername;
        protected Button buttonSendReply;
        protected ImageButton buttonReplyEditor;
        protected Toolbar toolbarActions;
        protected AdapterLink.ViewHolderLink.EventListener eventListener;
        private View.OnClickListener clickListenerLink;

        protected SharedPreferences preferences;
        protected int toolbarItemWidth;
        private Resources resources;

        public ViewHolderMessage(View itemView, AdapterCallback adapterCallback, AdapterListener adapterListener, final AdapterLink.ViewHolderLink.EventListener listener) {
            super(itemView, adapterCallback);
            this.adapterListener = adapterListener;
            this.eventListener = listener;

            resources = itemView.getResources();
            toolbarItemWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                    itemView.getResources().getDisplayMetrics());

            textSubject = (TextView) itemView.findViewById(R.id.text_subject);
            textMessage = (TextView) itemView.findViewById(R.id.text_message);
            textMessage.setMovementMethod(LinkMovementMethod.getInstance());
            textInfo = (TextView) itemView.findViewById(R.id.text_info);
            layoutContainerReply = (ViewGroup) itemView.findViewById(
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
            textUsername = (TextView) itemView.findViewById(R.id.text_username);

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

            Themer themer = new Themer(itemView.getContext());

            for (int index = 0; index < menu.size(); index++) {
                menu.getItem(index).getIcon().mutate().setColorFilter(themer.getColorFilterIcon());
            }
            buttonReplyEditor.setColorFilter(themer.getColorFilterIcon());
            preferences = PreferenceManager.getDefaultSharedPreferences(itemView.getContext());

            clickListenerLink = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (message.isNew()) {
                        eventListener.markRead(message);
                        message.setIsNew(false);
                        onBind(message);
                    }

                    UtilsAnimation.animateExpand(toolbarActions, 1f, null);
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

        public void onBind(Message message) {

            this.message = message;

            toolbarActions.setVisibility(View.GONE);

            layoutContainerReply.setVisibility(message.isReplyExpanded() ? View.VISIBLE : View.GONE);

            if (message.isReplyExpanded()) {
                editTextReply.setText(message.getReplyText());
            }
            textSubject.setText(Html.fromHtml(message.getSubject()));
            textMessage.setText(message.getBodyHtml());

            String prefix;

            if (message.getAuthor().equals(eventListener.getUser().getName())) {
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

            int colorTextSecondary = UtilsTheme.getAttributeColor(itemView.getContext(), android.R.attr.textColorSecondary, ContextCompat.getColor(itemView.getContext(), R.color.darkThemeTextColorMuted));

            @ColorInt int colorTextInfo = message.isNew() ? ContextCompat.getColor(itemView.getContext(), R.color.textColorAlert) : colorTextSecondary;
            textInfo.setTextColor(colorTextInfo);
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {

            switch (item.getItemId()) {
                case R.id.item_reply:
                    message.setReplyText(message.getReplyText());
                    toggleReply();
                    break;
                case R.id.item_view_profile:
                    Intent intent = new Intent(itemView.getContext(), ActivityMain.class);
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.putExtra(ActivityMain.REDDIT_PAGE,
                            "https://reddit.com/user/" + message.getAuthor());
                    eventListener.launchScreen(intent);
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
                textUsername.setText("- " + eventListener.getUser().getName());
                adapterListener.clearDecoration();
                editTextReply.setText(message.getReplyText());
                editTextReply.clearFocus();
                editTextReply.requestFocus();
                UtilsInput.showKeyboard(editTextReply);
            }
            else {
                UtilsInput.hideKeyboard(editTextReply);
            }
        }
    }

}
