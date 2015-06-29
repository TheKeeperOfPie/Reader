package com.winsonchiu.reader;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.winsonchiu.reader.data.Message;
import com.winsonchiu.reader.data.Reddit;
import com.winsonchiu.reader.data.Thing;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.winsonchiu.reader.ControllerInbox.VIEW_TYPE_COMMENT;
import static com.winsonchiu.reader.ControllerInbox.VIEW_TYPE_MESSAGE;

/**
 * Created by TheKeeperOfPie on 5/15/2015.
 */
public class AdapterInbox extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = AdapterInbox.class.getCanonicalName();

    private ControllerInbox controllerInbox;
    private ControllerUser controllerUser;
    private AdapterLink.ViewHolderBase.EventListener eventListenerBase;
    private AdapterCommentList.ViewHolderComment.EventListener eventListenerComment;
    private ViewHolderMessage.EventListener eventListenerInbox;
    private DisallowListener disallowListener;
    private ControllerProfile.Listener listener;
    private List<RecyclerView.ViewHolder> viewHolders;

    public AdapterInbox(ControllerInbox controllerInbox,
            ControllerUser controllerUser,
            AdapterLink.ViewHolderBase.EventListener eventListenerBase,
            AdapterCommentList.ViewHolderComment.EventListener eventListenerComment,
            ViewHolderMessage.EventListener eventListenerInbox,
            DisallowListener disallowListener,
            ControllerProfile.Listener listener) {
        this.controllerInbox = controllerInbox;
        this.controllerUser = controllerUser;
        this.eventListenerBase = eventListenerBase;
        this.eventListenerComment = eventListenerComment;
        this.eventListenerInbox = eventListenerInbox;
        this.disallowListener = disallowListener;
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

            case VIEW_TYPE_MESSAGE:
                return new ViewHolderMessage(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.row_message, parent, false), eventListenerInbox);
            case VIEW_TYPE_COMMENT:
                // TODO: Move to different ViewHolderComment constructor
                return new AdapterCommentList.ViewHolderComment(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.row_comment, parent, false), eventListenerBase, eventListenerComment, disallowListener, listener) {
                    @Override
                    public void expandToolbarActions() {
                        super.expandToolbarActions();

                        if (comment.isNew()) {
                            eventListenerInbox.markRead(comment);
                            comment.setIsNew(false);

                            textInfo.setTextColor(comment.isNew() ? itemView.getContext().getResources()
                                    .getColor(R.color.darkThemeTextColorAlert) : itemView.getContext().getResources()
                                    .getColor(R.color.darkThemeTextColorMuted));
                        }
                    }
                };

        }

        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        if (!controllerInbox.isLoading() && position > controllerInbox.getItemCount() - 5) {
            controllerInbox.loadMore();
        }

        switch (getItemViewType(position)) {
            case VIEW_TYPE_MESSAGE:
                ViewHolderMessage viewHolderMessage = (ViewHolderMessage) holder;
                viewHolderMessage.onBind(controllerInbox.getMessage(position));
                break;
            case VIEW_TYPE_COMMENT:
                AdapterCommentList.ViewHolderComment viewHolderComment = (AdapterCommentList.ViewHolderComment) holder;
                viewHolderComment.onBind(controllerInbox.getComment(position), controllerUser.getUser().getName());
                break;
        }

    }

    @Override
    public int getItemCount() {
        return controllerInbox.getItemCount();
    }

    @Override
    public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        viewHolders.add(holder);
        holder.itemView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        viewHolders.remove(holder);
    }

    public void setVisibility(int visibility) {
        for (RecyclerView.ViewHolder viewHolder : viewHolders) {
            viewHolder.itemView.setVisibility(visibility);
        }
    }

    protected static class ViewHolderMessage extends RecyclerView.ViewHolder {

        protected Message message;

        protected TextView textMessage;
        protected TextView textInfo;
        protected RelativeLayout layoutContainerReply;
        protected EditText editTextReply;
        protected Button buttonSendReply;
        protected Toolbar toolbarActions;
        private View.OnClickListener clickListenerLink;
        protected EventListener eventListener;

        protected int toolbarItemWidth;

        public ViewHolderMessage(View itemView, final EventListener listener) {
            super(itemView);
            this.eventListener = listener;

            toolbarItemWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                    itemView.getContext().getResources().getDisplayMetrics());

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
                        eventListener.sendMessage(message.getName(), editTextReply.getText().toString());
                        message.setReplyExpanded(!message.isReplyExpanded());
                        layoutContainerReply.setVisibility(View.GONE);
                    }
                }
            });
            toolbarActions = (Toolbar) itemView.findViewById(R.id.toolbar_actions);
            toolbarActions.inflateMenu(R.menu.menu_message);
            toolbarActions.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    switch (menuItem.getItemId()) {
                        case R.id.item_reply:
                            if (!message.isReplyExpanded()) {
                                editTextReply.requestFocus();
                                editTextReply.setText(null);
                            }
                            message.setReplyExpanded(!message.isReplyExpanded());
                            layoutContainerReply.setVisibility(
                                    message.isReplyExpanded() ? View.VISIBLE : View.GONE);
                            break;
                    }
                    return true;
                }
            });
            clickListenerLink = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (message.isNew()) {
                        eventListener.markRead(message);
                        message.setIsNew(false);
                        onBind(message);
                    }

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
            textInfo.setTextColor(message.isNew() ? itemView.getContext().getResources()
                    .getColor(R.color.darkThemeTextColorAlert) : itemView.getContext().getResources()
                    .getColor(R.color.darkThemeTextColorMuted));
        }

        public interface EventListener {
            void sendMessage(String name, String text);
            void markRead(Thing thing);
        }
    }

}
