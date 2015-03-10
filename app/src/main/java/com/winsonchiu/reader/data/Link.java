package com.winsonchiu.reader.data;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class Link extends Thing {


    // Constant values to represent Link states
    public enum Vote {
        NOT_VOTED, UPVOTED, DOWNVOTED
    }
    public enum Distinguished {
        NOT_DISTINGUISHED, MODERATOR, ADMIN, SPECIAL
    }

    private String author;
    private String authorFlairCssClass;
    private String authorFlairText;
    private boolean clicked;
    private String domain;
    private boolean hidden;
    private boolean isSelf;
    private Vote likes;
    private String linkFlairCssClass;
    private String linkFlairText;
    private String media;
    private String mediaEmbed;
    private int numComments;
    private boolean over18;
    private String permalink;
    private boolean saved;
    private int score;
    private String selfText;
    private String selfTextHtml;
    private String subreddit;
    private String subredditId;
    private String thumbnail;
    private String title;
    private String url;
    private long edited;
    private Distinguished distinguished;
    private boolean stickied;

    public Link() {
        super();
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

    public boolean isClicked() {
        return clicked;
    }

    public void setClicked(boolean clicked) {
        this.clicked = clicked;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public boolean isSelf() {
        return isSelf;
    }

    public void setSelf(boolean isSelf) {
        this.isSelf = isSelf;
    }

    public Vote getLikes() {
        return likes;
    }

    public void setLikes(Vote likes) {
        this.likes = likes;
    }

    public String getLinkFlairCssClass() {
        return linkFlairCssClass;
    }

    public void setLinkFlairCssClass(String linkFlairCssClass) {
        this.linkFlairCssClass = linkFlairCssClass;
    }

    public String getLinkFlairText() {
        return linkFlairText;
    }

    public void setLinkFlairText(String linkFlairText) {
        this.linkFlairText = linkFlairText;
    }

    public String getMedia() {
        return media;
    }

    public void setMedia(String media) {
        this.media = media;
    }

    public String getMediaEmbed() {
        return mediaEmbed;
    }

    public void setMediaEmbed(String mediaEmbed) {
        this.mediaEmbed = mediaEmbed;
    }

    public int getNumComments() {
        return numComments;
    }

    public void setNumComments(int numComments) {
        this.numComments = numComments;
    }

    public boolean isOver18() {
        return over18;
    }

    public void setOver18(boolean over18) {
        this.over18 = over18;
    }

    public String getPermalink() {
        return permalink;
    }

    public void setPermalink(String permalink) {
        this.permalink = permalink;
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

    public String getSelfText() {
        return selfText;
    }

    public void setSelfText(String selfText) {
        this.selfText = selfText;
    }

    public String getSelfTextHtml() {
        return selfTextHtml;
    }

    public void setSelfTextHtml(String selfTextHtml) {
        this.selfTextHtml = selfTextHtml;
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

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getEdited() {
        return edited;
    }

    public void setEdited(long edited) {
        this.edited = edited;
    }

    public Distinguished getDistinguished() {
        return distinguished;
    }

    public void setDistinguished(Distinguished distinguished) {
        this.distinguished = distinguished;
    }

    public boolean isStickied() {
        return stickied;
    }

    public void setStickied(boolean stickied) {
        this.stickied = stickied;
    }

    public static Link fromJson(JSONObject rootJsonObject) throws JSONException {

        Link link = new Link();
        link.setKind(rootJsonObject.getString("kind"));

        JSONObject jsonObject = rootJsonObject.getJSONObject("data");

        link.setId(jsonObject.getString("id"));
        link.setName(jsonObject.getString("name"));

        link.setAuthor(jsonObject.getString("author"));
        link.setAuthorFlairCssClass(jsonObject.getString("author_flair_css_class"));
        link.setAuthorFlairText(jsonObject.getString("author_flair_text"));
        link.setClicked(jsonObject.getBoolean("clicked"));
        link.setDomain(jsonObject.getString("domain"));
        link.setHidden(jsonObject.getBoolean("hidden"));
        link.setSelf(jsonObject.getBoolean("is_self"));

        switch (jsonObject.getString("likes")) {
            case "null":
                link.setLikes(Vote.NOT_VOTED);
                break;
            case "true":
                link.setLikes(Vote.UPVOTED);
                break;
            case "false":
                link.setLikes(Vote.DOWNVOTED);
                break;
        }

        link.setLinkFlairCssClass(jsonObject.getString("link_flair_css_class"));
        link.setLinkFlairText(jsonObject.getString("link_flair_text"));
        link.setMedia(jsonObject.getString("media"));
        link.setMediaEmbed(jsonObject.getString("media_embed"));
        link.setNumComments(jsonObject.getInt("num_comments"));
        link.setOver18(jsonObject.getBoolean("over_18"));
        link.setPermalink(jsonObject.getString("permalink"));
        link.setSaved(jsonObject.getBoolean("saved"));
        link.setScore(jsonObject.getInt("score"));
        link.setSelfText(jsonObject.getString("selftext"));
        link.setSelfTextHtml(jsonObject.getString("selftext_html"));
        link.setSubreddit(jsonObject.getString("subreddit"));
        link.setSubredditId(jsonObject.getString("subreddit_id"));
        link.setThumbnail(jsonObject.getString("thumbnail"));
        link.setTitle(jsonObject.getString("title"));
        link.setUrl(jsonObject.getString("url"));

        link.setEdited(jsonObject.getString("edited").equals("false") ? 0 : jsonObject.getLong("edited"));

        switch (jsonObject.getString("distinguished")) {
            case "null":
                link.setDistinguished(Distinguished.NOT_DISTINGUISHED);
                break;
            case "moderator":
                link.setDistinguished(Distinguished.MODERATOR);
                break;
            case "admin":
                link.setDistinguished(Distinguished.ADMIN);
                break;
            case "special":
                link.setDistinguished(Distinguished.SPECIAL);
                break;
        }

        link.setStickied(jsonObject.getBoolean("stickied"));

        return link;
    }

}
