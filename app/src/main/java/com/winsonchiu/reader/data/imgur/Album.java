/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.data.imgur;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 3/19/2015.
 */
public class Album implements Parcelable {

    private String id;
    private String title;
    private String description;
    private int dateTime;
    private String cover;
    private int coverWidth;
    private int coverHeight;
    private String accountUrl;
    private int accountId;
    private String privacy;
    private String layout;
    private int views;
    private String link;
    private boolean favorite;
    private boolean nsfw;
    private String section;
    private int imagesCount;
    private List<Image> images;

    private int page;

    public Album() {
        super();
    }

    public static Album fromJson(JSONObject jsonObject) throws JSONException {

        Album album = new Album();

        album.setId(jsonObject.optString("id"));
        album.setTitle(jsonObject.optString("title"));
        album.setDescription(jsonObject.optString("description"));
        album.setDateTime(jsonObject.optInt("datetime"));
        album.setCover(jsonObject.optString("cover"));
        album.setCoverWidth(jsonObject.optInt("cover_width"));
        album.setCoverHeight(jsonObject.optInt("cover_height"));
        album.setAccountUrl(jsonObject.optString("account_url"));
        album.setAccountId(jsonObject.optInt("account_id"));
        album.setPrivacy(jsonObject.optString("privacy"));
        album.setLayout(jsonObject.optString("layout"));
        album.setViews(jsonObject.optInt("views"));
        album.setLink(jsonObject.optString("link"));
        album.setFavorite(jsonObject.optBoolean("favorite"));
        album.setNsfw(jsonObject.optBoolean("nsfw"));
        album.setSection(jsonObject.optString("section"));

        List<Image> images = new ArrayList<>(album.getImagesCount());

        if (jsonObject.has("images")) {
            JSONArray imageArray = jsonObject.getJSONArray("images");
            for (int index = 0; index < imageArray.length(); index++) {
                images.add(Image.fromJson(imageArray.getJSONObject(index)));
            }
        }
        else {
            images.add(Image.fromJson(jsonObject));
        }

        album.setImagesCount(images.size());
        album.setImages(images);

        return album;
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

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public int getCoverWidth() {
        return coverWidth;
    }

    public void setCoverWidth(int coverWidth) {
        this.coverWidth = coverWidth;
    }

    public int getCoverHeight() {
        return coverHeight;
    }

    public void setCoverHeight(int coverHeight) {
        this.coverHeight = coverHeight;
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

    public String getPrivacy() {
        return privacy;
    }

    public void setPrivacy(String privacy) {
        this.privacy = privacy;
    }

    public String getLayout() {
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout;
    }

    public int getViews() {
        return views;
    }

    public void setViews(int views) {
        this.views = views;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
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

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public int getImagesCount() {
        return imagesCount;
    }

    public void setImagesCount(int imagesCount) {
        this.imagesCount = imagesCount;
    }

    public List<Image> getImages() {
        return images;
    }

    public void setImages(List<Image> images) {
        this.images = images;
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
        dest.writeString(this.cover);
        dest.writeInt(this.coverWidth);
        dest.writeInt(this.coverHeight);
        dest.writeString(this.accountUrl);
        dest.writeInt(this.accountId);
        dest.writeString(this.privacy);
        dest.writeString(this.layout);
        dest.writeInt(this.views);
        dest.writeString(this.link);
        dest.writeByte(favorite ? (byte) 1 : (byte) 0);
        dest.writeByte(nsfw ? (byte) 1 : (byte) 0);
        dest.writeString(this.section);
        dest.writeInt(this.imagesCount);
        dest.writeList(this.images);
    }

    protected Album(Parcel in) {
        this.id = in.readString();
        this.title = in.readString();
        this.description = in.readString();
        this.dateTime = in.readInt();
        this.cover = in.readString();
        this.coverWidth = in.readInt();
        this.coverHeight = in.readInt();
        this.accountUrl = in.readString();
        this.accountId = in.readInt();
        this.privacy = in.readString();
        this.layout = in.readString();
        this.views = in.readInt();
        this.link = in.readString();
        this.favorite = in.readByte() != 0;
        this.nsfw = in.readByte() != 0;
        this.section = in.readString();
        this.imagesCount = in.readInt();
        this.images = new ArrayList<Image>();
        in.readList(this.images, List.class.getClassLoader());
    }

    public static final Creator<Album> CREATOR = new Creator<Album>() {
        public Album createFromParcel(Parcel source) {
            return new Album(source);
        }

        public Album[] newArray(int size) {
            return new Album[size];
        }
    };

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }
}
