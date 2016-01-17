/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.imgur;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by TheKeeperOfPie on 3/19/2015.
 */
public class Image implements Parcelable {

    private String id;
    private String title;
    private String description;
    private int dateTime;
    private String type;
    private boolean animated;
    private int width;
    private int height;
    private int size;
    private int views;
    private int bandwidth;
    private String section;
    private String link;
    private String gifv;
    private String mp4;
    private String webm;
    private boolean favorite;
    private boolean nsfw;
    private String accountUrl;
    private int accountId;

    public Image() {
        super();
    }

    public static Image fromJson(JSONObject jsonObject) throws JSONException {
        Image image = new Image();

        image.setId(jsonObject.optString("id"));
        image.setTitle(jsonObject.optString("title"));
        image.setDescription(jsonObject.optString("description"));
        image.setDateTime(jsonObject.optInt("datetime"));
        image.setType(jsonObject.optString("type"));
        image.setAnimated(jsonObject.optBoolean("animated"));
        image.setWidth(jsonObject.optInt("width"));
        image.setHeight(jsonObject.optInt("height"));
        image.setSize(jsonObject.optInt("size"));
        image.setViews(jsonObject.optInt("views"));
        image.setBandwidth(jsonObject.optInt("bandwidth"));
        image.setFavorite(jsonObject.optBoolean("favorite"));
        image.setNsfw(jsonObject.optBoolean("nsfw"));
        image.setSection(jsonObject.optString("section"));
        image.setAccountUrl(jsonObject.optString("account_url"));
        image.setAccountId(jsonObject.optInt("account_id"));
        image.setLink(jsonObject.optString("link"));
        image.setGifv(jsonObject.optString("gifv"));
        image.setMp4(jsonObject.optString("mp4"));
        image.setWebm(jsonObject.optString("webm"));

        return image;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getDateTime() {
        return dateTime;
    }

    public void setDateTime(int dateTime) {
        this.dateTime = dateTime;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isAnimated() {
        return animated;
    }

    public void setAnimated(boolean animated) {
        this.animated = animated;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getViews() {
        return views;
    }

    public void setViews(int views) {
        this.views = views;
    }

    public int getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(int bandwidth) {
        this.bandwidth = bandwidth;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getGifv() {
        return gifv;
    }

    public void setGifv(String gifv) {
        this.gifv = gifv;
    }

    public String getMp4() {
        return mp4;
    }

    public void setMp4(String mp4) {
        this.mp4 = mp4;
    }

    public String getWebm() {
        return webm;
    }

    public void setWebm(String webm) {
        this.webm = webm;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public boolean isNsfw() {
        return nsfw;
    }

    public void setNsfw(boolean nsfw) {
        this.nsfw = nsfw;
    }

    public String getAccountUrl() {
        return accountUrl;
    }

    public void setAccountUrl(String accountUrl) {
        this.accountUrl = accountUrl;
    }

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.title);
        dest.writeString(this.description);
        dest.writeInt(this.dateTime);
        dest.writeString(this.type);
        dest.writeByte(animated ? (byte) 1 : (byte) 0);
        dest.writeInt(this.width);
        dest.writeInt(this.height);
        dest.writeInt(this.size);
        dest.writeInt(this.views);
        dest.writeInt(this.bandwidth);
        dest.writeString(this.section);
        dest.writeString(this.link);
        dest.writeString(this.gifv);
        dest.writeString(this.mp4);
        dest.writeString(this.webm);
        dest.writeByte(favorite ? (byte) 1 : (byte) 0);
        dest.writeByte(nsfw ? (byte) 1 : (byte) 0);
        dest.writeString(this.accountUrl);
        dest.writeInt(this.accountId);
    }

    protected Image(Parcel in) {
        this.id = in.readString();
        this.title = in.readString();
        this.description = in.readString();
        this.dateTime = in.readInt();
        this.type = in.readString();
        this.animated = in.readByte() != 0;
        this.width = in.readInt();
        this.height = in.readInt();
        this.size = in.readInt();
        this.views = in.readInt();
        this.bandwidth = in.readInt();
        this.section = in.readString();
        this.link = in.readString();
        this.gifv = in.readString();
        this.mp4 = in.readString();
        this.webm = in.readString();
        this.favorite = in.readByte() != 0;
        this.nsfw = in.readByte() != 0;
        this.accountUrl = in.readString();
        this.accountId = in.readInt();
    }

    public static final Creator<Image> CREATOR = new Creator<Image>() {
        public Image createFromParcel(Parcel source) {
            return new Image(source);
        }

        public Image[] newArray(int size) {
            return new Image[size];
        }
    };
}
