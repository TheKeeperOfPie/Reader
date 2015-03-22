package com.winsonchiu.reader.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class Link extends Thing {

    private static final String TAG = Link.class.getCanonicalName();

    private String author;
    private String authorFlairCssClass;
    private String authorFlairText;
    private boolean clicked;
    private String domain;
    private boolean hidden;
    private boolean isSelf;
    private int likes;
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
    private Reddit.Distinguished distinguished;
    private boolean stickied;

    private Listing comments;

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
                link.setLikes(0);
                break;
            case "true":
                link.setLikes(1);
                break;
            case "false":
                link.setLikes(-1);
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

        String edited = jsonObject.getString("edited");
        switch (edited) {
            case "true":
                link.setEdited(1);
                break;
            case "false":
                link.setEdited(0);
                break;
            default:
                link.setEdited(jsonObject.getLong("edited"));
                break;
        }

        switch (jsonObject.getString("distinguished")) {
            case "null":
                link.setDistinguished(Reddit.Distinguished.NOT_DISTINGUISHED);
                break;
            case "moderator":
                link.setDistinguished(Reddit.Distinguished.MODERATOR);
                break;
            case "admin":
                link.setDistinguished(Reddit.Distinguished.ADMIN);
                break;
            case "special":
                link.setDistinguished(Reddit.Distinguished.SPECIAL);
                break;
        }

        link.setStickied(jsonObject.getBoolean("stickied"));

        return link;
    }

    public static Link fromJson(JSONArray jsonArray) throws JSONException {

        Link link = fromJson(jsonArray.getJSONObject(0)
                .getJSONObject("data")
                .getJSONArray("children")
                .getJSONObject(0));

        List<Comment> comments = new ArrayList<>();

        JSONObject jsonObject = jsonArray.getJSONObject(1);

        Listing listing = Listing.fromJson(jsonObject);

//        JSONArray arrayComments = jsonArray.getJSONObject(1).getJSONObject("data").getJSONArray("children");
//
//        for (int index = 0; index < arrayComments.length(); index++) {
//            Comment.addAllFromJson(comments, arrayComments.getJSONObject(index), 0);
//        }

        link.setComments(listing);

        return link;
    }

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

    public int isLikes() {
        return likes;
    }

    public void setLikes(int likes) {
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

    public Reddit.Distinguished getDistinguished() {
        return distinguished;
    }

    public void setDistinguished(Reddit.Distinguished distinguished) {
        this.distinguished = distinguished;
    }

    public boolean isStickied() {
        return stickied;
    }

    public void setStickied(boolean stickied) {
        this.stickied = stickied;
    }

    public Listing getComments() {
        return comments;
    }

    public void setComments(Listing comments) {
        this.comments = comments;
    }
}
