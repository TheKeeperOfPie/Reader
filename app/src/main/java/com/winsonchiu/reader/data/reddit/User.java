/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.reddit;

import com.fasterxml.jackson.databind.JsonNode;
import com.winsonchiu.reader.utils.UtilsJson;

/**
 * Created by TheKeeperOfPie on 4/22/2015.
 */
public class User {

    private static final String TAG = User.class.getCanonicalName();

    private boolean hasMail;
    private String name = "";
    private long created;
    private boolean hideFromRobots;
    private int goldCreddits;
    private long createdUtc;
    private boolean hasModMail;
    private int linkKarma;
    private int commentKarma;
    private boolean over18;
    private boolean isGold;
    private boolean isMod;
    private long goldExpiration;
    private boolean hasVerifiedEmail;
    private String id = "";
    private int inboxCount;

    public static User fromJson(JsonNode nodeRoot) {

        User user = new User();

        user.setName(UtilsJson.getString(nodeRoot.get("name")));
        user.setHideFromRobots(UtilsJson.getBoolean(nodeRoot.get("hide_from_robots")));

        // Timestamps multiplied by 1000 as Java uses milliseconds and Reddit uses seconds
        user.setCreated(UtilsJson.getLong(nodeRoot.get("created")) * 1000);
        user.setCreatedUtc(UtilsJson.getLong(nodeRoot.get("created_utc")) * 1000);
        user.setGoldExpiration(UtilsJson.getLong(nodeRoot.get("gold_expiration")) * 1000);

        user.setLinkKarma(UtilsJson.getInt(nodeRoot.get("link_karma")));
        user.setCommentKarma(UtilsJson.getInt(nodeRoot.get("comment_karma")));
        user.setIsGold(UtilsJson.getBoolean(nodeRoot.get("is_gold")));
        user.setIsMod(UtilsJson.getBoolean(nodeRoot.get("is_mod")));
        user.setHasVerifiedEmail(
                UtilsJson.getBoolean(nodeRoot.get("has_verified_email")));
        user.setId(UtilsJson.getString(nodeRoot.get("id")));
        user.setHasMail(UtilsJson.getBoolean(nodeRoot.get("has_mail")));
        user.setInboxCount(UtilsJson.getInt(nodeRoot.get("inbox_count")));
        user.setGoldCreddits(UtilsJson.getInt(nodeRoot.get("gold_creddits")));
        user.setHasModMail(UtilsJson.getBoolean(nodeRoot.get("has_mod_mail")));
        user.setOver18(UtilsJson.getBoolean(nodeRoot.get("over_18")));

        return user;
    }

    public boolean isHasMail() {
        return hasMail;
    }

    public void setHasMail(boolean hasMail) {
        this.hasMail = hasMail;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public boolean isHideFromRobots() {
        return hideFromRobots;
    }

    public void setHideFromRobots(boolean hideFromRobots) {
        this.hideFromRobots = hideFromRobots;
    }

    public int getGoldCreddits() {
        return goldCreddits;
    }

    public void setGoldCreddits(int goldCreddits) {
        this.goldCreddits = goldCreddits;
    }

    public long getCreatedUtc() {
        return createdUtc;
    }

    public void setCreatedUtc(long createdUtc) {
        this.createdUtc = createdUtc;
    }

    public boolean isHasModMail() {
        return hasModMail;
    }

    public void setHasModMail(boolean hasModMail) {
        this.hasModMail = hasModMail;
    }

    public int getLinkKarma() {
        return linkKarma;
    }

    public void setLinkKarma(int linkKarma) {
        this.linkKarma = linkKarma;
    }

    public int getCommentKarma() {
        return commentKarma;
    }

    public void setCommentKarma(int commentKarma) {
        this.commentKarma = commentKarma;
    }

    public boolean isOver18() {
        return over18;
    }

    public void setOver18(boolean over18) {
        this.over18 = over18;
    }

    public boolean isGold() {
        return isGold;
    }

    public void setIsGold(boolean isGold) {
        this.isGold = isGold;
    }

    public boolean isMod() {
        return isMod;
    }

    public void setIsMod(boolean isMod) {
        this.isMod = isMod;
    }

    public long getGoldExpiration() {
        return goldExpiration;
    }

    public void setGoldExpiration(long goldExpiration) {
        this.goldExpiration = goldExpiration;
    }

    public boolean isHasVerifiedEmail() {
        return hasVerifiedEmail;
    }

    public void setHasVerifiedEmail(boolean hasVerifiedEmail) {
        this.hasVerifiedEmail = hasVerifiedEmail;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getInboxCount() {
        return inboxCount;
    }

    public void setInboxCount(int inboxCount) {
        this.inboxCount = inboxCount;
    }
}
