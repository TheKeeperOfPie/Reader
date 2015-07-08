/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.reddit;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class Comment extends Thing {

    public static final String DELETED = "[deleted]";

    private static final String TAG = Comment.class.getCanonicalName();

    private Comment parent;

    private String approvedBy = "";
    private String author = "";
    private String authorFlairCssClass = "";
    private String authorFlairText = "";
    private String bannedBy = "";
    private String body = "";
    private CharSequence bodyHtml = "";
    private Reddit.Distinguished distinguished;
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
    private List<String> children;
    private boolean isNew;
    private String context;

    // For More entries
    private boolean isMore;
    private int count;

    private int level;
    private List<Comment> replies;
    private String replyText;
    private boolean replyExpanded;
    private boolean editMode;

    public static void addAllFromJson(List<Comment> comments, JSONObject rootJsonObject, int level) throws JSONException {

        comments.add(Comment.fromJson(rootJsonObject, level));

        if (rootJsonObject.getJSONObject("data").has("replies") && !TextUtils.isEmpty(rootJsonObject.getJSONObject("data").optString("replies")) && !rootJsonObject.getJSONObject("data").optString("replies").equals("null")) {
            JSONObject data = rootJsonObject.getJSONObject("data")
                    .getJSONObject("replies")
                    .getJSONObject("data");

            if (data.has("children")) {
                JSONArray arrayComments = data.getJSONArray("children");
                for (int index = 0; index < arrayComments.length(); index++) {
                    Comment.addAllFromJson(comments, arrayComments.getJSONObject(index), level + 1);
                }
            }
        }

    }

    public static Comment fromJson(JSONObject rootJsonObject, int level) throws JSONException {

        Comment comment = new Comment();
        comment.setLevel(level);
        comment.setKind(rootJsonObject.optString("kind"));

        JSONObject jsonObject = rootJsonObject.getJSONObject("data");

        String id = jsonObject.optString("id");
        int indexStart = id.indexOf("_");
        if (indexStart >= 0) {
            comment.setId(id.substring(indexStart + 1));
        }
        else {
            comment.setId(id);
        }
        comment.setName(jsonObject.optString("name"));

        String parentId = jsonObject.optString("parent_id");
        indexStart = parentId.indexOf("_");
        if (indexStart >= 0) {
            comment.setParentId(parentId.substring(indexStart + 1));
        }
        else {
            comment.setParentId(parentId);
        }

        if (comment.getKind().equals("more")) {
            comment.setIsMore(true);
            comment.setCount(jsonObject.optInt("count"));
            List<String> children = new LinkedList<>();
            JSONArray childrenArray = jsonObject.getJSONArray("children");
            for (int index = 0; index < childrenArray.length(); index++) {
                children.add(childrenArray.optString(index));
            }
            comment.setChildren(children);
            return comment;
        }

        // Timestamps multiplied by 1000 as Java uses milliseconds and Reddit uses seconds
        comment.setCreated(jsonObject.optLong("created") * 1000);
        comment.setCreatedUtc(jsonObject.optLong("created_utc") * 1000);

        comment.setApprovedBy(jsonObject.optString("approved_by"));
        comment.setAuthor(jsonObject.optString("author"));
        comment.setAuthorFlairCssClass(jsonObject.optString("author_flair_css_class"));
        comment.setAuthorFlairText(jsonObject.optString("author_flair_text"));
        comment.setBannedBy(jsonObject.optString("banned_by"));
        comment.setBody(jsonObject.optString("body"));
        comment.setBodyHtml(Reddit.getFormattedHtml(jsonObject.optString("body_html")));


        switch (jsonObject.optString("distinguished")) {
            case "null":
                comment.setDistinguished(Reddit.Distinguished.NOT_DISTINGUISHED);
                break;
            case "moderator":
                comment.setDistinguished(Reddit.Distinguished.MODERATOR);
                break;
            case "admin":
                comment.setDistinguished(Reddit.Distinguished.ADMIN);
                break;
            case "special":
                comment.setDistinguished(Reddit.Distinguished.SPECIAL);
                break;
        }

        String edited = jsonObject.optString("edited");
        switch (edited) {
            case "true":
                comment.setEdited(1);
                break;
            case "false":
                comment.setEdited(0);
                break;
            default:
                comment.setEdited(jsonObject.optLong("edited") * 1000);
                break;
        }

        comment.setGilded(jsonObject.optInt("gilded"));

        switch (jsonObject.optString("likes")) {
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

        comment.setLinkId(jsonObject.optString("link_id"));
        comment.setNumReports(jsonObject.optInt("num_reports"));
        comment.setSaved(jsonObject.optBoolean("saved"));
        comment.setScore(jsonObject.optInt("score"));
        comment.setScoreHidden(jsonObject.optBoolean("score_hidden"));
        comment.setSubreddit(jsonObject.optString("subreddit"));
        comment.setSubredditId(jsonObject.optString("subreddit_id"));

        comment.setLinkAuthor(jsonObject.optString("link_author"));
        comment.setLinkTitle(jsonObject.optString("link_title"));
        comment.setLinkUrl(jsonObject.optString("link_url"));

        comment.setIsNew(jsonObject.optBoolean("new"));
        comment.setContext(jsonObject.optString("context"));

//        JSONArray arrayReplies = jsonObject.getJSONArray("replies");
//        ArrayList<Comment> listReplies = new ArrayList<>(arrayReplies.length());
//        for (int index = 0; index < arrayReplies.length(); index++) {
//            listReplies.add(Comment.fromJson(arrayReplies.getJSONObject(index)));
//        }
//
//        comment.setReplies(listReplies);

        return comment;
    }

    public Comment() {
        super();
    }

    public Comment(Comment parent) {
        super();
        this.parent = parent;
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

    public int isLikes() {
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

    public List<Comment> getReplies() {
        return replies;
    }

    public void setReplies(List<Comment> replies) {
        this.replies = replies;
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

    public boolean isReplyExpanded() {
        return replyExpanded;
    }

    public void setReplyExpanded(boolean replyExpanded) {
        this.replyExpanded = replyExpanded;
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

    public boolean isNew() {
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

    public String getReplyText() {
        return replyText;
    }

    public void setReplyText(String replyText) {
        this.replyText = replyText;
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
}
