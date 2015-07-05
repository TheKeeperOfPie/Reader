/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data;

import android.support.annotation.Nullable;
import android.text.Html;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class Subreddit extends Thing {

    public static final String PUBLIC = "public";
    public static final String PRIVATE = "private";
    public static final String RESTRICTED = "restricted";
    public static final String GOLD_RESTRICTED = "gold_restricted";
    public static final String ARCHIVED = "archived";

    private static final String TAG = Subreddit.class.getCanonicalName();

    private int accountsActive;
    private String bannerImg;
    private int[] bannerSize;
    private boolean collapseDeletedComments;
    private int commentScoreHideMins;
    private String description;
    private String descriptionHtml;
    private String displayName;
    private String headerImg;
    private int[] headerSize;
    private String headerTitle;
    private boolean hideAds;
    private String iconImg;
    private int[] iconSize;
    private boolean over18;
    private String publicDescription;
    private String publicDescriptionHtml;
    private boolean publicTraffic;
    private long subscribers;
    private String submissionType;
    private String submitLinkLabel;
    private String submitText;
    private String submitTextLabel;
    private String submitTextHtml;
    private String subredditType;
    private String title;
    private String url;
    private boolean userIsBanned;
    private boolean userIsContributor;
    private boolean userIsModerator;
    private boolean userIsSubscriber;
    private long created;
    private long createdUtc;

    public Subreddit() {
        super();
    }

    public String toJsonString() throws JSONException {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", getId());
        jsonObject.put("name", getName());
        jsonObject.put("accountsActive", accountsActive);
        jsonObject.put("bannerImg", bannerImg);
        jsonObject.put("bannerSize", bannerSize);
        jsonObject.put("collapseDeletedComments", collapseDeletedComments);
        jsonObject.put("commentScoreHideMins", commentScoreHideMins);
        jsonObject.put("description", description);
        jsonObject.put("descriptionHtml", descriptionHtml);
        jsonObject.put("displayName", displayName);
        jsonObject.put("headerImg", headerImg);
        jsonObject.put("headerSize", headerSize);
        jsonObject.put("headerTitle", headerTitle);
        jsonObject.put("hideAds", hideAds);
        jsonObject.put("iconImg", iconImg);
        jsonObject.put("iconSize", iconSize);
        jsonObject.put("over18", over18);
        jsonObject.put("publicDescription", publicDescription);
        jsonObject.put("publicDescriptionHtml", publicDescriptionHtml);
        jsonObject.put("publicTraffic", publicTraffic);
        jsonObject.put("subscribers", subscribers);
        jsonObject.put("submissionType", submissionType);
        jsonObject.put("submitLinkLabel", submitLinkLabel);
        jsonObject.put("submitText", submitText);
        jsonObject.put("submitTextLabel", submitTextLabel);
        jsonObject.put("submitTextHtml", submitTextHtml);
        jsonObject.put("subredditType", subredditType);
        jsonObject.put("title", title);
        jsonObject.put("url", url);
        jsonObject.put("userIsBanned", userIsBanned);
        jsonObject.put("userIsContributor", userIsContributor);
        jsonObject.put("userIsModerator", userIsModerator);
        jsonObject.put("userIsSubscriber", userIsSubscriber);

        // Timestamps divided by 1000 so fromJson can recreate the object correctly
        jsonObject.put("created", created / 1000);
        jsonObject.put("createdUtc", createdUtc / 1000);

        JSONObject rootJsonObject = new JSONObject();
        rootJsonObject.put("kind", getKind());
        rootJsonObject.put("data", jsonObject);

        return jsonObject.toString();
    }

    public static Subreddit fromJson(JSONObject rootJsonObject) throws JSONException {

        Subreddit subreddit = new Subreddit();
        subreddit.setKind(rootJsonObject.optString("kind"));

        JSONObject jsonObject = rootJsonObject.getJSONObject("data");

        subreddit.setId(jsonObject.optString("id"));
        subreddit.setName(jsonObject.optString("name"));

        // Timestamps multiplied by 1000 as Java uses milliseconds and Reddit uses seconds
        subreddit.setCreated(jsonObject.optLong("created") * 1000);
        subreddit.setCreatedUtc(jsonObject.optLong("created_utc") * 1000);

        subreddit.setAccountsActive(jsonObject.optInt("accounts_active"));
        subreddit.setBannerImg(jsonObject.optString("banner_img"));

        if (!"null".equalsIgnoreCase(jsonObject.optString("banner_size"))) {
            JSONArray jsonArray = jsonObject.getJSONArray("banner_size");
            int[] array = new int[jsonArray.length()];
            for (int index = 0; index < jsonArray.length(); index++) {
                array[index] = jsonArray.optInt(index);
            }
            subreddit.setBannerSize(array);
        }

        subreddit.setCollapseDeletedComments(jsonObject.optBoolean("collapse_deleted_comments"));
        subreddit.setDescription(jsonObject.optString("description"));
        subreddit.setDescriptionHtml(jsonObject.optString("description_html"));
        subreddit.setDisplayName(jsonObject.optString("display_name"));
        subreddit.setHeaderImg(jsonObject.optString("header_img"));

        if (!"null".equalsIgnoreCase(jsonObject.optString("header_size"))) {
            JSONArray jsonArray = jsonObject.getJSONArray("header_size");
            int[] array = new int[jsonArray.length()];
            for (int index = 0; index < jsonArray.length(); index++) {
                array[index] = jsonArray.optInt(index);
            }
            subreddit.setHeaderSize(array);
        }

        subreddit.setHeaderTitle(jsonObject.optString("header_title"));
        subreddit.setHideAds(jsonObject.optBoolean("hide_ads"));
        subreddit.setIconImg(jsonObject.optString("icon_img"));

        if (!"null".equalsIgnoreCase(jsonObject.optString("icon_size"))) {
            JSONArray jsonArray = jsonObject.getJSONArray("icon_size");
            int[] array = new int[jsonArray.length()];
            for (int index = 0; index < jsonArray.length(); index++) {
                array[index] = jsonArray.optInt(index);
            }
            subreddit.setIconSize(array);
        }

        subreddit.setOver18(jsonObject.optBoolean("over18"));
        subreddit.setPublicDescription(jsonObject.optString("public_description"));
        subreddit.setPublicDescriptionHtml(jsonObject.optString("public_description_html"));
        subreddit.setPublicTraffic(jsonObject.optBoolean("public_traffic"));
        subreddit.setSubscribers(jsonObject.optLong("subscribers"));
        subreddit.setSubmissionType(jsonObject.optString("submission_type"));
        subreddit.setSubmitLinkLabel(jsonObject.optString("submit_link_label"));
        subreddit.setSubmitText(jsonObject.optString("submit_text"));
        subreddit.setSubmitTextLabel(jsonObject.optString("submit_text_label"));
        subreddit.setSubmitTextHtml(jsonObject.optString("submit_text_html"));
        subreddit.setSubredditType(jsonObject.optString("subreddit_type"));
        subreddit.setTitle(Html.fromHtml(jsonObject.optString("title")).toString());
        subreddit.setUrl(jsonObject.optString("url"));
        subreddit.setUserIsBanned(jsonObject.optBoolean("user_is_banned"));
        subreddit.setUserIsContributor(jsonObject.optBoolean("user_is_contributor"));
        subreddit.setUserIsModerator(jsonObject.optBoolean("user_is_moderator"));
        subreddit.setUserIsSubscriber(jsonObject.optBoolean("user_is_subscriber"));

        return subreddit;
    }

    public int getAccountsActive() {
        return accountsActive;
    }

    public void setAccountsActive(int accountsActive) {
        this.accountsActive = accountsActive;
    }

    public String getBannerImg() {
        return bannerImg;
    }

    public void setBannerImg(String bannerImg) {
        this.bannerImg = bannerImg;
    }

    @Nullable
    public int[] getBannerSize() {
        return bannerSize;
    }

    public void setBannerSize(int[] bannerSize) {
        this.bannerSize = bannerSize;
    }

    public boolean isCollapseDeletedComments() {
        return collapseDeletedComments;
    }

    public void setCollapseDeletedComments(boolean collapseDeletedComments) {
        this.collapseDeletedComments = collapseDeletedComments;
    }

    public int getCommentScoreHideMins() {
        return commentScoreHideMins;
    }

    public void setCommentScoreHideMins(int commentScoreHideMins) {
        this.commentScoreHideMins = commentScoreHideMins;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CharSequence getDescriptionHtml() {
        return Reddit.getFormattedHtml(descriptionHtml);
    }

    public void setDescriptionHtml(String descriptionHtml) {
        this.descriptionHtml = descriptionHtml;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getHeaderImg() {
        return headerImg;
    }

    public void setHeaderImg(String headerImg) {
        this.headerImg = headerImg;
    }

    @Nullable
    public int[] getHeaderSize() {
        return headerSize;
    }

    public void setHeaderSize(int[] headerSize) {
        this.headerSize = headerSize;
    }

    public String getHeaderTitle() {
        return headerTitle;
    }

    public void setHeaderTitle(String headerTitle) {
        this.headerTitle = headerTitle;
    }

    public boolean isHideAds() {
        return hideAds;
    }

    public void setHideAds(boolean hideAds) {
        this.hideAds = hideAds;
    }

    public String getIconImg() {
        return iconImg;
    }

    public void setIconImg(String iconImg) {
        this.iconImg = iconImg;
    }

    @Nullable
    public int[] getIconSize() {
        return iconSize;
    }

    public void setIconSize(int[] iconSize) {
        this.iconSize = iconSize;
    }

    public boolean isOver18() {
        return over18;
    }

    public void setOver18(boolean over18) {
        this.over18 = over18;
    }

    public String getPublicDescription() {
        return publicDescription;
    }

    public void setPublicDescription(String publicDescription) {
        this.publicDescription = publicDescription;
    }

    public CharSequence getPublicDescriptionHtml() {
        return Reddit.getFormattedHtml(publicDescriptionHtml);
    }

    public void setPublicDescriptionHtml(String publicDescriptionHtml) {
        this.publicDescriptionHtml = publicDescriptionHtml;
    }

    public boolean isPublicTraffic() {
        return publicTraffic;
    }

    public void setPublicTraffic(boolean publicTraffic) {
        this.publicTraffic = publicTraffic;
    }

    public long getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(long subscribers) {
        this.subscribers = subscribers;
    }

    public String getSubmissionType() {
        return submissionType;
    }

    public void setSubmissionType(String submissionType) {
        this.submissionType = submissionType;
    }

    public String getSubmitLinkLabel() {
        return submitLinkLabel;
    }

    public void setSubmitLinkLabel(String submitLinkLabel) {
        this.submitLinkLabel = submitLinkLabel;
    }

    public String getSubmitText() {
        return submitText;
    }

    public void setSubmitText(String submitText) {
        this.submitText = submitText;
    }

    public String getSubmitTextLabel() {
        return submitTextLabel;
    }

    public void setSubmitTextLabel(String submitTextLabel) {
        this.submitTextLabel = submitTextLabel;
    }

    public String getSubmitTextHtml() {
        return submitTextHtml;
    }

    public void setSubmitTextHtml(String submitTextHtml) {
        this.submitTextHtml = submitTextHtml;
    }

    public String getSubredditType() {
        return subredditType;
    }

    public void setSubredditType(String subredditType) {
        this.subredditType = subredditType;
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

    public boolean isUserIsBanned() {
        return userIsBanned;
    }

    public void setUserIsBanned(boolean userIsBanned) {
        this.userIsBanned = userIsBanned;
    }

    public boolean isUserIsContributor() {
        return userIsContributor;
    }

    public void setUserIsContributor(boolean userIsContributor) {
        this.userIsContributor = userIsContributor;
    }

    public boolean isUserIsModerator() {
        return userIsModerator;
    }

    public void setUserIsModerator(boolean userIsModerator) {
        this.userIsModerator = userIsModerator;
    }

    public boolean isUserIsSubscriber() {
        return userIsSubscriber;
    }

    public void setUserIsSubscriber(boolean userIsSubscriber) {
        this.userIsSubscriber = userIsSubscriber;
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
}
