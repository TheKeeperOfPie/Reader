/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.reddit;

import android.os.Parcel;
import android.text.TextUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.winsonchiu.reader.utils.UtilsJson;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class Message extends Replyable {

    private String author = "";
    private String body = "";
    private CharSequence bodyHtml = "";
    private String context = "";
    private String dest = "";
    private String firstMessageName = "";
    private int likes;
    private String linkTitle = "";
    private boolean isNew;
    private String parentId = "";
    private String replies = "";
    private String subject = "";
    private String subreddit = "";
    private boolean wasComment;
    private long created;
    private long createdUtc;

    public static Message fromJson(JsonNode nodeRoot) {

        Message message = new Message();

        message.setKind(UtilsJson.getString(nodeRoot.get("kind")));

        JsonNode nodeData = nodeRoot.get("data");

        String id = UtilsJson.getString(nodeData.get("id"));
        int indexStart = id.indexOf("_");
        if (indexStart >= 0) {
            message.setId(id.substring(indexStart + 1));
        }
        else {
            message.setId(id);
        }
        message.setName(UtilsJson.getString(nodeData.get("name")));

        String parentId = UtilsJson.getString(nodeData.get("parent_id"));
        indexStart = parentId.indexOf("_");
        if (indexStart >= 0) {
            message.setParentId(parentId.substring(indexStart + 1));
        }
        else {
            message.setParentId(parentId);
        }

        // Timestamps multiplied by 1000 as Java uses milliseconds and Reddit uses seconds
        message.setCreated(UtilsJson.getLong(nodeData.get("created")) * 1000);
        message.setCreatedUtc(UtilsJson.getLong(nodeData.get("created_utc")) * 1000);


        message.setAuthor(UtilsJson.getString(nodeData.get("author")));
        message.setBody(UtilsJson.getString(nodeData.get("body")));
        message.setBodyHtml(Reddit.getFormattedHtml(UtilsJson.getString(
                nodeData.get("body_html"))));
        message.setContext(UtilsJson.getString(nodeData.get("context")));
        message.setDest(UtilsJson.getString(nodeData.get("dest")));
        message.setFirstMessageName(UtilsJson.getString(nodeData.get("first_message_name")));

        switch (UtilsJson.getString(nodeData.get("likes"))) {
            case "null":
                message.setLikes(0);
                break;
            case "true":
                message.setLikes(1);
                break;
            case "false":
                message.setLikes(-1);
                break;
        }

        message.setLinkTitle(UtilsJson.getString(nodeData.get("link_title")));
        message.setIsNew(UtilsJson.getBoolean(nodeData.get("new")));
        message.setReplies(UtilsJson.getString(nodeData.get("replies")));
        message.setSubject(UtilsJson.getString(nodeData.get("subject")));
        message.setSubreddit(UtilsJson.getString(nodeData.get("subreddit")));
        message.setWasComment(UtilsJson.getBoolean(nodeData.get("was_comment")));

        return message;

    }
    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public CharSequence getBodyHtml() {
        return bodyHtml;
    }

    public void setBodyHtml(CharSequence bodyHtml) {
        this.bodyHtml = bodyHtml;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getFirstMessageName() {
        return firstMessageName;
    }

    public void setFirstMessageName(String firstMessageName) {
        this.firstMessageName = firstMessageName;
    }

    public int isLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public String getLinkTitle() {
        return linkTitle;
    }

    public void setLinkTitle(String linkTitle) {
        this.linkTitle = linkTitle;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setIsNew(boolean isNew) {
        this.isNew = isNew;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getReplies() {
        return replies;
    }

    public void setReplies(String replies) {
        this.replies = replies;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSubreddit() {
        return subreddit;
    }

    public void setSubreddit(String subreddit) {
        this.subreddit = subreddit;
    }

    public boolean getWasComment() {
        return wasComment;
    }

    public void setWasComment(boolean wasComment) {
        this.wasComment = wasComment;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public long getCreatedUtc() {
        return createdUtc;
    }

    public void setCreatedUtc(long createdUtc) {
        this.createdUtc = createdUtc;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Thing thing = (Thing) o;

        return getId().equals(thing.getId());

    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    public String getDest() {
        return dest;
    }

    public void setDest(String dest) {
        this.dest = dest;
    }

    @Override
    public CharSequence getParentHtml() {
        return getBodyHtml();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.author);
        dest.writeString(this.body);
        TextUtils.writeToParcel(this.bodyHtml, dest, flags);
        dest.writeString(this.context);
        dest.writeString(this.dest);
        dest.writeString(this.firstMessageName);
        dest.writeInt(this.likes);
        dest.writeString(this.linkTitle);
        dest.writeByte(isNew ? (byte) 1 : (byte) 0);
        dest.writeString(this.parentId);
        dest.writeString(this.replies);
        dest.writeString(this.subject);
        dest.writeString(this.subreddit);
        dest.writeByte(wasComment ? (byte) 1 : (byte) 0);
        dest.writeLong(this.created);
        dest.writeLong(this.createdUtc);
    }

    public Message() {
    }

    protected Message(Parcel in) {
        this.author = in.readString();
        this.body = in.readString();
        this.bodyHtml = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        this.context = in.readString();
        this.dest = in.readString();
        this.firstMessageName = in.readString();
        this.likes = in.readInt();
        this.linkTitle = in.readString();
        this.isNew = in.readByte() != 0;
        this.parentId = in.readString();
        this.replies = in.readString();
        this.subject = in.readString();
        this.subreddit = in.readString();
        this.wasComment = in.readByte() != 0;
        this.created = in.readLong();
        this.createdUtc = in.readLong();
    }

    public static final Creator<Message> CREATOR = new Creator<Message>() {
        public Message createFromParcel(Parcel source) {
            return new Message(source);
        }

        public Message[] newArray(int size) {
            return new Message[size];
        }
    };
}