/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.reddit;

import android.support.annotation.Nullable;
import android.text.Html;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.winsonchiu.reader.utils.UtilsJson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 3/7/2015.
 */
public class Subreddit extends Thing implements JsonSerializable {

    public static final String PUBLIC = "public";
    public static final String PRIVATE = "private";
    public static final String RESTRICTED = "restricted";
    public static final String GOLD_RESTRICTED = "gold_restricted";
    public static final String ARCHIVED = "archived";

    private static final String TAG = Subreddit.class.getCanonicalName();

    private int accountsActive;
    private String bannerImg = "";
    private List<Integer> bannerSize;
    private boolean collapseDeletedComments;
    private int commentScoreHideMins;
    private String description = "";
    private String descriptionHtml = "";
    private String displayName = "";
    private String headerImg = "";
    private List<Integer> headerSize;
    private String headerTitle = "";
    private boolean hideAds;
    private String iconImg = "";
    private List<Integer> iconSize;
    private boolean over18;
    private String publicDescription = "";
    private String publicDescriptionHtml = "";
    private boolean publicTraffic;
    private long subscribers;
    private String submissionType = "";
    private String submitLinkLabel = "";
    private String submitText = "";
    private String submitTextLabel = "";
    private String submitTextHtml = "";
    private String subredditType = "";
    private String title = "";
    private String url = "";
    private boolean userIsBanned;
    private boolean userIsContributor;
    private boolean userIsModerator;
    private boolean userIsSubscriber;
    private long created;
    private long createdUtc;

    public Subreddit() {
        super();
    }

    public static Subreddit fromJson(JsonNode nodeRoot) {

        Subreddit subreddit = new Subreddit();
        subreddit.setKind(UtilsJson.getString(nodeRoot.get("kind")));

        JsonNode nodeData = nodeRoot.get("data");

        subreddit.setId(UtilsJson.getString(nodeData.get("id")));
        subreddit.setName(UtilsJson.getString(nodeData.get("name")));

        // Timestamps multiplied by 1000 as Java uses milliseconds and Reddit uses seconds
        subreddit.setCreated(UtilsJson.getLong(nodeData.get("created")) * 1000);
        subreddit.setCreatedUtc(UtilsJson.getLong(nodeData.get("created_utc")) * 1000);

        subreddit.setAccountsActive(UtilsJson.getInt(nodeData.get("accounts_active")));
        subreddit.setBannerImg(UtilsJson.getString(nodeData.get("banner_img")));

        if (nodeData.hasNonNull("banner_size")) {
            List<Integer> list = new ArrayList<>();
            for (JsonNode jsonNode : nodeData.get("banner_size")) {
                list.add(UtilsJson.getInt(jsonNode));

            }
            subreddit.setBannerSize(list);
        }

        subreddit.setCollapseDeletedComments(UtilsJson.getBoolean(nodeData.get("collapse_deleted_comments")));
        subreddit.setDescription(UtilsJson.getString(nodeData.get("description")));
        subreddit.setDescriptionHtml(UtilsJson.getString(nodeData.get("description_html")));
        subreddit.setDisplayName(UtilsJson.getString(nodeData.get("display_name")));
        subreddit.setHeaderImg(UtilsJson.getString(nodeData.get("header_img")));

        if (nodeData.hasNonNull("header_size")) {
            List<Integer> list = new ArrayList<>();
            for (JsonNode jsonNode : nodeData.get("header_size")) {
                list.add(UtilsJson.getInt(jsonNode));

            }
            subreddit.setHeaderSize(list);
        }

        subreddit.setHeaderTitle(UtilsJson.getString(nodeData.get("header_title")));
        subreddit.setHideAds(UtilsJson.getBoolean(nodeData.get("hide_ads")));
        subreddit.setIconImg(UtilsJson.getString(nodeData.get("icon_img")));

        if (nodeData.hasNonNull("icon_size")) {
            List<Integer> list = new ArrayList<>();
            for (JsonNode jsonNode : nodeData.get("icon_size")) {
                list.add(UtilsJson.getInt(jsonNode));
            }
            subreddit.setIconSize(list);
        }

        subreddit.setOver18(UtilsJson.getBoolean(nodeData.get("over18")));
        subreddit.setPublicDescription(UtilsJson.getString(
                nodeData.get("public_description")));
        subreddit.setPublicDescriptionHtml(UtilsJson.getString(
                nodeData.get("public_description_html")));
        subreddit.setPublicTraffic(UtilsJson.getBoolean(nodeData.get("public_traffic")));
        subreddit.setSubscribers(UtilsJson.getLong(nodeData.get("subscribers")));
        subreddit.setSubmissionType(UtilsJson.getString(nodeData.get("submission_type")));
        subreddit.setSubmitLinkLabel(UtilsJson.getString(nodeData.get("submit_link_label")));
        subreddit.setSubmitText(UtilsJson.getString(nodeData.get("submit_text")));
        subreddit.setSubmitTextLabel(UtilsJson.getString(nodeData.get("submit_text_label")));
        subreddit.setSubmitTextHtml(UtilsJson.getString(nodeData.get("submit_text_html")));
        subreddit.setSubredditType(UtilsJson.getString(nodeData.get("subreddit_type")));
        subreddit.setTitle(UtilsJson.getString(nodeData.get("title")));
        subreddit.setUrl(UtilsJson.getString(nodeData.get("url")));
        subreddit.setUserIsBanned(UtilsJson.getBoolean(nodeData.get("user_is_banned")));
        subreddit.setUserIsContributor(UtilsJson.getBoolean(
                nodeData.get("user_is_contributor")));
        subreddit.setUserIsModerator(
                UtilsJson.getBoolean(nodeData.get("user_is_moderator")));
        subreddit.setUserIsSubscriber(UtilsJson.getBoolean(
                nodeData.get("user_is_subscriber")));

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
    public List<Integer> getBannerSize() {
        return bannerSize;
    }

    public void setBannerSize(List<Integer> bannerSize) {
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
        if (Reddit.NULL.equals(description)) {
            description = "";
        }
        this.description = description;
    }

    public String getDescriptionHtml() {
        return descriptionHtml;
    }

    public void setDescriptionHtml(String descriptionHtml) {
        if (Reddit.NULL.equals(descriptionHtml)) {
            descriptionHtml = "";
        }
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
    public List<Integer> getHeaderSize() {
        return headerSize;
    }

    public void setHeaderSize(List<Integer> headerSize) {
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
    public List<Integer> getIconSize() {
        return iconSize;
    }

    public void setIconSize(List<Integer> iconSize) {
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
        if (Reddit.NULL.equals(publicDescription)) {
            publicDescription = "";
        }
        this.publicDescription = publicDescription;
    }

    public String getPublicDescriptionHtml() {
        return publicDescriptionHtml;
    }

    public void setPublicDescriptionHtml(String publicDescriptionHtml) {
        if (Reddit.NULL.equals(publicDescriptionHtml)) {
            publicDescriptionHtml = "";
        }
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
        if (Reddit.NULL.equals(submitText)) {
            submitText = "";
        }
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
        if (Reddit.NULL.equals(submitTextHtml)) {
            submitTextHtml = "";
        }
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

    @Override
    public void serialize(JsonGenerator gen, SerializerProvider serializers) throws IOException {

        gen.writeStartObject();
        gen.writeStringField("kind", getKind());
        gen.writeObjectFieldStart("data");
        gen.writeStringField("id", getId());
        gen.writeStringField("name", getName());
        gen.writeNumberField("created", getCreated() / 1000);
        gen.writeNumberField("created_utc", getCreatedUtc() / 1000);
        gen.writeNumberField("accounts_active", getAccountsActive());
        gen.writeStringField("banner_img", getBannerImg());
        if (getBannerSize() != null) {
            gen.writeArrayFieldStart("banner_size");
            for (int size : getBannerSize()) {
                gen.writeNumber(size);
            }
            gen.writeEndArray();
        }
        gen.writeBooleanField("collapse_deleted_comments", isCollapseDeletedComments());
        gen.writeStringField("description", getDescription());
        gen.writeStringField("description_html", getDescriptionHtml());
        gen.writeStringField("display_name", getDisplayName());
        gen.writeStringField("header_img", getHeaderImg());
        if (getHeaderSize() != null) {
            gen.writeArrayFieldStart("header_size");
            for (int size : getHeaderSize()) {
                gen.writeNumber(size);
            }
            gen.writeEndArray();
        }
        gen.writeStringField("header_title", getHeaderTitle());
        gen.writeBooleanField("hide_ads", isHideAds());
        gen.writeStringField("icon_img", getIconImg());
        if (getIconSize() != null) {
            gen.writeArrayFieldStart("icon_size");
            for (int size : getIconSize()) {
                gen.writeNumber(size);
            }
            gen.writeEndArray();
        }
        gen.writeBooleanField("over18", isOver18());
        gen.writeStringField("public_description", getPublicDescription());
        gen.writeStringField("public_description_html", getPublicDescriptionHtml());
        gen.writeBooleanField("public_traffic", isPublicTraffic());
        gen.writeNumberField("subscribers", getSubscribers());
        gen.writeStringField("submission_type", getSubmissionType());
        gen.writeStringField("submit_link_label", getSubmitLinkLabel());
        gen.writeStringField("submit_text", getSubmitText());
        gen.writeStringField("submit_text_label", getSubmitLinkLabel());
        gen.writeStringField("submit_text_html", getSubmitTextHtml());
        gen.writeStringField("subreddit_type", getSubredditType());
        gen.writeStringField("title", getTitle());
        gen.writeStringField("url", getUrl());
        gen.writeBooleanField("user_is_banned", isUserIsBanned());
        gen.writeBooleanField("user_is_contributor", isUserIsContributor());
        gen.writeBooleanField("user_is_moderator", isUserIsModerator());
        gen.writeBooleanField("user_is_subscriber", isUserIsSubscriber());
        gen.writeEndObject();
        gen.writeEndObject();
    }

    @Override
    public void serializeWithType(JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
        serialize(gen, serializers);
    }
}
