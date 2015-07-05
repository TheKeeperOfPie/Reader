/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class Message extends Thing {

    private String author;
    private String body;
    private CharSequence bodyHtml;
    private String context;
    private String dest;
    private String firstMessageName;
    private int likes;
    private String linkTitle;
    private boolean isNew;
    private String parentId;
    private String replies;
    private String subject;
    private String subreddit;
    private boolean wasComment;
    private long created;
    private long createdUtc;

    private boolean replyExpanded;

    public static Message fromJson(JSONObject rootJsonObject) throws JSONException {

        Message message = new Message();

        message.setKind(rootJsonObject.optString("kind"));

        JSONObject jsonObject = rootJsonObject.getJSONObject("data");

        String id = jsonObject.optString("id");
        int indexStart = id.indexOf("_");
        if (indexStart >= 0) {
            message.setId(id.substring(indexStart + 1));
        }
        else {
            message.setId(id);
        }
        message.setName(jsonObject.optString("name"));

        String parentId = jsonObject.optString("parent_id");
        indexStart = parentId.indexOf("_");
        if (indexStart >= 0) {
            message.setParentId(parentId.substring(indexStart + 1));
        }
        else {
            message.setParentId(parentId);
        }

        // Timestamps multiplied by 1000 as Java uses milliseconds and Reddit uses seconds
        message.setCreated(jsonObject.optLong("created") * 1000);
        message.setCreatedUtc(jsonObject.optLong("created_utc") * 1000);


        message.setAuthor(jsonObject.optString("author"));
        message.setBody(jsonObject.optString("body"));
        message.setBodyHtml(Reddit.getFormattedHtml(jsonObject.optString("body_html")));
        message.setContext(jsonObject.optString("context"));
        message.setDest(jsonObject.optString("dest"));
        message.setFirstMessageName(jsonObject.optString("first_message_name"));

        switch (jsonObject.optString("likes")) {
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

        message.setLinkTitle(jsonObject.optString("link_title"));
        message.setIsNew(jsonObject.optBoolean("new"));
        message.setReplies(jsonObject.optString("replies"));
        message.setSubject(jsonObject.optString("subject"));
        message.setSubreddit(jsonObject.optString("subreddit"));
        message.setWasComment(jsonObject.optBoolean("was_comment"));

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

    public boolean isReplyExpanded() {
        return replyExpanded;
    }

    public void setReplyExpanded(boolean replyExpanded) {
        this.replyExpanded = replyExpanded;
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
}