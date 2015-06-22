package com.winsonchiu.reader.data;

import org.json.JSONException;
import org.json.JSONObject;

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


    public static User fromJson(JSONObject rootJsonObject) throws JSONException {

        User user = new User();

        user.setName(rootJsonObject.optString("name"));
        user.setHideFromRobots(rootJsonObject.optBoolean("hide_from_robots"));

        // Timestamps multiplied by 1000 as Java uses milliseconds and Reddit uses seconds
        user.setCreated(rootJsonObject.optLong("created") * 1000);
        user.setCreatedUtc(rootJsonObject.optLong("created_utc") * 1000);
        user.setGoldExpiration(rootJsonObject.optLong("gold_expiration") * 1000);

        user.setLinkKarma(rootJsonObject.optInt("link_karma"));
        user.setCommentKarma(rootJsonObject.optInt("comment_karma"));
        user.setIsGold(rootJsonObject.optBoolean("is_gold"));
        user.setIsMod(rootJsonObject.optBoolean("is_mod"));
        user.setHasVerifiedEmail(rootJsonObject.optBoolean("has_verified_email"));
        user.setId(rootJsonObject.optString("id"));
        user.setHasMail(rootJsonObject.optBoolean("has_mail"));
        user.setInboxCount(rootJsonObject.optInt("inbox_count"));
        user.setGoldCreddits(rootJsonObject.optInt("gold_creddits"));
        user.setHasModMail(rootJsonObject.optBoolean("has_mod_mail"));
        user.setOver18(rootJsonObject.optBoolean("over_18"));

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
