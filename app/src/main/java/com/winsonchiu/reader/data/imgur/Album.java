package com.winsonchiu.reader.data.imgur;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 3/19/2015.
 */
public class Album {

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

    public Album() {
        super();
    }

    public static Album fromJson(JSONObject jsonObject) throws JSONException {

        Album album = new Album();

        album.setId(jsonObject.getString("id"));
        album.setTitle(jsonObject.getString("title"));
        album.setDescription(jsonObject.getString("description"));
        album.setDateTime(jsonObject.getInt("datetime"));
        album.setCover(jsonObject.getString("cover"));
        album.setCoverWidth(jsonObject.getInt("cover_width"));
        album.setCoverHeight(jsonObject.getInt("cover_height"));
        album.setAccountUrl(jsonObject.getString("account_url"));
        album.setAccountId(jsonObject.getInt("account_id"));
        album.setPrivacy(jsonObject.getString("privacy"));
        album.setLayout(jsonObject.getString("layout"));
        album.setViews(jsonObject.getInt("views"));
        album.setLink(jsonObject.getString("link"));
        album.setFavorite(jsonObject.getBoolean("favorite"));

        if (!jsonObject.getString("nsfw").equals("null")) {
            album.setNsfw(jsonObject.getBoolean("nsfw"));
        }

        album.setSection(jsonObject.getString("section"));
        album.setImagesCount(jsonObject.getInt("images_count"));

        List<Image> images = new ArrayList<>(album.getImagesCount());

        JSONArray imageArray = jsonObject.getJSONArray("images");
        for (int index = 0; index < imageArray.length(); index++) {
            images.add(Image.fromJson(imageArray.getJSONObject(index)));
        }

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
}
