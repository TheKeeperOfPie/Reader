/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.reddit;

import android.text.Html;
import android.text.TextUtils;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.winsonchiu.reader.utils.UtilsJson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class Comment extends Replyable {

    public static final String DELETED = "[deleted]";

    private static final String TAG = Comment.class.getCanonicalName();
    private String approvedBy = "";
    private String author = "";
    private String authorFlairCssClass = "";
    private String authorFlairText = "";
    private String bannedBy = "";
    private CharSequence body = "";
    private CharSequence bodyHtml = "";
    private Reddit.Distinguished distinguished = Reddit.Distinguished.NOT_DISTINGUISHED;
    private long edited;
    private int gilded;
    private int likes;
    private String linkId = "";
    private int numReports; // May be "null"
    private String parentId = "";
    private boolean saved;
    private int score;
    private boolean scoreHidden;
    private String subreddit = "";
    private String subredditId = "";
    private long created;
    private long createdUtc;

    // May not be present
    private String linkAuthor = "";
    private String linkTitle = "";
    private String linkUrl = "";
    private List<String> children = new ArrayList<>();
    private List<String> replies = new ArrayList<>();
    private boolean isNew;
    private String context;

    // For More entries
    private boolean isMore;
    private int count;

    private int level;
    private boolean editMode;
    private int collapsed;

    public static void addAllFromJson(List<Thing> comments, JsonNode jsonNode, int level) {

        comments.add(Comment.fromJson(jsonNode, level));

        if (jsonNode.get("data").hasNonNull("replies")) {
            JsonNode data = jsonNode.get("data")
                    .get("replies")
                    .get("data");

            if (data != null && data.hasNonNull("children")) {
                for (JsonNode node : data.get("children")) {
                    Comment.addAllFromJson(comments, node, level + 1);
                }
            }
        }

    }

    public static Comment fromJson(JsonNode rootNode, int level) {

        Comment comment = new Comment();
        comment.setLevel(level);
        comment.setKind(UtilsJson.getString(rootNode.get("kind")));

        JsonNode jsonNode = rootNode.get("data");

        String id = UtilsJson.getString(jsonNode.get("id"));
        int indexStart = id.indexOf("_");
        if (indexStart >= 0) {
            comment.setId(id.substring(indexStart + 1));
        }
        else {
            comment.setId(id);
        }
        comment.setName(UtilsJson.getString(jsonNode.get("name")));

        String parentId = UtilsJson.getString(jsonNode.get("parent_id"));
        indexStart = parentId.indexOf("_");
        if (indexStart >= 0) {
            comment.setParentId(parentId.substring(indexStart + 1));
        }
        else {
            comment.setParentId(parentId);
        }

        if (comment.getKind().equals("more")) {
            comment.setIsMore(true);
            comment.setCount(UtilsJson.getInt(jsonNode.get("count")));
            List<String> children = new LinkedList<>();
            for (JsonNode node : jsonNode.get("children")) {
                children.add(UtilsJson.getString(node));
            }
            comment.setChildren(children);
            return comment;
        }

        // Timestamps multiplied by 1000 as Java uses milliseconds and Reddit uses seconds
        comment.setCreated(UtilsJson.getLong(jsonNode.get("created")) * 1000);
        comment.setCreatedUtc(UtilsJson.getLong(jsonNode.get("created_utc")) * 1000);

        comment.setApprovedBy(UtilsJson.getString(jsonNode.get("approved_by")));
        comment.setAuthor(UtilsJson.getString(jsonNode.get("author")));
        comment.setAuthorFlairCssClass(UtilsJson.getString(jsonNode.get("author_flair_css_class")));
        comment.setAuthorFlairText(UtilsJson.getString(jsonNode.get("author_flair_text")));
        comment.setBannedBy(UtilsJson.getString(jsonNode.get("banned_by")));
        comment.setBody(Html.fromHtml(UtilsJson.getString(jsonNode.get("body")).replaceAll("\n",
                "<br>")));
        comment.setBodyHtml(Reddit.getFormattedHtml(UtilsJson.getString(
                jsonNode.get("body_html"))));

        switch (UtilsJson.getString(jsonNode.get("distinguished"))) {
            case "moderator":
                comment.setDistinguished(Reddit.Distinguished.MODERATOR);
                break;
            case "admin":
                comment.setDistinguished(Reddit.Distinguished.ADMIN);
                break;
            case "special":
                comment.setDistinguished(Reddit.Distinguished.SPECIAL);
                break;
            default:
                comment.setDistinguished(Reddit.Distinguished.NOT_DISTINGUISHED);
                break;
        }

        String edited = UtilsJson.getString(jsonNode.get("edited"));
        switch (edited) {
            case "true":
                comment.setEdited(1);
                break;
            case "false":
                comment.setEdited(0);
                break;
            default:
                comment.setEdited(UtilsJson.getLong(jsonNode.get("edited")) * 1000);
                break;
        }

        comment.setGilded(UtilsJson.getInt(jsonNode.get("gilded")));

        switch (UtilsJson.getString(jsonNode.get("likes"))) {
            case "null":
                comment.setLikes(0);
                break;
            case "true":
                comment.setLikes(1);
                break;
            case "false":
                comment.setLikes(-1);
                break;
        }

        comment.setLinkId(UtilsJson.getString(jsonNode.get("link_id")));
        comment.setNumReports(UtilsJson.getInt(jsonNode.get("num_reports")));
        comment.setSaved(UtilsJson.getBoolean(jsonNode.get("saved")));
        comment.setScore(UtilsJson.getInt(jsonNode.get("score")));
        comment.setScoreHidden(UtilsJson.getBoolean(jsonNode.get("score_hidden")));
        comment.setSubreddit(UtilsJson.getString(jsonNode.get("subreddit")));
        comment.setSubredditId(UtilsJson.getString(jsonNode.get("subreddit_id")));

        comment.setLinkAuthor(UtilsJson.getString(jsonNode.get("link_author")));
        comment.setLinkTitle(UtilsJson.getString(jsonNode.get("link_title")));
        comment.setLinkUrl(UtilsJson.getString(jsonNode.get("link_url")));

        comment.setIsNew(UtilsJson.getBoolean(jsonNode.get("new")));
        comment.setContext(UtilsJson.getString(jsonNode.get("context")));

        return comment;
    }

    public Comment() {
        super();
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthorFlairCssClass() {
        return authorFlairCssClass;
    }

    public void setAuthorFlairCssClass(String authorFlairCssClass) {
        this.authorFlairCssClass = authorFlairCssClass;
    }

    public String getAuthorFlairText() {
        return authorFlairText;
    }

    public void setAuthorFlairText(String authorFlairText) {
        this.authorFlairText = authorFlairText;
    }

    public String getBannedBy() {
        return bannedBy;
    }

    public void setBannedBy(String bannedBy) {
        this.bannedBy = bannedBy;
    }

    public CharSequence getBody() {
        return body;
    }

    public void setBody(CharSequence body) {
        this.body = body;
    }

    public CharSequence getBodyHtml() {
        return bodyHtml;
    }

    public void setBodyHtml(CharSequence bodyHtml) {
        this.bodyHtml = bodyHtml;
    }

    public Reddit.Distinguished getDistinguished() {
        return distinguished;
    }

    public void setDistinguished(Reddit.Distinguished distinguished) {
        this.distinguished = distinguished;
    }

    public long getEdited() {
        return edited;
    }

    public void setEdited(long edited) {
        this.edited = edited;
    }

    public int getGilded() {
        return gilded;
    }

    public void setGilded(int gilded) {
        this.gilded = gilded;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public String getLinkId() {
        return linkId;
    }

    public void setLinkId(String linkId) {
        this.linkId = linkId;
    }

    public int getNumReports() {
        return numReports;
    }

    public void setNumReports(int numReports) {
        this.numReports = numReports;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public boolean isSaved() {
        return saved;
    }

    public void setSaved(boolean saved) {
        this.saved = saved;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public boolean isScoreHidden() {
        return scoreHidden;
    }

    public void setScoreHidden(boolean scoreHidden) {
        this.scoreHidden = scoreHidden;
    }

    public String getSubreddit() {
        return subreddit;
    }

    public void setSubreddit(String subreddit) {
        this.subreddit = subreddit;
    }

    public String getSubredditId() {
        return subredditId;
    }

    public void setSubredditId(String subredditId) {
        this.subredditId = subredditId;
    }

    public String getLinkAuthor() {
        return linkAuthor;
    }

    public void setLinkAuthor(String linkAuthor) {
        this.linkAuthor = linkAuthor;
    }

    public String getLinkTitle() {
        return linkTitle;
    }

    public void setLinkTitle(String linkTitle) {
        this.linkTitle = linkTitle;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public boolean isMore() {
        return isMore;
    }

    public void setIsMore(boolean isMore) {
        this.isMore = isMore;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public List<String> getChildren() {
        return children;
    }

    public void setChildren(List<String> children) {
        this.children = children;
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

    public boolean getIsNew() {
        return isNew;
    }

    public void setIsNew(boolean isNew) {
        this.isNew = isNew;
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getCollapsed() {
        return collapsed;
    }

    public void setCollapsed(int collapsed) {
        this.collapsed = collapsed;
    }

    @Override
    public CharSequence getParentHtml() {
        return getBodyHtml();
    }

    public List<String> getReplies() {
        return replies;
    }

    public void setReplies(List<String> replies) {
        this.replies = replies;
    }
}
